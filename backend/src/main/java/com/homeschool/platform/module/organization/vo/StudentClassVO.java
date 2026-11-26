package com.homeschool.platform.module.organization.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class StudentClassVO {

    private Long id;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private Long classId;
    private String className;
    private String gradeName;
    private String academicYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private LocalDateTime createdAt;
}

