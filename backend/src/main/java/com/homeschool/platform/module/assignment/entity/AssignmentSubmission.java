package com.homeschool.platform.module.assignment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("assignment_submission")
public class AssignmentSubmission {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assignmentId;
    private Long studentId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String content;
    private String status;
    private Integer attemptCount;
    private LocalDateTime submittedAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime returnedAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String returnReason;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

