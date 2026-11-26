package com.homeschool.platform.module.notice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homeschool.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notice")
public class Notice extends BaseEntity {

    private Long publisherId;
    private String scopeType;
    private Long gradeId;
    private Long classId;
    private String title;
    private String content;
    private String status;
    private LocalDateTime publishedAt;
    private LocalDateTime withdrawnAt;
}

