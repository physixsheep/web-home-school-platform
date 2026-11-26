package com.homeschool.platform.module.score.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homeschool.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("examination")
public class Examination extends BaseEntity {

    private String name;
    private String examType;
    private Long gradeId;
    private Long classId;
    private String academicYear;
    private String term;
    private LocalDate examDate;
    private String status;
    private Long createdBy;
}

