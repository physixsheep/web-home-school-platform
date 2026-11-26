package com.homeschool.platform.module.audit.vo;

import java.time.LocalDateTime;

public record OperationLogVO(
        Long id,
        Long userId,
        String username,
        String roles,
        String module,
        String operation,
        String httpMethod,
        String requestUri,
        String requestParams,
        Integer resultCode,
        String clientIp,
        String userAgent,
        Long durationMs,
        LocalDateTime createdAt) {
}

