package com.homeschool.platform.module.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String usernameSnapshot;
    private String roleSnapshot;
    private String module;
    private String operation;
    private String httpMethod;
    private String requestUri;
    private String requestParams;
    private Integer resultCode;
    private String clientIp;
    private String userAgent;
    private Long durationMs;
    private LocalDateTime createdAt;
}

