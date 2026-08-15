package com.example.testplatform.service;

import com.example.testplatform.common.BizException;
import com.example.testplatform.dao.CaseDao;
import com.example.testplatform.dao.StepDao;
import com.example.testplatform.dto.CaseRequest;
import com.example.testplatform.model.TestCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CaseService {

    private final CaseDao caseDao;
    private final StepDao stepDao;
    private final ProjectService projectService;

    public CaseService(CaseDao caseDao, StepDao stepDao, ProjectService projectService) {
        this.caseDao = caseDao;
        this.stepDao = stepDao;
        this.projectService = projectService;
    }

    public List<TestCase> list(long userId, String role, long projectId) {
        projectService.requireAccessible(userId, role, projectId);
        return caseDao.listByProject(projectId);
    }

    @Transactional(rollbackFor = Exception.class)
    public TestCase create(long userId, String role, long projectId, CaseRequest req) {
        projectService.requireAccessible(userId, role, projectId);
        String name = validateName(req);
        String description = req.description() == null ? "" : req.description().trim();
        Long id = caseDao.insert(new TestCase(null, projectId, name, description, null));
        return caseDao.findById(id == null ? 0 : id);
    }

    @Transactional(rollbackFor = Exception.class)
    public TestCase update(long userId, String role, long caseId, CaseRequest req) {
        TestCase exist = requireCase(caseId);
        projectService.requireAccessible(userId, role, exist.projectId());
        String name = validateName(req);
        String description = req.description() == null ? "" : req.description().trim();
        caseDao.update(caseId, new TestCase(exist.id(), exist.projectId(), name, description, exist.createdAt()));
        return caseDao.findById(caseId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(long userId, String role, long caseId) {
        TestCase exist = requireCase(caseId);
        projectService.requireAccessible(userId, role, exist.projectId());
        stepDao.deleteByCase(caseId);
        caseDao.delete(caseId);
    }

    public TestCase requireCase(long caseId) {
        TestCase c = caseDao.findById(caseId);
        if (c == null) {
            throw new BizException(404, "用例不存在");
        }
        return c;
    }

    private String validateName(CaseRequest req) {
        if (req == null || req.name() == null || req.name().trim().isEmpty()) {
            throw new BizException(400, "用例名称不能为空");
        }
        String name = req.name().trim();
        if (name.length() > 64) {
            throw new BizException(400, "用例名称不能超过 64 字");
        }
        return name;
    }
}
