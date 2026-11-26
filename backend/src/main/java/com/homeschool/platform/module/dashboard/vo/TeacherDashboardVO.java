package com.homeschool.platform.module.dashboard.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TeacherDashboardVO {

    private Long classCount;
    private Long studentCount;
    private Long publishedAssignmentCount;
    private BigDecimal submissionRate;
    private Long pendingGradingCount;
    private Long pendingLeaveCount;
    private List<TeacherClassStatVO> classes;
}

