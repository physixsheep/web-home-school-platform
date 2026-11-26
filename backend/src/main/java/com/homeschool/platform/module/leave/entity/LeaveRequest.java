package com.homeschool.platform.module.leave.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.homeschool.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("leave_request")
public class LeaveRequest extends BaseEntity {

    private Long studentId;
    private Long classId;
    private String leaveType;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String reason;
    private String status;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long approverTeacherId;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime approvedAt;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String approvalOpinion;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime cancelledAt;
    private Integer version;
}

