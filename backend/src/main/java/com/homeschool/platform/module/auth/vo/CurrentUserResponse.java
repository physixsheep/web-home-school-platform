package com.homeschool.platform.module.auth.vo;

import java.util.List;

public record CurrentUserResponse(
        Long id,
        String username,
        String realName,
        List<String> roles) {
}

