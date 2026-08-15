package com.example.testplatform.model;

public record TestCase(
        Long id,
        Long projectId,
        String name,
        String description,
        String createdAt
) {
}
