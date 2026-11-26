package com.homeschool.platform.module.organization.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homeschool.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("student")
public class Student extends BaseEntity {

    private Long userId;
    private String studentNo;
    private String gender;
    private LocalDate birthDate;
    private LocalDate enrollmentDate;
    private String status;
}

