package com.homeschool.platform.module.score.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ExamVO {

    private Long id;
    private String name;
    private String examType;
    private Long gradeId;
    private String gradeName;
    private Long classId;
    private String className;
    private String academicYear;
    private String term;
    private LocalDate examDate;
    private String status;
    private LocalDateTime createdAt;
}

