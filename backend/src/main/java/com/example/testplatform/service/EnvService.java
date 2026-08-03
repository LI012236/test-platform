package com.example.testplatform.service;

import com.example.testplatform.common.BizException;
import com.example.testplatform.dao.EnvDao;
import com.example.testplatform.dto.EnvRequest;
import com.example.testplatform.model.Env;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EnvService {

    private final EnvDao envDao;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;

    public EnvService(EnvDao envDao, ProjectService projectService, ObjectMapper objectMapper) {
        this.envDao = envDao;
        this.projectService = projectService;
        this.objectMapper = objectMapper;
    }

    public List<Env> list(long userId, String role, long projectId) {
        projectService.requireAccessible(userId, role, projectId);
        return envDao.listByProject(projectId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Env create(long userId, String role, long projectId, EnvRequest req) {
        projectService.requireAccessible(userId, role, projectId);
        Env valid = validate(projectId, req, null);

        Long id = envDao.insert(valid);
        return envDao.findById(id == null ? 0 : id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Env update(long userId, String role, long envId, EnvRequest req) {
        Env exist = requireEnv(envId);
        projectService.requireAccessible(userId, role, exist.projectId());
        Env valid = validate(exist.projectId(), req, envId);
        envDao.update(envId, valid);
        return envDao.findById(envId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(long userId, String role, long envId) {
        Env exist = requireEnv(envId);
        projectService.requireAccessible(userId, role, exist.projectId());
        envDao.delete(envId);
    }

    public Env requireEnv(long envId) {
        Env env = envDao.findById(envId);
        if (env == null) {
            throw new BizException(404, "环境不存在");
        }
        return env;
    }

    private Env validate(long projectId, EnvRequest req, Long excludeId) {
        if (req == null || req.name() == null || req.name().trim().isEmpty()) {
            throw new BizException(400, "环境名称不能为空");
        }
        String name = req.name().trim();
        if (name.length() > 32) {
            throw new BizException(400, "环境名称不能超过 32 字");
        }
        if (envDao.existsName(projectId, name, excludeId)) {
            throw new BizException(409, "同项目下环境名称已存在");
        }

        String baseUrl = req.baseUrl() == null ? "" : req.baseUrl().trim();
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            throw new BizException(400, "base_url 必须以 http:// 或 https:// 开头");
        }
        if (baseUrl.length() > 255) {
            throw new BizException(400, "base_url 过长");
        }

        String headers = req.headersJson() == null ? "{}" : req.headersJson().trim();
        if (!headers.isEmpty()) {
            try {
                objectMapper.readTree(headers);
            } catch (Exception e) {
                throw new BizException(400, "headers_json 不是合法 JSON");
            }
        }
        return new Env(null, projectId, name, baseUrl, headers, null);
    }
}
