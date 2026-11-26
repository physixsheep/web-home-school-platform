package com.homeschool.platform.module.score.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SubjectRequest(
        @NotBlank(message = "学科编码不能为空")
        @Size(max = 30, message = "学科编码不能超过 30 个字符")
        String subjectCode,

        @NotBlank(message = "学科名称不能为空")
        @Size(max = 50, message = "学科名称不能超过 50 个字符")
        String subjectName,

        @NotNull(message = "默认满分不能为空")
        @DecimalMin(value = "0.01", message = "默认满分必须大于 0")
        BigDecimal defaultFullScore,

        @NotNull(message = "排序值不能为空")
        Integer sortOrder,

        @Pattern(regexp = "ENABLED|DISABLED", message = "学科状态不正确")
        String status) {
}

