package com.homeschool.platform.module.assignment.controller;

import com.homeschool.platform.common.api.ApiResponse;
import com.homeschool.platform.common.api.PageResponse;
import com.homeschool.platform.module.assignment.dto.AssignmentQuery;
import com.homeschool.platform.module.assignment.dto.AssignmentSaveRequest;
import com.homeschool.platform.module.assignment.dto.AssignmentSubmissionRequest;
import com.homeschool.platform.module.assignment.dto.SubmissionQuery;
import com.homeschool.platform.module.assignment.service.AssignmentService;
import com.homeschool.platform.module.assignment.vo.AssignmentOptionsVO;
import com.homeschool.platform.module.assignment.vo.AssignmentVO;
import com.homeschool.platform.module.assignment.vo.SubmissionRowVO;
import com.homeschool.platform.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "作业")
@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @Operation(summary = "分页查询当前用户可阅读的作业")
    @GetMapping
    public ApiResponse<PageResponse<AssignmentVO>> readable(
            AssignmentQuery query,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(assignmentService.readablePage(query, principal));
    }

    @Operation(summary = "分页查询教师本人发布的作业")
    @GetMapping("/manage")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<PageResponse<AssignmentVO>> manage(
            AssignmentQuery query,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(assignmentService.managePage(query, principal));
    }

    @Operation(summary = "查询教师任教学科或家长孩子选项")
    @GetMapping("/options")
    @PreAuthorize("hasAnyRole('TEACHER', 'PARENT')")
    public ApiResponse<AssignmentOptionsVO> options(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(assignmentService.options(principal));
    }

    @Operation(summary = "查询作业详情")
    @GetMapping("/{assignmentId}")
    public ApiResponse<AssignmentVO> detail(
            @PathVariable Long assignmentId,
            @RequestParam(required = false) Long studentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(
                assignmentService.detail(assignmentId, studentId, principal));
    }

    @Operation(summary = "创建作业草稿")
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Map<String, Long>> create(
            @Valid @RequestBody AssignmentSaveRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(Map.of(
                "id",
                assignmentService.create(request, principal)));
    }

    @Operation(summary = "更新作业")
    @PutMapping("/{assignmentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> update(
            @PathVariable Long assignmentId,
            @Valid @RequestBody AssignmentSaveRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        assignmentService.update(assignmentId, request, principal);
        return ApiResponse.success();
    }

    @Operation(summary = "发布作业")
    @PostMapping("/{assignmentId}/publish")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> publish(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        assignmentService.publish(assignmentId, principal);
        return ApiResponse.success();
    }

    @Operation(summary = "关闭作业")
    @PostMapping("/{assignmentId}/close")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> close(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        assignmentService.close(assignmentId, principal);
        return ApiResponse.success();
    }

    @Operation(summary = "删除作业草稿")
    @DeleteMapping("/{assignmentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> deleteDraft(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        assignmentService.deleteDraft(assignmentId, principal);
        return ApiResponse.success();
    }

    @Operation(summary = "分页查询作业提交清单")
    @GetMapping("/{assignmentId}/submissions")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<PageResponse<SubmissionRowVO>> submissions(
            @PathVariable Long assignmentId,
            SubmissionQuery query,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(
                assignmentService.submissions(assignmentId, query, principal));
    }

    @Operation(summary = "学生提交或重交作业")
    @PostMapping("/{assignmentId}/submission")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Map<String, Long>> submit(
            @PathVariable Long assignmentId,
            @Valid @RequestBody AssignmentSubmissionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(Map.of(
                "submissionId",
                assignmentService.submit(assignmentId, request, principal)));
    }
}

