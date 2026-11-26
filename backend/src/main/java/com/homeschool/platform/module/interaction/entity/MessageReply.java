package com.homeschool.platform.module.interaction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message_reply")
public class MessageReply {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long messageId;
    private Long replierUserId;
    private String replierRole;
    private String content;
    private LocalDateTime createdAt;
}

