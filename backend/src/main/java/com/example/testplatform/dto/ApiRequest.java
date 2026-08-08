package com.example.testplatform.dto;

public record ApiRequest(String name, String method, String path, String headersJson, String body) {
}
