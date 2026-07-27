package com.example.testplatform.model;

/**
 * 平台用户。password / salt 只用于服务端校验，绝不返回给前端（对外用 UserView）。
 */
public record User(
        Long id,
        String username,
        String password,
        String salt,
        String nickname,
        String role,
        String createdAt
) {
    public User withId(Long newId) {
        return new User(newId, username, password, salt, nickname, role, createdAt);
    }
}
