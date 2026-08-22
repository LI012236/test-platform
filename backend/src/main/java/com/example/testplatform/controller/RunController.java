package com.example.testplatform.controller;

import com.example.testplatform.common.ApiResponse;
import com.example.testplatform.common.BizException;
import com.example.testplatform.dao.RunDao;
import com.example.testplatform.dto.RunRequest;
import com.example.testplatform.model.Run;
import com.example.testplatform.service.ProjectService;
import com.example.testplatform.service.RunnerService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RunController {

    private final RunnerService runnerService;
    private final RunDao runDao;
    private final ProjectService projectService;

    public RunController(RunnerService runnerService, RunDao runDao, ProjectService projectService) {
        this.runnerService = runnerService;
        this.runDao = runDao;
        this.projectService = projectService;
    }

    /** 异步执行：立即返回 RUNNING 的 run，前端轮询 GET /api/runs/{id} */
    @PostMapping("/cases/{caseId}/run")
    public ApiResponse<Run> run(@RequestAttribute("userId") Long userId,
                                @RequestAttribute("role") String role,
                                @PathVariable("caseId") long caseId,
                                @RequestBody(required = false) RunRequest req) {
        return ApiResponse.ok(runnerService.run(userId, role, caseId, req));
    }

    @GetMapping("/runs/{id}")
    public ApiResponse<Map<String, Object>> detail(@RequestAttribute("userId") Long userId,
                                                   @RequestAttribute("role") String role,
                                                   @PathVariable("id") long id) {
        Run run = runDao.findById(id);
        if (run == null) {
            throw new BizException(404, "执行记录不存在");
        }
        projectService.requireAccessible(userId, role, run.projectId());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("run", run);
        data.put("details", runDao.listDetails(id));
        return ApiResponse.ok(data);
    }

    @GetMapping("/projects/{projectId}/runs")
    public ApiResponse<Map<String, Object>> list(@RequestAttribute("userId") Long userId,
                                                 @RequestAttribute("role") String role,
                                                 @PathVariable("projectId") long projectId,
                                                 @RequestParam(name = "limit", defaultValue = "20") int limit) {
        projectService.requireAccessible(userId, role, projectId);
        List<Run> list = runDao.listByProject(projectId, Math.min(Math.max(limit, 1), 100));
        return ApiResponse.ok(Map.of("list", list, "total", list.size()));
    }
}
