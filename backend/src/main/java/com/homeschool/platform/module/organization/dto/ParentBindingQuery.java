package com.homeschool.platform.module.organization.dto;

import com.homeschool.platform.common.api.PageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParentBindingQuery extends PageQuery {

    private String keyword;
    private String status;
    private Long parentId;
    private Long studentId;
}

