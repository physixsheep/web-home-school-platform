package com.homeschool.platform.module.leave.controller;

import com.homeschool.platform.common.api.ApiResponse;
import com.homeschool.platform.common.api.PageResponse;
import com.homeschool.platform.module.leave.dto.LeaveCreateRequest;
import com.homeschool.platform.module.leave.dto.LeaveDecisionRequest;
import com.homeschool.platform.module.leave.dto.LeaveQuery;
import com.homeschool.platform.module.leave.service.LeaveService;
import com.homeschool.platform.module.leave.vo.LeaveVO;
import com.homeschool.platform.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "请假审批")
@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @Operation(summary = "查询本人、绑定孩子或全校请假记录")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT', 'PARENT')")
    public ApiResponse<PageResponse<LeaveVO>> page(
            LeaveQuery query,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(leaveService.page(query, principal));
    }

    @Operation(summary = "班主任查询本班请假审批列表")
    @GetMapping("/approval")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<PageResponse<LeaveVO>> approvalPage(
            LeaveQuery query,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(leaveService.approvalPage(query, principal));
    }

    @Operation(summary = "查询请假详情")
    @GetMapping("/{leaveId}")
    public ApiResponse<LeaveVO> detail(
            @PathVariable Long leaveId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(leaveService.detail(leaveId, principal));
    }

    @Operation(summary = "学生提交请假申请")
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Map<String, Long>> create(
            @Valid @RequestBody LeaveCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(Map.of(
                "id",
                leaveService.create(request, principal)));
    }

    @Operation(summary = "学生撤销待审批请假")
    @PostMapping("/{leaveId}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Void> cancel(
            @PathVariable Long leaveId,
            @AuthenticationPrincipal UserPrincipal principal) {
        leaveService.cancel(leaveId, principal);
        return ApiResponse.success();
    }

    @Operation(summary = "班主任批准或驳回请假")
    @PostMapping("/{leaveId}/decision")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> decide(
            @PathVariable Long leaveId,
            @Valid @RequestBody LeaveDecisionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        leaveService.decide(leaveId, request, principal);
        return ApiResponse.success();
    }
}

