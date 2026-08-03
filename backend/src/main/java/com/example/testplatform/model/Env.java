package com.example.testplatform.model;

/**
 * 环境：一个项目下可以有 dev / test / prod 多套，各自不同的 base_url 与公共 Header
 */
public record Env(
        Long id,
        Long projectId,
        String name,
        String baseUrl,
        String headersJson,
        String createdAt
) {
}
