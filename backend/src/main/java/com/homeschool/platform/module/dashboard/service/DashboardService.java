package com.homeschool.platform.module.dashboard.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.homeschool.platform.common.exception.BusinessException;
import com.homeschool.platform.module.dashboard.mapper.DashboardMapper;
import com.homeschool.platform.module.dashboard.vo.AdminDashboardVO;
import com.homeschool.platform.module.dashboard.vo.TeacherDashboardVO;
import com.homeschool.platform.module.organization.entity.Teacher;
import com.homeschool.platform.module.organization.mapper.TeacherMapper;
import com.homeschool.platform.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DashboardService {

    private final DashboardMapper dashboardMapper;
    private final TeacherMapper teacherMapper;

    public DashboardService(DashboardMapper dashboardMapper, TeacherMapper teacherMapper) {
        this.dashboardMapper = dashboardMapper;
        this.teacherMapper = teacherMapper;
    }

    public AdminDashboardVO adminDashboard() {
        AdminDashboardVO dashboard = dashboardMapper.selectAdminSummary();
        dashboard.setStudentsByGrade(dashboardMapper.selectStudentsByGrade());
        dashboard.setNoticesByScope(dashboardMapper.selectNoticesByScope());
        return dashboard;
    }

    public TeacherDashboardVO teacherDashboard(UserPrincipal principal) {
        Teacher teacher = teacherMapper.selectOne(
                Wrappers.<Teacher>lambdaQuery()
                        .eq(Teacher::getUserId, principal.getUserId()));
        if (teacher == null || !"ACTIVE".equals(teacher.getStatus())) {
            throw new BusinessException(403, "教师档案不存在或已停用");
        }

        TeacherDashboardVO dashboard = new TeacherDashboardVO();
        var classes = dashboardMapper.selectTeacherClassStats(teacher.getId());
        dashboard.setClasses(classes);
        dashboard.setClassCount((long) classes.size());

        long studentCount = classes.stream()
                .mapToLong(item -> item.getStudentCount() == null ? 0 : item.getStudentCount())
                .sum();
        long assignmentCount = classes.stream()
                .mapToLong(item -> item.getAssignmentCount() == null ? 0 : item.getAssignmentCount())
                .sum();
        long submittedCount = classes.stream()
                .mapToLong(item -> item.getSubmittedCount() == null ? 0 : item.getSubmittedCount())
                .sum();
        long expectedSubmissions = classes.stream()
                .mapToLong(item -> {
                    long students = item.getStudentCount() == null ? 0 : item.getStudentCount();
                    long assignments = item.getAssignmentCount() == null
                            ? 0
                            : item.getAssignmentCount();
                    return students * assignments;
                })
                .sum();

        dashboard.setStudentCount(studentCount);
        dashboard.setPublishedAssignmentCount(assignmentCount);
        dashboard.setSubmissionRate(expectedSubmissions == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(submittedCount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(
                                BigDecimal.valueOf(expectedSubmissions),
                                2,
                                RoundingMode.HALF_UP));
        dashboard.setPendingGradingCount(
                dashboardMapper.selectTeacherPendingGrading(teacher.getId()));
        dashboard.setPendingLeaveCount(
                dashboardMapper.selectTeacherPendingLeave(teacher.getId()));
        return dashboard;
    }
}

