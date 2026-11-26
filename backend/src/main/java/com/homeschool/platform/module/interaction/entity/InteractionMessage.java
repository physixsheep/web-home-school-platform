package com.homeschool.platform.module.interaction.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.homeschool.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("interaction_message")
public class InteractionMessage extends BaseEntity {

    private Long senderUserId;
    private String senderRole;
    private Long studentId;
    private Long classId;
    private Long receiverTeacherId;
    private String subject;
    private String content;
    private String status;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime lastReplyAt;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime closedAt;
}

