package com.homeschool.platform.module.audit.dto;

import com.homeschool.platform.common.api.PageQuery;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class OperationLogQuery extends PageQuery {

    private String keyword;
    private String module;
    private Integer resultCode;
    private LocalDate dateFrom;
    private LocalDate dateTo;
}

