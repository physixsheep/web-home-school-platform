package com.homeschool.platform.module.organization.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homeschool.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("subject")
public class Subject extends BaseEntity {

    private String subjectCode;
    private String subjectName;
    private BigDecimal defaultFullScore;
    private Integer sortOrder;
    private String status;
}

