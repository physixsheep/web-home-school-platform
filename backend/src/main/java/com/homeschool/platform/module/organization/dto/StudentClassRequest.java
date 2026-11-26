package com.homeschool.platform.module.organization.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record StudentClassRequest(
        @NotNull(message = "学生不能为空")
        Long studentId,

        @NotNull(message = "班级不能为空")
        Long classId,

        @NotNull(message = "入班日期不能为空")
        LocalDate startDate) {
}

