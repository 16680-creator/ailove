package com.ailovedaily.exception;

/**
 * 无权限异常
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super(403, message);
    }
}
