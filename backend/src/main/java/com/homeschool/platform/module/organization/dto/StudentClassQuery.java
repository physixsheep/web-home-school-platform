package com.homeschool.platform.module.organization.dto;

import com.homeschool.platform.common.api.PageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentClassQuery extends PageQuery {

    private String keyword;
    private Long studentId;
    private Long classId;
    private String status;
}

