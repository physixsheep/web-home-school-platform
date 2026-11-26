package com.homeschool.platform.module.assignment.controller;

import com.homeschool.platform.common.api.ApiResponse;
import com.homeschool.platform.module.assignment.dto.AssignmentGradeRequest;
import com.homeschool.platform.module.assignment.dto.AssignmentReturnRequest;
import com.homeschool.platform.module.assignment.service.AssignmentService;
import com.homeschool.platform.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "作业批改")
@RestController
@RequestMapping("/api/submissions")
@PreAuthorize("hasRole('TEACHER')")
public class AssignmentSubmissionController {

    private final AssignmentService assignmentService;

    public AssignmentSubmissionController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @Operation(summary = "批改或修正作业成绩")
    @PostMapping("/{submissionId}/grade")
    public ApiResponse<Void> grade(
            @PathVariable Long submissionId,
            @Valid @RequestBody AssignmentGradeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        assignmentService.grade(submissionId, request, principal);
        return ApiResponse.success();
    }

    @Operation(summary = "退回学生作业")
    @PostMapping("/{submissionId}/return")
    public ApiResponse<Void> returnSubmission(
            @PathVariable Long submissionId,
            @Valid @RequestBody AssignmentReturnRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        assignmentService.returnSubmission(submissionId, request, principal);
        return ApiResponse.success();
    }
}

