package com.example.testplatform.config;

import com.example.testplatform.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 统一鉴权：
 * 1. 解析 Authorization: Bearer <token>
 * 2. 把 userId / role 放进 request attribute，业务层用 @RequestAttribute 直接取
 * 3. /api/admin/** 额外要求 ADMIN 角色
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;

    public AuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return reject(response, 401, "未登录或 token 失效");
        }

        TokenService.Session session = tokenService.resolve(auth.substring(7));
        if (session == null) {
            return reject(response, 401, "未登录或 token 失效");
        }

        if (request.getRequestURI().startsWith("/api/admin") && !"ADMIN".equals(session.role())) {
            return reject(response, 403, "无权限访问");
        }

        request.setAttribute("userId", session.userId());
        request.setAttribute("role", session.role());
        return true;
    }

    private boolean reject(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}");
        return false;
    }
}
