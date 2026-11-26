package com.homeschool.platform.module.notice.vo;

import com.homeschool.platform.module.file.vo.FileAttachmentVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NoticeVO {

    private Long id;
    private Long publisherId;
    private String publisherName;
    private String scopeType;
    private Long gradeId;
    private String gradeName;
    private Long classId;
    private String className;
    private String title;
    private String content;
    private String status;
    private LocalDateTime publishedAt;
    private LocalDateTime withdrawnAt;
    private Boolean isRead;
    private LocalDateTime readAt;
    private Long readCount;
    private List<FileAttachmentVO> attachments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

