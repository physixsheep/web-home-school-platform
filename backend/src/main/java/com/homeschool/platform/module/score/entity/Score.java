package com.homeschool.platform.module.score.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.homeschool.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("score")
public class Score extends BaseEntity {

    private Long examId;
    private Long studentId;
    private Long subjectId;
    private Long classId;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal score;
    private Integer isAbsent;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;
    private Long enteredBy;
    private Long updatedBy;
}

