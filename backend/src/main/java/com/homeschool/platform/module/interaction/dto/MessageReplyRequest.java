package com.homeschool.platform.module.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageReplyRequest(
        @NotBlank(message = "回复内容不能为空")
        @Size(max = 10000, message = "回复内容不能超过 10000 个字符")
        String content) {
}

