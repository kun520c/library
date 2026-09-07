package com.library.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(HttpStatus.OK.value(), "success", data);
    }

    public static Result<Void> success() {
        return new Result<>(HttpStatus.OK.value(), "success", null);
    }

    public static Result<Void> error(HttpStatus status, String message) {
        return new Result<>(status.value(), message, null);
    }
}
