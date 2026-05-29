package com.library.exception;


import com.library.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result handlerException(Exception e) {
        log.error("系统错误",e);
        return Result.error("系统错误");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handlerMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        return Result.error(e.getBindingResult().getFieldError().getDefaultMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public Result handlerBusinessException(BusinessException e) {
        return Result.error(e.getCode(),e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result handlerHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败",e);
        return Result.error(400,"请求参数格式错误，请检查JSON格式");
    }
}
