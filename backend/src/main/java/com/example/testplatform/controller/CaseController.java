package com.example.testplatform.controller;

import com.example.testplatform.common.ApiResponse;
import com.example.testplatform.dto.CaseRequest;
import com.example.testplatform.model.TestCase;
import com.example.testplatform.service.CaseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @GetMapping("/projects/{projectId}/cases")
    public ApiResponse<Map<String, Object>> list(@RequestAttribute("userId") Long userId,
                                                 @RequestAttribute("role") String role,
                                                 @PathVariable("projectId") long projectId) {
        List<TestCase> list = caseService.list(userId, role, projectId);
        return ApiResponse.ok(Map.of("list", list, "total", list.size()));
    }

    @GetMapping("/cases/{id}")
    public ApiResponse<TestCase> detail(@RequestAttribute("userId") Long userId,
                                        @RequestAttribute("role") String role,
                                        @PathVariable("id") long id) {
        TestCase c = caseService.requireCase(id);
        return ApiResponse.ok(c);
    }

    @PostMapping("/projects/{projectId}/cases")
    public ApiResponse<TestCase> create(@RequestAttribute("userId") Long userId,
                                        @RequestAttribute("role") String role,
                                        @PathVariable("projectId") long projectId,
                                        @RequestBody CaseRequest req) {
        return ApiResponse.ok(caseService.create(userId, role, projectId, req));
    }

    @PutMapping("/cases/{id}")
    public ApiResponse<TestCase> update(@RequestAttribute("userId") Long userId,
                                        @RequestAttribute("role") String role,
                                        @PathVariable("id") long id,
                                        @RequestBody CaseRequest req) {
        return ApiResponse.ok(caseService.update(userId, role, id, req));
    }

    @DeleteMapping("/cases/{id}")
    public ApiResponse<Map<String, Object>> delete(@RequestAttribute("userId") Long userId,
                                                   @RequestAttribute("role") String role,
                                                   @PathVariable("id") long id) {
        caseService.delete(userId, role, id);
        return ApiResponse.ok(Map.of("deleted", true, "id", id));
    }
}
