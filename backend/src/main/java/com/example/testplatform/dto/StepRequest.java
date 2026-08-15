package com.example.testplatform.dto;

/**
 * 步骤入参。extractors_json / assertions_json 是 JSON 数组字符串。
 */
public record StepRequest(
        Integer sort,
        Long apiId,
        String overrideHeaders,
        String overrideBody,
        String extractorsJson,
        String assertionsJson
) {
}
