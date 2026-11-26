package com.homeschool.platform.module.assignment.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.homeschool.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("assignment_comment")
public class AssignmentComment extends BaseEntity {

    private Long submissionId;
    private Long teacherId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal score;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String comment;
    private LocalDateTime gradedAt;

}

