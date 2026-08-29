package com.example.testplatform.controller;

import com.example.testplatform.common.ApiResponse;
import com.example.testplatform.service.SeedService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SeedController {

    private final SeedService seedService;

    public SeedController(SeedService seedService) {
        this.seedService = seedService;
    }

    /** 一键导入示例数据（接口 + 用例 + 步骤 + 环境） */
    @PostMapping("/projects/{projectId}/seed-demo")
    public ApiResponse<Map<String, Object>> seed(@RequestAttribute("userId") Long userId,
                                                 @RequestAttribute("role") String role,
                                                 @PathVariable("projectId") long projectId,
                                                 @RequestBody(required = false) Map<String, String> body) {
        String baseUrl = body == null ? null : body.get("base_url");
        return ApiResponse.ok(seedService.seed(userId, role, projectId, baseUrl));
    }
}
