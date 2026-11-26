package com.homeschool.platform.module.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignmentReturnRequest(
        @NotBlank(message = "退回原因不能为空")
        @Size(max = 500, message = "退回原因不能超过 500 个字符")
        String reason) {
}

