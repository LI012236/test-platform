package com.example.testplatform.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * 密码工具：SHA-256(salt + 明文)
 *
 * 说明：真实项目应该用 BCrypt/Argon2（自带盐、抗暴力破解）。
 * 这里为了不引额外依赖用 SHA-256 + 随机盐，面试时讲清"为什么 BCrypt 更好"即可。
 */
public final class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private PasswordUtil() {
    }

    public static String newSalt() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        return toHex(bytes);
    }

    public static String hash(String salt, String rawPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((salt + rawPassword).getBytes(StandardCharsets.UTF_8));
            return toHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    public static boolean matches(String salt, String rawPassword, String hashed) {
        return hash(salt, rawPassword).equals(hashed);
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }
}
