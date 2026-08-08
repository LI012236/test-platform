package com.example.testplatform.model;

public record Api(
        Long id,
        Long projectId,
        String name,
        String method,
        String path,
        String headersJson,
        String body,
        String createdAt
) {
}
