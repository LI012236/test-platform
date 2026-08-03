package com.example.testplatform.dto;

/**
 * 环境入参。headers_json 是 JSON 字符串，例如 {"Content-Type":"application/json"}
 */
public record EnvRequest(String name, String baseUrl, String headersJson) {
}
