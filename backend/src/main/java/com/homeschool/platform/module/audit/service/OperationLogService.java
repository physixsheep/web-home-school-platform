package com.homeschool.platform.module.audit.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homeschool.platform.common.api.PageResponse;
import com.homeschool.platform.module.audit.dto.OperationLogQuery;
import com.homeschool.platform.module.audit.entity.OperationLog;
import com.homeschool.platform.module.audit.mapper.OperationLogMapper;
import com.homeschool.platform.module.audit.vo.OperationLogVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperationLogService {

    private final OperationLogMapper logMapper;

    public OperationLogService(OperationLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    public PageResponse<OperationLogVO> page(OperationLogQuery query) {
        String keyword = StringUtils.hasText(query.getKeyword())
                ? query.getKeyword().trim()
                : null;
        var wrapper = Wrappers.<OperationLog>lambdaQuery()
                .and(keyword != null, item -> item
                        .like(OperationLog::getUsernameSnapshot, keyword)
                        .or()
                        .like(OperationLog::getModule, keyword)
                        .or()
                        .like(OperationLog::getOperation, keyword)
                        .or()
                        .like(OperationLog::getRequestUri, keyword))
                .eq(StringUtils.hasText(query.getModule()),
                        OperationLog::getModule,
                        query.getModule())
                .eq(query.getResultCode() != null,
                        OperationLog::getResultCode,
                        query.getResultCode())
                .ge(query.getDateFrom() != null,
                        OperationLog::getCreatedAt,
                        query.getDateFrom() == null
                                ? null
                                : query.getDateFrom().atStartOfDay())
                .lt(query.getDateTo() != null,
                        OperationLog::getCreatedAt,
                        query.getDateTo() == null
                                ? null
                                : query.getDateTo().plusDays(1).atStartOfDay())
                .orderByDesc(OperationLog::getId);
        IPage<OperationLog> source = logMapper.selectPage(
                new Page<>(query.normalizedPage(), query.normalizedSize()),
                wrapper);
        Page<OperationLogVO> target = new Page<>(
                source.getCurrent(),
                source.getSize(),
                source.getTotal());
        target.setRecords(source.getRecords().stream().map(this::toVO).toList());
        return PageResponse.from(target);
    }

    private OperationLogVO toVO(OperationLog log) {
        return new OperationLogVO(
                log.getId(),
                log.getUserId(),
                log.getUsernameSnapshot(),
                log.getRoleSnapshot(),
                log.getModule(),
                log.getOperation(),
                log.getHttpMethod(),
                log.getRequestUri(),
                log.getRequestParams(),
                log.getResultCode(),
                log.getClientIp(),
                log.getUserAgent(),
                log.getDurationMs(),
                log.getCreatedAt());
    }
}

