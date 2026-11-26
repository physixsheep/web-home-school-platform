package com.homeschool.platform.module.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record TeacherAssignmentRequest(
        @NotNull(message = "教师不能为空")
        Long teacherId,

        @NotNull(message = "班级不能为空")
        Long classId,

        Long subjectId,

        @NotBlank(message = "关联类型不能为空")
        @Pattern(
                regexp = "HOMEROOM|SUBJECT",
                message = "关联类型必须为 HOMEROOM 或 SUBJECT")
        String relationType,

        @NotNull(message = "开始日期不能为空")
        LocalDate startDate,

        LocalDate endDate) {
}

