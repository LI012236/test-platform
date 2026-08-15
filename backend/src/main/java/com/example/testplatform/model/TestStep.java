package com.example.testplatform.model;

/**
 * 用例步骤：引用一个接口，并可选地覆盖 Header / Body，配置变量提取与断言。
 */
public record TestStep(
        Long id,
        Long caseId,
        Integer sort,
        Long apiId,
        String apiName,
        String method,
        String path,
        String overrideHeaders,
        String overrideBody,
        String extractorsJson,
        String assertionsJson,
        String createdAt
) {
}
