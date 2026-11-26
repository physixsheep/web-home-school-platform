package com.homeschool.platform.module.dashboard.vo;

import lombok.Data;

import java.util.List;

@Data
public class AdminDashboardVO {

    private Long studentCount;
    private Long teacherCount;
    private Long parentCount;
    private Long classCount;
    private Long publishedNoticeCount;
    private Long pendingLeaveCount;
    private List<NameValueVO> studentsByGrade;
    private List<NameValueVO> noticesByScope;
}

