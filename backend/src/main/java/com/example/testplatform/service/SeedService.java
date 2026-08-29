package com.example.testplatform.service;

import com.example.testplatform.common.BizException;
import com.example.testplatform.dao.ApiDao;
import com.example.testplatform.dao.CaseDao;
import com.example.testplatform.dao.EnvDao;
import com.example.testplatform.dao.StepDao;
import com.example.testplatform.model.Api;
import com.example.testplatform.model.Env;
import com.example.testplatform.model.TestCase;
import com.example.testplatform.model.TestStep;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一键导入示例：为项目生成一套「登录 → 加购 → 下单 → 查订单」的接口、用例与步骤。
 * 幂等：同名接口/用例已存在则跳过。
 */
@Service
public class SeedService {

    private static final String AUTH_HEADER = "{\"Authorization\":\"Bearer ${token}\"}";

    private final ApiDao apiDao;
    private final CaseDao caseDao;
    private final StepDao stepDao;
    private final EnvDao envDao;
    private final ProjectService projectService;

    public SeedService(ApiDao apiDao, CaseDao caseDao, StepDao stepDao,
                       EnvDao envDao, ProjectService projectService) {
        this.apiDao = apiDao;
        this.caseDao = caseDao;
        this.stepDao = stepDao;
        this.envDao = envDao;
        this.projectService = projectService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> seed(long userId, String role, long projectId, String baseUrl) {
        projectService.requireAccessible(userId, role, projectId);

        String url = (baseUrl == null || baseUrl.isBlank()) ? "http://127.0.0.1:18091" : baseUrl.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new BizException(400, "base_url 必须以 http:// 或 https:// 开头");
        }

        int envCreated = 0;
        List<Env> envs = envDao.listByProject(projectId);
        if (envs.isEmpty()) {
            envDao.insert(new Env(null, projectId, "dev", url,
                    "{\"Content-Type\":\"application/json\"}", null));
            envCreated = 1;
        }

        Map<String, Api> apis = new LinkedHashMap<>();
        apis.put("用户登录", ensureApi(projectId, "用户登录", "POST", "/api/auth/login",
                "{}", "{\"username\":\"admin\",\"password\":\"admin123\"}"));
        apis.put("商品列表", ensureApi(projectId, "商品列表", "GET", "/api/products?page=1&size=8",
                "{}", null));
        apis.put("加入购物车", ensureApi(projectId, "加入购物车", "POST", "/api/cart",
                "{}", "{\"product_id\":1,\"quantity\":1}"));
        apis.put("创建订单", ensureApi(projectId, "创建订单", "POST", "/api/orders",
                "{}", "{\"request_id\":\"seed-demo-001\",\"receiver_name\":\"何杰\","
                + "\"receiver_phone\":\"13800000000\",\"receiver_address\":\"成都市武侯区示例路 1 号\"}"));
        apis.put("我的订单", ensureApi(projectId, "我的订单", "GET", "/api/orders", "{}", null));

        int caseCreated = 0;
        TestCase existingCase = caseDao.listByProject(projectId).stream()
                .filter(c -> "下单主流程".equals(c.name()))
                .findFirst().orElse(null);

        if (existingCase == null) {
            Long caseId = caseDao.insert(new TestCase(null, projectId, "下单主流程",
                    "登录 → 加购 → 下单 → 查订单", null));
            stepDao.insert(new TestStep(null, caseId, 1, apis.get("用户登录").id(), null, null, null,
                    null, null,
                    "[{\"name\":\"token\",\"path\":\"$.data.token\"}]",
                    "[{\"type\":\"status\",\"expect\":200},{\"type\":\"json\",\"path\":\"$.code\",\"expect\":0}]",
                    null));
            stepDao.insert(new TestStep(null, caseId, 2, apis.get("加入购物车").id(), null, null, null,
                    AUTH_HEADER, null,
                    "[]", "[{\"type\":\"status\",\"expect\":200}]", null));
            stepDao.insert(new TestStep(null, caseId, 3, apis.get("创建订单").id(), null, null, null,
                    AUTH_HEADER, null,
                    "[]", "[{\"type\":\"status\",\"expect\":200},{\"type\":\"time\",\"max_ms\":3000}]", null));
            stepDao.insert(new TestStep(null, caseId, 4, apis.get("我的订单").id(), null, null, null,
                    AUTH_HEADER, null,
                    "[]", "[{\"type\":\"status\",\"expect\":200}]", null));
            caseCreated = 1;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("apis", apis.size());
        data.put("env_created", envCreated);
        data.put("case_created", caseCreated);
        data.put("message", "示例数据已就绪：5 个接口 + 1 条 4 步用例");
        return data;
    }

    private Api ensureApi(long projectId, String name, String method, String path,
                          String headersJson, String body) {
        Api exist = apiDao.listByProject(projectId).stream()
                .filter(a -> name.equals(a.name()))
                .findFirst().orElse(null);
        if (exist != null) {
            return exist;
        }
        Long id = apiDao.insert(new Api(null, projectId, name, method, path, headersJson, body, null));
        return apiDao.findById(id == null ? 0 : id);
    }
}
