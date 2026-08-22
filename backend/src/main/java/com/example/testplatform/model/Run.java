package com.example.testplatform.model;

public record Run(
        Long id,
        Long projectId,
        Long caseId,
        Long envId,
        String status,
        Integer total,
        Integer passed,
        Integer failed,
        Integer durationMs,
        String startedAt,
        String finishedAt,
        String errorMsg
) {
    public Run withId(Long newId) {
        return new Run(newId, projectId, caseId, envId, status, total, passed, failed,
                durationMs, startedAt, finishedAt, errorMsg);
    }
}
