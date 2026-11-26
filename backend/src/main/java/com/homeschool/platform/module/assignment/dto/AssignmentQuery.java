package com.homeschool.platform.module.assignment.dto;

import com.homeschool.platform.common.api.PageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignmentQuery extends PageQuery {

    private String keyword;
    private String status;
    private Long studentId;
}

