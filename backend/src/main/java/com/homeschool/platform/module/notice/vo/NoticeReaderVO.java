package com.homeschool.platform.module.notice.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeReaderVO {

    private Long userId;
    private String username;
    private String realName;
    private String roleCode;
    private String roleName;
    private LocalDateTime readAt;
}

