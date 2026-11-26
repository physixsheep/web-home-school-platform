package com.homeschool.platform.module.score.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubjectVO(
        Long id,
        String subjectCode,
        String subjectName,
        BigDecimal defaultFullScore,
        Integer sortOrder,
        String status,
        LocalDateTime createdAt) {
}

