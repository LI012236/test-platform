package com.example.testplatform.controller;

import com.example.testplatform.common.ApiResponse;
import com.example.testplatform.dto.LoginRequest;
import com.example.testplatform.dto.RegisterRequest;
import com.example.testplatform.dto.UserView;
import com.example.testplatform.model.User;
import com.example.testplatform.service.TokenService;
import com.example.testplatform.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final TokenService tokenService;

    public AuthController(UserService userService, TokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody RegisterRequest req) {
        User user = userService.register(req.username(), req.password(), req.nickname());
        String token = tokenService.issue(user);
        return ApiResponse.ok(payload(token, user));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest req) {
        User user = userService.login(req.username(), req.password());
        String token = tokenService.issue(user);
        return ApiResponse.ok(payload(token, user));
    }

    @GetMapping("/me")
    public ApiResponse<UserView> me(@RequestAttribute("userId") Long userId) {
        return ApiResponse.ok(toView(userService.requireById(userId)));
    }

    @PostMapping("/logout")
    public ApiResponse<Map<String, Object>> logout(@RequestHeader("Authorization") String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            tokenService.revoke(auth.substring(7));
        }
        return ApiResponse.ok(Map.of("logout", true));
    }

    private Map<String, Object> payload(String token, User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("user", toView(user));
        return data;
    }

    private UserView toView(User user) {
        return new UserView(user.id(), user.username(), user.nickname(), user.role());
    }
}
