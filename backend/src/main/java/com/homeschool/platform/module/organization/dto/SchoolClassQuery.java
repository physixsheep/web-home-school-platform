package com.homeschool.platform.module.organization.dto;

import com.homeschool.platform.common.api.PageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SchoolClassQuery extends PageQuery {

    private String keyword;
    private Long gradeId;
    private String academicYear;
    private String status;
}

