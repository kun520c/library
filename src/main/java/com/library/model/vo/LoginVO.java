package com.library.model.vo;

public record LoginVO(String tokenType, String accessToken, long expiresIn) {
}
