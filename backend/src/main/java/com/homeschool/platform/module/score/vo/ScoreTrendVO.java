package com.homeschool.platform.module.score.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ScoreTrendVO {

    private Long examId;
    private String examName;
    private LocalDate examDate;
    private String academicYear;
    private String term;
    private BigDecimal totalScore;
    private BigDecimal averageScore;
    private Integer subjectCount;
}

