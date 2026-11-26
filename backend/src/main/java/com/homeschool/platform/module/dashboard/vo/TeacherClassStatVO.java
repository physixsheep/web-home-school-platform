package com.homeschool.platform.module.dashboard.vo;

import lombok.Data;

@Data
public class TeacherClassStatVO {

    private Long classId;
    private String className;
    private String gradeName;
    private Long studentCount;
    private Long assignmentCount;
    private Long submittedCount;
}

