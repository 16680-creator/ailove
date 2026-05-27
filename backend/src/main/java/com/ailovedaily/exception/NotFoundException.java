package com.ailovedaily.exception;

/**
 * 资源未找到异常
 */
public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super(404, message);
    }

    public static NotFoundException of(String resource, Object id) {
        return new NotFoundException(String.format("%s不存在: %s", resource, id));
    }
}
