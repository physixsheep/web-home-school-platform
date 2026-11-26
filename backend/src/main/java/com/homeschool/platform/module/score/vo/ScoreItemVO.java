package com.homeschool.platform.module.score.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ScoreItemVO {

    private Long examId;
    private String examName;
    private String examType;
    private LocalDate examDate;
    private String academicYear;
    private String term;
    private Long classId;
    private String className;
    private Long subjectId;
    private String subjectName;
    private BigDecimal score;
    private BigDecimal fullScore;
    private Integer isAbsent;
    private String remark;
}

