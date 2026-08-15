package com.example.testplatform.controller;

import com.example.testplatform.common.ApiResponse;
import com.example.testplatform.dto.StepRequest;
import com.example.testplatform.model.TestStep;
import com.example.testplatform.service.StepService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StepController {

    private final StepService stepService;

    public StepController(StepService stepService) {
        this.stepService = stepService;
    }

    @GetMapping("/cases/{caseId}/steps")
    public ApiResponse<Map<String, Object>> list(@RequestAttribute("userId") Long userId,
                                                 @RequestAttribute("role") String role,
                                                 @PathVariable("caseId") long caseId) {
        List<TestStep> list = stepService.list(userId, role, caseId);
        return ApiResponse.ok(Map.of("list", list, "total", list.size()));
    }

    @PostMapping("/cases/{caseId}/steps")
    public ApiResponse<TestStep> add(@RequestAttribute("userId") Long userId,
                                     @RequestAttribute("role") String role,
                                     @PathVariable("caseId") long caseId,
                                     @RequestBody StepRequest req) {
        return ApiResponse.ok(stepService.add(userId, role, caseId, req));
    }

    @PutMapping("/steps/{id}")
    public ApiResponse<TestStep> update(@RequestAttribute("userId") Long userId,
                                        @RequestAttribute("role") String role,
                                        @PathVariable("id") long id,
                                        @RequestBody StepRequest req) {
        return ApiResponse.ok(stepService.update(userId, role, id, req));
    }

    @DeleteMapping("/steps/{id}")
    public ApiResponse<Map<String, Object>> delete(@RequestAttribute("userId") Long userId,
                                                   @RequestAttribute("role") String role,
                                                   @PathVariable("id") long id) {
        stepService.delete(userId, role, id);
        return ApiResponse.ok(Map.of("deleted", true, "id", id));
    }
}
