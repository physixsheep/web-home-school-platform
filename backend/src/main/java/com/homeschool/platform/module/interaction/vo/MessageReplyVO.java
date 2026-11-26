package com.homeschool.platform.module.interaction.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageReplyVO {

    private Long id;
    private Long replierUserId;
    private String replierName;
    private String replierRole;
    private String content;
    private LocalDateTime createdAt;
}

