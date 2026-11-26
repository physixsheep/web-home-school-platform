package com.homeschool.platform.module.growth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homeschool.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("growth_record")
public class GrowthRecord extends BaseEntity {

    private Long studentId;
    private Long classId;
    private String category;
    private String title;
    private String content;
    private LocalDate occurredAt;
    private Long recorderTeacherId;
    private Integer visibleToParent;
}

