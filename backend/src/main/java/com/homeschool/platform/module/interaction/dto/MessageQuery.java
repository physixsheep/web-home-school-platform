package com.homeschool.platform.module.interaction.dto;

import com.homeschool.platform.common.api.PageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageQuery extends PageQuery {

    private String keyword;
    private String status;
    private Long studentId;
}

