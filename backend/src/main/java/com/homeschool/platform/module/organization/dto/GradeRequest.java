package com.homeschool.platform.module.organization.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GradeRequest(
        @NotBlank(message = "年级名称不能为空")
        @Size(max = 30, message = "年级名称不能超过 30 个字符")
        String gradeName,

        @NotNull(message = "年级层级不能为空")
        @Min(value = 1, message = "年级层级最小为 1")
        @Max(value = 3, message = "年级层级最大为 3")
        Integer gradeLevel,

        @NotNull(message = "排序值不能为空")
        @Min(value = 0, message = "排序值不能小于 0")
        Integer sortOrder,

        @Pattern(regexp = "ENABLED|DISABLED", message = "状态值不正确")
        String status) {
}

