package com.homeschool.platform.module.score.dto;

import com.homeschool.platform.common.api.PageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExamQuery extends PageQuery {

    private String keyword;
    private String status;
    private String academicYear;
}

