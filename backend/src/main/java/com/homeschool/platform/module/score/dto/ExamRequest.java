package com.homeschool.platform.module.score.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ExamRequest(
        @NotBlank(message = "考试名称不能为空")
        @Size(max = 150, message = "考试名称不能超过 150 个字符")
        String name,

        @NotBlank(message = "考试类型不能为空")
        @Size(max = 30, message = "考试类型不能超过 30 个字符")
        String examType,

        @NotNull(message = "考试年级不能为空")
        Long gradeId,

        Long classId,

        @NotBlank(message = "学年不能为空")
        @Size(max = 20, message = "学年不能超过 20 个字符")
        String academicYear,

        @NotBlank(message = "学期不能为空")
        @Size(max = 20, message = "学期不能超过 20 个字符")
        String term,

        @NotNull(message = "考试日期不能为空")
        LocalDate examDate,

        @NotBlank(message = "考试状态不能为空")
        @Pattern(
                regexp = "DRAFT|PUBLISHED|ARCHIVED",
                message = "考试状态不正确")
        String status) {
}

