package com.homeschool.platform.module.growth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record GrowthSaveRequest(
        @NotNull(message = "学生不能为空")
        Long studentId,

        @NotBlank(message = "成长记录分类不能为空")
        @Pattern(
                regexp = "STUDY|MORAL|ACTIVITY|HONOR|COMPREHENSIVE",
                message = "成长记录分类不正确")
        String category,

        @NotBlank(message = "成长记录标题不能为空")
        @Size(max = 200, message = "标题不能超过 200 个字符")
        String title,

        @NotBlank(message = "成长记录内容不能为空")
        @Size(max = 50000, message = "内容不能超过 50000 个字符")
        String content,

        @NotNull(message = "发生日期不能为空")
        LocalDate occurredAt,

        Boolean visibleToParent,

        List<Long> attachmentIds) {
}

