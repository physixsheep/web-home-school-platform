package com.homeschool.platform.module.organization.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SchoolClassVO {

    private Long id;
    private Long gradeId;
    private String gradeName;
    private String className;
    private String academicYear;
    private Integer graduationYear;
    private String status;
    private Long studentCount;
    private Long teacherCount;
    private LocalDateTime createdAt;
}

