package com.example.testplatform.model;

public record Project(
        Long id,
        String name,
        String description,
        Long ownerId,
        String createdAt
) {
}
