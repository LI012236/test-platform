package com.example.testplatform.service;

import com.example.testplatform.common.BizException;
import com.example.testplatform.common.PasswordUtil;
import com.example.testplatform.dao.UserDao;
import com.example.testplatform.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    @Transactional(rollbackFor = Exception.class)
    public User register(String username, String password, String nickname) {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new BizException(400, "用户名需为 3-20 位字母、数字或下划线");
        }
        if (password == null || password.length() < 6 || password.length() > 32) {
            throw new BizException(400, "密码长度需在 6-32 位之间");
        }
        if (userDao.findByUsername(username) != null) {
            throw new BizException(409, "用户名已存在");
        }

        String salt = PasswordUtil.newSalt();
        String hashed = PasswordUtil.hash(salt, password);

        User user = new User(
                null,
                username,
                hashed,
                salt,
                (nickname == null || nickname.isBlank()) ? username : nickname.trim(),
                "USER",
                LocalDateTime.now().format(FMT)
        );

        Long id = userDao.insert(user);
        return user.withId(id);
    }

    public User login(String username, String password) {
        if (username == null || password == null) {
            throw new BizException(400, "用户名和密码不能为空");
        }
        User user = userDao.findByUsername(username);
        if (user == null || !PasswordUtil.matches(user.salt(), password, user.password())) {
            throw new BizException(401, "用户名或密码错误");
        }
        return user;
    }

    public User requireById(long id) {
        User user = userDao.findById(id);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user;
    }

    /** 初始化管理员账号（幂等） */
    @Transactional(rollbackFor = Exception.class)
    public void ensureAdmin(String username, String password) {
        if (userDao.findByUsername(username) != null) {
            return;
        }
        String salt = PasswordUtil.newSalt();
        User admin = new User(
                null,
                username,
                PasswordUtil.hash(salt, password),
                salt,
                "管理员",
                "ADMIN",
                LocalDateTime.now().format(FMT)
        );
        userDao.insert(admin);
    }
}
