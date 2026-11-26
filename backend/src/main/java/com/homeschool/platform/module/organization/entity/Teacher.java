package com.homeschool.platform.module.organization.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homeschool.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("teacher")
public class Teacher extends BaseEntity {

    private Long userId;
    private String employeeNo;
    private String gender;
    private String title;
    private String specialty;
    private LocalDate hireDate;
    private String officePhone;
    private String status;
}

