package com.homeschool.platform.module.organization.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ParentBindingVO {

    private Long id;
    private Long parentId;
    private String parentName;
    private String parentAccount;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private String relationship;
    private Integer isPrimary;
    private String status;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
}

