package com.homeschool.platform.module.assignment.vo;

import java.math.BigDecimal;

public record TeachingOptionVO(
        Long classId,
        String className,
        Long gradeId,
        String gradeName,
        Long subjectId,
        String subjectName,
        BigDecimal fullScore) {
}

