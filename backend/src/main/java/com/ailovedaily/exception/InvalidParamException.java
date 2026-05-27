package com.ailovedaily.exception;

/**
 * 参数无效异常
 */
public class InvalidParamException extends BusinessException {

    public InvalidParamException(String message) {
        super(400, message);
    }
}
