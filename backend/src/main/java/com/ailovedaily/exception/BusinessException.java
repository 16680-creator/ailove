package com.ailovedaily.exception;

import lombok.Getter;

import java.util.Map;

/**
 * 业务异常。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;
    private final transient Map<String, Object> data;

    public BusinessException(String message) {
        this(500, message, null);
    }

    public BusinessException(Integer code, String message) {
        this(code, message, null);
    }

    public BusinessException(Integer code, String message, Map<String, Object> data) {
        super(message);
        this.code = code;
        this.data = data;
    }
}
