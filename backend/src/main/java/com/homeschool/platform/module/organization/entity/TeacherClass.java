package com.homeschool.platform.module.organization.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homeschool.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("teacher_class")
public class TeacherClass extends BaseEntity {

    private Long teacherId;
    private Long classId;
    private Long subjectId;
    private String relationType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}

