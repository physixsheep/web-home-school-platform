package com.homeschool.platform.module.growth.dto;

import com.homeschool.platform.common.api.PageQuery;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class GrowthQuery extends PageQuery {

    private String keyword;
    private String category;
    private Long studentId;
    private LocalDate dateFrom;
    private LocalDate dateTo;
}

