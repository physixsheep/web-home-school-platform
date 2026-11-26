package com.homeschool.platform.module.organization.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homeschool.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("parent")
public class Parent extends BaseEntity {

    private Long userId;
    private String gender;
    private String occupation;
    private String emergencyContact;
    private String status;
}

