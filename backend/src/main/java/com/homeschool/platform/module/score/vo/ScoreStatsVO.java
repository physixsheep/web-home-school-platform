package com.homeschool.platform.module.score.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScoreStatsVO {

    private Long enteredCount;
    private Long absentCount;
    private BigDecimal averageScore;
    private BigDecimal highestScore;
    private BigDecimal lowestScore;
}

