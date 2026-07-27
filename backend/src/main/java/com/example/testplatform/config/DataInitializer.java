package com.example.testplatform.config;

import com.example.testplatform.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时保证管理员账号存在（幂等）
 * 账号：admin / admin123
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserService userService;

    public DataInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        userService.ensureAdmin("admin", "admin123");
        log.info("管理员账号已就绪: admin / admin123");
    }
}
