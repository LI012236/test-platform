package com.example.testplatform.service;

import com.example.testplatform.common.BizException;
import com.example.testplatform.dao.ApiDao;
import com.example.testplatform.dao.CaseDao;
import com.example.testplatform.dao.EnvDao;
import com.example.testplatform.dao.RunDao;
import com.example.testplatform.dao.StepDao;
import com.example.testplatform.dto.RunRequest;
import com.example.testplatform.dto.RunResult;
import com.example.testplatform.model.Api;
import com.example.testplatform.model.Env;
import com.example.testplatform.model.Run;
import com.example.testplatform.model.RunDetail;
import com.example.testplatform.model.TestCase;
import com.example.testplatform.model.TestStep;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 执行引擎：把「用例步骤 + 环境」变成真实请求，并按断言给出结论。
 *
 * 关键设计：
 * 1. 变量池在步骤间传递（前一步提取的 token 给后一步用）
 * 2. 每步都保存请求/响应快照，失败可复现
 * 3. 断言类型：status / json / contain / time
 * 4. 默认失败即中断，可配置 continue_on_fail 跑完全部步骤
 */
@Service
public class RunnerService {

    private static final Logger log = LoggerFactory.getLogger(RunnerService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern VAR = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}");
    private static final int MAX_SNAPSHOT = 4096;

    private final CaseDao caseDao;
    private final StepDao stepDao;
    private final ApiDao apiDao;
    private final EnvDao envDao;
    private final RunDao runDao;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;
    private final HttpClient http;
    private final ExecutorService executor;

    public RunnerService(CaseDao caseDao, StepDao stepDao, ApiDao apiDao, EnvDao envDao,
                         RunDao runDao, ProjectService projectService, ObjectMapper objectMapper) {
        this.caseDao = caseDao;
        this.stepDao = stepDao;
        this.apiDao = apiDao;
        this.envDao = envDao;
        this.runDao = runDao;
        this.projectService = projectService;
        this.objectMapper = objectMapper;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "runner-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
    }

    public Run run(long userId, String role, long caseId, RunRequest req) {
        TestCase testCase = caseDao.findById(caseId);
        if (testCase == null) {
            throw new BizException(404, "用例不存在");
        }
        projectService.requireAccessible(userId, role, testCase.projectId());

        if (req == null || req.envId() == null) {
            throw new BizException(400, "env_id 不能为空");
        }
        Env env = envDao.findById(req.envId());
        if (env == null) {
            throw new BizException(404, "环境不存在");
        }
        if (!env.projectId().equals(testCase.projectId())) {
            throw new BizException(400, "环境不属于该用例所在项目");
        }

        List<TestStep> steps = stepDao.listByCase(caseId);
        if (steps.isEmpty()) {
            throw new BizException(400, "用例还没有步骤");
        }
        boolean continueOnFail = Boolean.TRUE.equals(req.continueOnFail());

        Run created = new Run(null, testCase.projectId(), caseId, env.id(), "RUNNING",
                0, 0, 0, 0, LocalDateTime.now().format(FMT), null, null);
        Long runId = runDao.insert(created);
        Run saved = created.withId(runId);

        // 异步执行：立即返回 RUNNING，前端轮询 GET /api/runs/{id}
        executor.submit(() -> execute(runId, steps, env, continueOnFail));
        return saved;
    }

    private void execute(long runId, List<TestStep> steps, Env env, boolean continueOnFail) {
        long start = System.currentTimeMillis();
        Map<String, String> vars = new HashMap<>();
        int passed = 0;
        int failed = 0;
        int executed = 0;
        String error = null;

        try {
            for (TestStep step : steps) {
                RunDetail detail = executeStep(step, env, vars).withRunId(runId);
                runDao.insertDetail(detail);
                executed++;
                if ("PASSED".equals(detail.status())) {
                    passed++;
                } else {
                    failed++;
                }
                // 每步都更新进度，前端可以看到「跑到第几步」
                runDao.updateProgress(runId, "RUNNING", executed, passed, failed,
                        (int) (System.currentTimeMillis() - start), null, null);

                if (!"PASSED".equals(detail.status()) && !continueOnFail) {
                    break;
                }
            }
        } catch (Exception e) {
            error = truncate(e.toString());
            log.error("执行异常 runId={}", runId, e);
        }

        String status = error != null ? "ERROR" : (failed > 0 ? "FAILED" : "PASSED");
        runDao.updateProgress(runId, status, executed, passed, failed,
                (int) (System.currentTimeMillis() - start),
                LocalDateTime.now().format(FMT), error);
    }

    private RunDetail executeStep(TestStep step, Env env, Map<String, String> vars) {
        Api api = apiDao.findById(step.apiId());
        if (api == null) {
            return new RunDetail(null, null, step.id(), step.sort(), "ERROR", null, null,
                    "[]", 0, "步骤引用的接口已被删除", null, null);
        }

        Map<String, String> headers = new LinkedHashMap<>();
        mergeHeaders(headers, env.headersJson(), vars);
        mergeHeaders(headers, api.headersJson(), vars);
        mergeHeaders(headers, step.overrideHeaders(), vars);

        String body = step.overrideBody() != null && !step.overrideBody().isBlank()
                ? step.overrideBody()
                : api.body();
        body = substitute(body, vars);

        // 有 body 但没显式声明 Content-Type 时补一个默认值，避免服务端返回 415
        if (body != null && !body.isBlank()) {
            boolean hasContentType = headers.keySet().stream()
                    .anyMatch(k -> "Content-Type".equalsIgnoreCase(k));
            if (!hasContentType) {
                headers.put("Content-Type", "application/json");
            }
        }

        String url = joinUrl(env.baseUrl(), substitute(api.path(), vars));

        String requestSnapshot = snapshotRequest(api.method(), url, headers, body);
        long start = System.currentTimeMillis();

        HttpResponse<String> response;
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10));
            headers.forEach(builder::header);

            if ("GET".equals(api.method()) || "DELETE".equals(api.method())) {
                builder.method(api.method(), HttpRequest.BodyPublishers.noBody());
            } else if (body != null && !body.isBlank()) {
                builder.method(api.method(),
                        HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            } else {
                builder.method(api.method(), HttpRequest.BodyPublishers.noBody());
            }

            response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            int cost = (int) (System.currentTimeMillis() - start);
            log.warn("步骤执行异常: {} {}", api.method(), url, e);
            return new RunDetail(null, null, step.id(), step.sort(), "ERROR",
                    truncate(requestSnapshot), null, "[]", cost, truncate(e.toString()), null, null);
        }

        int cost = (int) (System.currentTimeMillis() - start);
        String responseBody = response.body() == null ? "" : response.body();

        List<Map<String, Object>> assertionResults = new ArrayList<>();
        boolean allPassed = evaluateAssertions(step.assertionsJson(), response.statusCode(), responseBody, cost,
                assertionResults);

        // 变量提取：即使断言失败也执行，方便排查后续步骤
        extractVariables(step.extractorsJson(), responseBody, vars);

        String assertionsJson;
        try {
            assertionsJson = objectMapper.writeValueAsString(assertionResults);
        } catch (Exception e) {
            assertionsJson = "[]";
        }

        return new RunDetail(null, null, step.id(), step.sort(),
                allPassed ? "PASSED" : "FAILED",
                truncate(requestSnapshot),
                truncate(responseBody),
                assertionsJson, cost, null, null, null);
    }

    private boolean evaluateAssertions(String assertionsJson, int statusCode, String body,
                                       int costMs, List<Map<String, Object>> out) {
        JsonNode root;
        try {
            JsonNode arr = objectMapper.readTree(assertionsJson == null ? "[]" : assertionsJson);
            root = safeParse(body);
            if (arr == null || !arr.isArray() || arr.isEmpty()) {
                // 没有配置断言时，默认认为 HTTP 状态 < 400 即通过
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("type", "status_default");
                r.put("passed", statusCode < 400);
                r.put("actual", statusCode);
                out.add(r);
                return statusCode < 400;
            }
            boolean all = true;
            for (JsonNode node : arr) {
                String type = node.path("type").asText("");
                boolean passed;
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("type", type);

                switch (type) {
                    case "status" -> {
                        int expect = node.path("expect").asInt(-1);
                        passed = statusCode == expect;
                        result.put("expect", expect);
                        result.put("actual", statusCode);
                    }
                    case "json" -> {
                        String path = node.path("path").asText("");
                        JsonNode expectNode = node.path("expect");
                        JsonNode actual = at(root, path);
                        passed = jsonEquals(actual, expectNode);
                        result.put("path", path);
                        result.put("expect", expectNode.isMissingNode() ? null : expectNode.asText());
                        result.put("actual", actual.isMissingNode() ? "<缺失>" : actual.asText());
                    }
                    case "contain" -> {
                        String text = node.path("text").asText("");
                        passed = body != null && body.contains(text);
                        result.put("text", text);
                    }
                    case "time" -> {
                        long max = node.path("max_ms").asLong(-1);
                        passed = max > 0 && costMs <= max;
                        result.put("max_ms", max);
                        result.put("actual", costMs);
                    }
                    default -> {
                        passed = false;
                        result.put("error", "未知断言类型: " + type);
                    }
                }
                result.put("passed", passed);
                out.add(result);
                all = all && passed;
            }
            return all;
        } catch (Exception e) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("type", "assertion_error");
            r.put("passed", false);
            r.put("error", e.getMessage());
            out.add(r);
            return false;
        }
    }

    private void extractVariables(String extractorsJson, String body, Map<String, String> vars) {
        if (extractorsJson == null || extractorsJson.isBlank() || "[]".equals(extractorsJson)) {
            return;
        }
        try {
            JsonNode arr = objectMapper.readTree(extractorsJson);
            JsonNode root = safeParse(body);
            for (JsonNode node : arr) {
                String name = node.path("name").asText("");
                String path = node.path("path").asText("");
                if (name.isEmpty() || path.isEmpty()) {
                    continue;
                }
                JsonNode value = at(root, path);
                if (!value.isMissingNode()) {
                    vars.put(name, value.isValueNode() ? value.asText() : value.toString());
                }
            }
        } catch (Exception e) {
            log.warn("变量提取失败: {}", extractorsJson, e);
        }
    }

    // ---------------- 工具方法 ----------------

    private JsonNode safeParse(String json) {
        try {
            return json == null || json.isBlank()
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(json);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    /** 支持 $.a.b、$.a[0].b 这类简单路径 */
    private JsonNode at(JsonNode root, String path) {
        if (root == null || path == null || path.isEmpty() || "$".equals(path)) {
            return root == null ? MissingNode.getInstance() : root;
        }
        String p = path.startsWith("$") ? path.substring(1) : path;
        JsonNode cur = root;
        for (String rawSeg : p.split("\\.")) {
            String seg = rawSeg.trim();
            if (seg.isEmpty()) {
                continue;
            }
            String name = seg;
            int index = -1;
            int bracket = seg.indexOf('[');
            if (bracket >= 0) {
                name = seg.substring(0, bracket);
                try {
                    index = Integer.parseInt(seg.substring(bracket + 1, seg.length() - 1));
                } catch (Exception e) {
                    return MissingNode.getInstance();
                }
            }
            if (!name.isEmpty()) {
                if (cur == null || !cur.has(name)) {
                    return MissingNode.getInstance();
                }
                cur = cur.get(name);
            }
            if (index >= 0) {
                if (cur == null || !cur.isArray() || index >= cur.size()) {
                    return MissingNode.getInstance();
                }
                cur = cur.get(index);
            }
        }
        return cur == null ? MissingNode.getInstance() : cur;
    }

    private boolean jsonEquals(JsonNode actual, JsonNode expect) {
        if (actual == null || actual.isMissingNode()) {
            return false;
        }
        if (actual.isNumber() && expect.isNumber()) {
            return actual.asDouble() == expect.asDouble();
        }
        return actual.asText().equals(expect.asText());
    }

    private void mergeHeaders(Map<String, String> target, String json, Map<String, String> vars) {
        if (json == null || json.isBlank()) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                target.put(e.getKey(), substitute(e.getValue().asText(), vars));
            }
        } catch (Exception e) {
            log.warn("Header 解析失败: {}", json);
        }
    }

    private String substitute(String text, Map<String, String> vars) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        Matcher m = VAR.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            String value = vars.getOrDefault(name, m.group(0));
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String joinUrl(String baseUrl, String path) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String p = path.startsWith("/") ? path : "/" + path;
        return base + p;
    }

    private String snapshotRequest(String method, String url, Map<String, String> headers, String body) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("method", method);
        map.put("url", url);
        map.put("headers", headers);
        map.put("body", body == null || body.isBlank() ? null : body);
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return url;
        }
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= MAX_SNAPSHOT ? text : text.substring(0, MAX_SNAPSHOT) + "...(truncated)";
    }
}
