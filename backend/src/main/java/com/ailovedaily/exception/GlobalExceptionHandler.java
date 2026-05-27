package com.ailovedaily.exception;

import com.ailovedaily.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 全局异常处理。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResultVO<Map<String, Object>> handleBusinessException(BusinessException exception) {
        log.warn("业务异常: {}", exception.getMessage());
        ResultVO<Map<String, Object>> result = ResultVO.error(exception.getCode(), exception.getMessage());
        result.setData(exception.getData());
        return result;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultVO<Void> handleValidationException(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return ResultVO.error(400, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResultVO<Void> handleMessageNotReadableException(HttpMessageNotReadableException exception) {
        return ResultVO.error(400, "请求参数格式不正确");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResultVO<Void> handleRuntimeException(RuntimeException exception) {
        log.warn("业务异常: {}", exception.getMessage());
        return ResultVO.error(exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResultVO<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return ResultVO.error("系统繁忙，请稍后重试");
    }
}
