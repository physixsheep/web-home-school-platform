package com.homeschool.platform.module.organization.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homeschool.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("school_class")
public class SchoolClass extends BaseEntity {

    private Long gradeId;
    private String className;
    private String academicYear;
    private Integer graduationYear;
    private String status;
}

