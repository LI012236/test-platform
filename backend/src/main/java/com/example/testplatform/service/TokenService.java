package com.example.testplatform.service;

import com.example.testplatform.model.User;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token 管理（内存版）。
 *
 * 为什么不直接上 JWT：
 * - 少一个依赖、少一套签名配置，出错点更少；
 * - 能讲清"token 怎么发、怎么校验、怎么过期、怎么登出"这四件事就够了。
 * 局限：进程重启 token 全失效，多实例部署要换成 Redis。
 */
@Component
public class TokenService {

    public record Session(Long userId, String username, String role) {
    }

    private final Map<String, Session> tokens = new ConcurrentHashMap<>();

    public String issue(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new Session(user.id(), user.username(), user.role()));
        return token;
    }

    public Session resolve(String token) {
        return token == null ? null : tokens.get(token);
    }

    public void revoke(String token) {
        if (token != null) {
            tokens.remove(token);
        }
    }

    public int onlineCount() {
        return tokens.size();
    }
}
