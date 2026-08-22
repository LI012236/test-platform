package com.example.testplatform.model;

/**
 * 执行明细：保存这一步实际发出的请求与收到的响应，失败时能直接复现。
 * apiName / method 由查询时联表带出，便于报告里显示「第几步、调的哪个接口」。
 */
public record RunDetail(
        Long id,
        Long runId,
        Long stepId,
        Integer sort,
        String status,
        String requestSnapshot,
        String responseSnapshot,
        String assertionsResult,
        Integer durationMs,
        String errorMsg,
        String apiName,
        String method
) {
    public RunDetail withRunId(Long newRunId) {
        return new RunDetail(id, newRunId, stepId, sort, status, requestSnapshot,
                responseSnapshot, assertionsResult, durationMs, errorMsg, apiName, method);
    }
}
