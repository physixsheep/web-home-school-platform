package com.homeschool.platform.module.user.dto;

import com.homeschool.platform.common.api.PageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserQuery extends PageQuery {

    private String keyword;
    private String roleCode;
    private String status;
}

