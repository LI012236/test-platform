package com.example.testplatform.service;

import com.example.testplatform.common.BizException;
import com.example.testplatform.dao.ApiDao;
import com.example.testplatform.dao.StepDao;
import com.example.testplatform.dto.StepRequest;
import com.example.testplatform.model.Api;
import com.example.testplatform.model.TestCase;
import com.example.testplatform.model.TestStep;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StepService {

    private final StepDao stepDao;
    private final ApiDao apiDao;
    private final CaseService caseService;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;

    public StepService(StepDao stepDao, ApiDao apiDao, CaseService caseService,
                       ProjectService projectService, ObjectMapper objectMapper) {
        this.stepDao = stepDao;
        this.apiDao = apiDao;
        this.caseService = caseService;
        this.projectService = projectService;
        this.objectMapper = objectMapper;
    }

    public List<TestStep> list(long userId, String role, long caseId) {
        TestCase c = caseService.requireCase(caseId);
        projectService.requireAccessible(userId, role, c.projectId());
        return stepDao.listByCase(caseId);
    }

    @Transactional(rollbackFor = Exception.class)
    public TestStep add(long userId, String role, long caseId, StepRequest req) {
        TestCase c = caseService.requireCase(caseId);
        projectService.requireAccessible(userId, role, c.projectId());
        TestStep valid = validate(c, req, null);

        int sort = (req.sort() == null || req.sort() <= 0)
                ? stepDao.maxSort(caseId) + 1
                : req.sort();

        TestStep toInsert = new TestStep(null, caseId, sort, valid.apiId(),
                null, null, null, valid.overrideHeaders(), valid.overrideBody(),
                valid.extractorsJson(), valid.assertionsJson(), null);
        Long id = stepDao.insert(toInsert);
        return stepDao.findById(id == null ? 0 : id);
    }

    @Transactional(rollbackFor = Exception.class)
    public TestStep update(long userId, String role, long stepId, StepRequest req) {
        TestStep exist = requireStep(stepId);
        TestCase c = caseService.requireCase(exist.caseId());
        projectService.requireAccessible(userId, role, c.projectId());
        TestStep valid = validate(c, req, stepId);

        int sort = (req.sort() == null || req.sort() <= 0) ? exist.sort() : req.sort();
        TestStep toUpdate = new TestStep(stepId, exist.caseId(), sort, valid.apiId(),
                null, null, null, valid.overrideHeaders(), valid.overrideBody(),
                valid.extractorsJson(), valid.assertionsJson(), exist.createdAt());
        stepDao.update(stepId, toUpdate);
        return stepDao.findById(stepId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(long userId, String role, long stepId) {
        TestStep exist = requireStep(stepId);
        TestCase c = caseService.requireCase(exist.caseId());
        projectService.requireAccessible(userId, role, c.projectId());
        stepDao.delete(stepId);
    }

    public TestStep requireStep(long stepId) {
        TestStep s = stepDao.findById(stepId);
        if (s == null) {
            throw new BizException(404, "步骤不存在");
        }
        return s;
    }

    private TestStep validate(TestCase c, StepRequest req, Long excludeId) {
        if (req == null || req.apiId() == null) {
            throw new BizException(400, "api_id 不能为空");
        }
        Api api = apiDao.findById(req.apiId());
        if (api == null) {
            throw new BizException(404, "接口不存在");
        }
        if (!api.projectId().equals(c.projectId())) {
            throw new BizException(400, "步骤引用的接口不属于当前项目");
        }

        String extractors = normalizeArray(req.extractorsJson(), "extractors_json");
        String assertions = normalizeArray(req.assertionsJson(), "assertions_json");

        return new TestStep(null, c.id(), null, req.apiId(), null, null, null,
                req.overrideHeaders(), req.overrideBody(), extractors, assertions, null);
    }

    /** 校验必须是 JSON 数组；空值归一为 [] */
    private String normalizeArray(String json, String field) {
        if (json == null || json.isBlank()) {
            return "[]";
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isArray()) {
                throw new BizException(400, field + " 必须是 JSON 数组");
            }
            return json;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(400, field + " 不是合法 JSON");
        }
    }
}
