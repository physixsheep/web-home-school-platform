package com.homeschool.platform.module.assignment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AssignmentSaveRequest(
        @NotBlank(message = "作业标题不能为空")
        @Size(max = 200, message = "作业标题不能超过 200 个字符")
        String title,

        @NotBlank(message = "作业内容不能为空")
        @Size(max = 50000, message = "作业内容不能超过 50000 个字符")
        String content,

        @NotNull(message = "作业班级不能为空")
        Long classId,

        @NotNull(message = "作业学科不能为空")
        Long subjectId,

        @NotNull(message = "截止时间不能为空")
        LocalDateTime deadline,

        @DecimalMin(value = "0.01", message = "作业满分必须大于 0")
        BigDecimal maxScore,

        List<Long> attachmentIds) {
}

