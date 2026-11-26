package com.homeschool.platform.module.score.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScoreEntryRowVO {

    private Long studentId;
    private String studentName;
    private String studentNo;
    private BigDecimal score;
    private Integer isAbsent;
    private String remark;
}

