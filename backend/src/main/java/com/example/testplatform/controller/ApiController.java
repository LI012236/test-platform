package com.example.testplatform.controller;

import com.example.testplatform.common.ApiResponse;
import com.example.testplatform.dto.ApiRequest;
import com.example.testplatform.model.Api;
import com.example.testplatform.service.ApiService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final ApiService apiService;

    public ApiController(ApiService apiService) {
        this.apiService = apiService;
    }

    @GetMapping("/projects/{projectId}/apis")
    public ApiResponse<Map<String, Object>> list(@RequestAttribute("userId") Long userId,
                                                 @RequestAttribute("role") String role,
                                                 @PathVariable("projectId") long projectId) {
        List<Api> list = apiService.list(userId, role, projectId);
        return ApiResponse.ok(Map.of("list", list, "total", list.size()));
    }

    @PostMapping("/projects/{projectId}/apis")
    public ApiResponse<Api> create(@RequestAttribute("userId") Long userId,
                                   @RequestAttribute("role") String role,
                                   @PathVariable("projectId") long projectId,
                                   @RequestBody ApiRequest req) {
        return ApiResponse.ok(apiService.create(userId, role, projectId, req));
    }

    @PutMapping("/apis/{id}")
    public ApiResponse<Api> update(@RequestAttribute("userId") Long userId,
                                   @RequestAttribute("role") String role,
                                   @PathVariable("id") long id,
                                   @RequestBody ApiRequest req) {
        return ApiResponse.ok(apiService.update(userId, role, id, req));
    }

    @DeleteMapping("/apis/{id}")
    public ApiResponse<Map<String, Object>> delete(@RequestAttribute("userId") Long userId,
                                                   @RequestAttribute("role") String role,
                                                   @PathVariable("id") long id) {
        apiService.delete(userId, role, id);
        return ApiResponse.ok(Map.of("deleted", true, "id", id));
    }
}
