package com.example.testplatform.controller;

import com.example.testplatform.common.ApiResponse;
import com.example.testplatform.dto.ProjectRequest;
import com.example.testplatform.model.Project;
import com.example.testplatform.service.ProjectService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestAttribute("userId") Long userId,
                                                 @RequestAttribute("role") String role) {
        List<Project> list = projectService.list(userId, role);
        return ApiResponse.ok(Map.of("list", list, "total", list.size()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Project> detail(@RequestAttribute("userId") Long userId,
                                       @RequestAttribute("role") String role,
                                       @PathVariable("id") long id) {
        return ApiResponse.ok(projectService.requireAccessible(userId, role, id));
    }

    @PostMapping
    public ApiResponse<Project> create(@RequestAttribute("userId") Long userId,
                                       @RequestBody ProjectRequest req) {
        return ApiResponse.ok(projectService.create(userId, req));
    }

    @PutMapping("/{id}")
    public ApiResponse<Project> update(@RequestAttribute("userId") Long userId,
                                       @RequestAttribute("role") String role,
                                       @PathVariable("id") long id,
                                       @RequestBody ProjectRequest req) {
        return ApiResponse.ok(projectService.update(userId, role, id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> delete(@RequestAttribute("userId") Long userId,
                                                   @RequestAttribute("role") String role,
                                                   @PathVariable("id") long id) {
        projectService.delete(userId, role, id);
        return ApiResponse.ok(Map.of("deleted", true, "id", id));
    }
}
