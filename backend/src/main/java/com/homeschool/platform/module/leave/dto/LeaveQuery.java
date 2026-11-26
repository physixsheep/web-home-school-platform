package com.homeschool.platform.module.leave.dto;

import com.homeschool.platform.common.api.PageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaveQuery extends PageQuery {

    private String keyword;
    private String status;
    private Long studentId;
}

