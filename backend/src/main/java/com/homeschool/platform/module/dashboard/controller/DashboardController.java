package com.homeschool.platform.module.dashboard.controller;

import com.homeschool.platform.common.api.ApiResponse;
import com.homeschool.platform.module.dashboard.service.DashboardService;
import com.homeschool.platform.module.dashboard.vo.AdminDashboardVO;
import com.homeschool.platform.module.dashboard.vo.TeacherDashboardVO;
import com.homeschool.platform.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "数据统计工作台")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "管理员全校实时统计")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminDashboardVO> adminDashboard() {
        return ApiResponse.success(dashboardService.adminDashboard());
    }

    @Operation(summary = "教师工作台实时统计")
    @GetMapping("/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<TeacherDashboardVO> teacherDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(dashboardService.teacherDashboard(principal));
    }
}

