package com.homeschool.platform.module.organization.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TeacherAssignmentVO {

    private Long id;
    private Long teacherId;
    private String teacherName;
    private String employeeNo;
    private Long classId;
    private String className;
    private String gradeName;
    private Long subjectId;
    private String subjectName;
    private String relationType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private LocalDateTime createdAt;
}

