package com.example.testplatform.dto;

/**
 * 对外返回的用户信息（不含 password / salt）
 */
public record UserView(Long id, String username, String nickname, String role) {
}
