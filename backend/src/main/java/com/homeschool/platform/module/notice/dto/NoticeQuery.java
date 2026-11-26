package com.homeschool.platform.module.notice.dto;

import com.homeschool.platform.common.api.PageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoticeQuery extends PageQuery {

    private String keyword;
    private String status;
    private String scopeType;
}

