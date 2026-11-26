package com.homeschool.platform.module.auth.vo;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        CurrentUserResponse user) {
}

