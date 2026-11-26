package com.homeschool.platform.module.organization.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SchoolClassRequest(
        @NotNull(message = "年级不能为空")
        Long gradeId,

        @NotBlank(message = "班级名称不能为空")
        @Size(max = 50, message = "班级名称不能超过 50 个字符")
        String className,

        @NotBlank(message = "学年不能为空")
        @Size(max = 20, message = "学年不能超过 20 个字符")
        String academicYear,

        @Min(value = 2000, message = "毕业年份不能早于 2000")
        @Max(value = 2100, message = "毕业年份不能晚于 2100")
        Integer graduationYear,

        @Pattern(regexp = "ENABLED|DISABLED", message = "状态值不正确")
        String status) {
}

