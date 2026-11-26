package com.homeschool.platform.module.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MessageCreateRequest(
        @NotNull(message = "涉及学生不能为空")
        Long studentId,

        @NotNull(message = "接收教师不能为空")
        Long teacherId,

        @NotBlank(message = "留言主题不能为空")
        @Size(max = 200, message = "留言主题不能超过 200 个字符")
        String subject,

        @NotBlank(message = "留言内容不能为空")
        @Size(max = 10000, message = "留言内容不能超过 10000 个字符")
        String content) {
}

