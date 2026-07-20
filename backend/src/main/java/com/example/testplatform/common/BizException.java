package com.example.testplatform.common;

/**
 * 业务异常：code 直接复用 HTTP 状态码
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
