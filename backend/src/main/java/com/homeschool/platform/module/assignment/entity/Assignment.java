package com.homeschool.platform.module.assignment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homeschool.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("assignment")
public class Assignment extends BaseEntity {

    private Long classId;
    private Long teacherId;
    private Long subjectId;
    private String title;
    private String content;
    private LocalDateTime deadline;
    private BigDecimal maxScore;
    private String status;
    private LocalDateTime publishedAt;
}

