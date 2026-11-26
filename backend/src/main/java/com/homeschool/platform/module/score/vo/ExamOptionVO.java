package com.homeschool.platform.module.score.vo;

import java.time.LocalDate;

public record ExamOptionVO(
        Long id,
        String name,
        LocalDate examDate,
        Long gradeId,
        Long classId,
        String gradeName,
        String className) {
}

