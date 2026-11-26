package com.homeschool.platform.module.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record NoticeSaveRequest(
        @NotBlank(message = "通知标题不能为空")
        @Size(max = 200, message = "通知标题不能超过 200 个字符")
        String title,

        @NotBlank(message = "通知正文不能为空")
        @Size(max = 50000, message = "通知正文不能超过 50000 个字符")
        String content,

        @NotBlank(message = "通知范围不能为空")
        @Pattern(
                regexp = "ALL|GRADE|CLASS",
                message = "通知范围必须为 ALL、GRADE 或 CLASS")
        String scopeType,

        Long gradeId,
        Long classId,
        List<Long> attachmentIds) {
}

