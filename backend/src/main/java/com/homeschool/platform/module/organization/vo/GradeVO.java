package com.homeschool.platform.module.organization.vo;

import java.time.LocalDateTime;

public record GradeVO(
        Long id,
        String gradeName,
        Integer gradeLevel,
        Integer sortOrder,
        String status,
        LocalDateTime createdAt) {
}

