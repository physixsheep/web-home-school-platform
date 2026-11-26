package com.homeschool.platform.module.interaction.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageVO {

    private Long id;
    private Long senderUserId;
    private String senderName;
    private String senderRole;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private String className;
    private String gradeName;
    private Long receiverTeacherId;
    private String receiverName;
    private String subject;
    private String content;
    private String status;
    private LocalDateTime lastReplyAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long replyCount;
    private String lastReplyPreview;
}

