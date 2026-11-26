package com.homeschool.platform.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 72, message = "密码长度必须为 8 到 72 个字符")
        String newPassword) {
}

