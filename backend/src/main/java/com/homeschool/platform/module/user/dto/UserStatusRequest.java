package com.homeschool.platform.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserStatusRequest(
        @NotBlank(message = "账号状态不能为空")
        @Pattern(
                regexp = "ENABLED|DISABLED",
                message = "账号状态必须为 ENABLED 或 DISABLED")
        String status) {
}

