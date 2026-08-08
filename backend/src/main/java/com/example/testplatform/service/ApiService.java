package com.example.testplatform.service;

import com.example.testplatform.common.BizException;
import com.example.testplatform.dao.ApiDao;
import com.example.testplatform.dto.ApiRequest;
import com.example.testplatform.model.Api;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class ApiService {

    private static final Set<String> METHODS = Set.of("GET", "POST", "PUT", "DELETE", "PATCH");

    private final ApiDao apiDao;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;

    public ApiService(ApiDao apiDao, ProjectService projectService, ObjectMapper objectMapper) {
        this.apiDao = apiDao;
        this.projectService = projectService;
        this.objectMapper = objectMapper;
    }

    public List<Api> list(long userId, String role, long projectId) {
        projectService.requireAccessible(userId, role, projectId);
        return apiDao.listByProject(projectId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Api create(long userId, String role, long projectId, ApiRequest req) {
        projectService.requireAccessible(userId, role, projectId);
        Api valid = validate(projectId, req);
        Long id = apiDao.insert(valid);
        return apiDao.findById(id == null ? 0 : id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Api update(long userId, String role, long apiId, ApiRequest req) {
        Api exist = requireApi(apiId);
        projectService.requireAccessible(userId, role, exist.projectId());
        Api valid = validate(exist.projectId(), req);
        apiDao.update(apiId, valid);
        return apiDao.findById(apiId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(long userId, String role, long apiId) {
        Api exist = requireApi(apiId);
        projectService.requireAccessible(userId, role, exist.projectId());
        if (apiDao.countUsedByStep(apiId) > 0) {
            throw new BizException(409, "该接口已被用例步骤引用，无法删除");
        }
        apiDao.delete(apiId);
    }

    public Api requireApi(long apiId) {
        Api api = apiDao.findById(apiId);
        if (api == null) {
            throw new BizException(404, "接口不存在");
        }
        return api;
    }

    private Api validate(long projectId, ApiRequest req) {
        if (req == null || req.name() == null || req.name().trim().isEmpty()) {
            throw new BizException(400, "接口名称不能为空");
        }
        String name = req.name().trim();
        if (name.length() > 64) {
            throw new BizException(400, "接口名称不能超过 64 字");
        }

        String method = req.method() == null ? "" : req.method().trim().toUpperCase();
        if (!METHODS.contains(method)) {
            throw new BizException(400, "method 只能是 GET/POST/PUT/DELETE/PATCH");
        }

        String path = req.path() == null ? "" : req.path().trim();
        if (!path.startsWith("/")) {
            throw new BizException(400, "path 必须以 / 开头");
        }
        if (path.length() > 255) {
            throw new BizException(400, "path 过长");
        }

        String headers = req.headersJson() == null || req.headersJson().isBlank() ? "{}" : req.headersJson().trim();
        try {
            objectMapper.readTree(headers);
        } catch (Exception e) {
            throw new BizException(400, "headers_json 不是合法 JSON");
        }

        return new Api(null, projectId, name, method, path, headers, req.body(), null);
    }
}
