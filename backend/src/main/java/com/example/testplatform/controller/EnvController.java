package com.example.testplatform.controller;

import com.example.testplatform.common.ApiResponse;
import com.example.testplatform.dto.EnvRequest;
import com.example.testplatform.model.Env;
import com.example.testplatform.service.EnvService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class EnvController {

    private final EnvService envService;

    public EnvController(EnvService envService) {
        this.envService = envService;
    }

    @GetMapping("/projects/{projectId}/envs")
    public ApiResponse<Map<String, Object>> list(@RequestAttribute("userId") Long userId,
                                                 @RequestAttribute("role") String role,
                                                 @PathVariable("projectId") long projectId) {
        List<Env> list = envService.list(userId, role, projectId);
        return ApiResponse.ok(Map.of("list", list, "total", list.size()));
    }

    @PostMapping("/projects/{projectId}/envs")
    public ApiResponse<Env> create(@RequestAttribute("userId") Long userId,
                                   @RequestAttribute("role") String role,
                                   @PathVariable("projectId") long projectId,
                                   @RequestBody EnvRequest req) {
        return ApiResponse.ok(envService.create(userId, role, projectId, req));
    }

    @PutMapping("/envs/{id}")
    public ApiResponse<Env> update(@RequestAttribute("userId") Long userId,
                                   @RequestAttribute("role") String role,
                                   @PathVariable("id") long id,
                                   @RequestBody EnvRequest req) {
        return ApiResponse.ok(envService.update(userId, role, id, req));
    }

    @DeleteMapping("/envs/{id}")
    public ApiResponse<Map<String, Object>> delete(@RequestAttribute("userId") Long userId,
                                                   @RequestAttribute("role") String role,
                                                   @PathVariable("id") long id) {
        envService.delete(userId, role, id);
        return ApiResponse.ok(Map.of("deleted", true, "id", id));
    }
}
