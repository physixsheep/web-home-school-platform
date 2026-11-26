package com.homeschool.platform.module.organization.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homeschool.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("student_class")
public class StudentClass extends BaseEntity {

    private Long studentId;
    private Long classId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}

