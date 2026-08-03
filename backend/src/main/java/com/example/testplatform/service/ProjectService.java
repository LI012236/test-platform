package com.example.testplatform.service;

import com.example.testplatform.common.BizException;
import com.example.testplatform.dao.ProjectDao;
import com.example.testplatform.dto.ProjectRequest;
import com.example.testplatform.model.Project;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectDao projectDao;

    public ProjectService(ProjectDao projectDao) {
        this.projectDao = projectDao;
    }

    public List<Project> list(long userId, String role) {
        // ADMIN 看全部，普通用户只看自己的
        return projectDao.list("ADMIN".equals(role) ? null : userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Project create(long userId, ProjectRequest req) {
        String name = validateName(req);
        String description = req.description() == null ? "" : req.description().trim();
        if (description.length() > 255) {
            throw new BizException(400, "项目描述不能超过 255 字");
        }
        Long id = projectDao.insert(new Project(null, name, description, userId, null));
        return projectDao.findById(id == null ? 0 : id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Project update(long userId, String role, long id, ProjectRequest req) {
        Project exist = requireAccessible(userId, role, id);
        String name = validateName(req);
        String description = req.description() == null ? "" : req.description().trim();
        projectDao.update(id, new Project(exist.id(), name, description, exist.ownerId(), exist.createdAt()));
        return projectDao.findById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(long userId, String role, long id) {
        requireAccessible(userId, role, id);
        projectDao.deleteCascade(id);
    }

    public Project requireAccessible(long userId, String role, long projectId) {
        Project project = projectDao.findById(projectId);
        if (project == null) {
            throw new BizException(404, "项目不存在");
        }
        if (!project.ownerId().equals(userId) && !"ADMIN".equals(role)) {
            throw new BizException(403, "无权访问他人项目");
        }
        return project;
    }

    private String validateName(ProjectRequest req) {
        if (req == null || req.name() == null || req.name().trim().isEmpty()) {
            throw new BizException(400, "项目名称不能为空");
        }
        String name = req.name().trim();
        if (name.length() > 64) {
            throw new BizException(400, "项目名称不能超过 64 字");
        }
        return name;
    }
}
