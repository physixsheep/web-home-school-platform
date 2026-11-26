package com.homeschool.platform.module.score.controller;

import com.homeschool.platform.common.api.ApiResponse;
import com.homeschool.platform.common.api.PageResponse;
import com.homeschool.platform.module.score.dto.ExamQuery;
import com.homeschool.platform.module.score.dto.ExamRequest;
import com.homeschool.platform.module.score.dto.SubjectRequest;
import com.homeschool.platform.module.score.service.AcademicAdminService;
import com.homeschool.platform.module.score.vo.ExamVO;
import com.homeschool.platform.module.score.vo.SubjectVO;
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

@Tag(name = "管理员-考试与学科")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AcademicAdminController {

    private final AcademicAdminService academicService;

    public AcademicAdminController(AcademicAdminService academicService) {
        this.academicService = academicService;
    }

    @Operation(summary = "分页查询考试")
    @GetMapping("/exams")
    public ApiResponse<PageResponse<ExamVO>> exams(ExamQuery query) {
        return ApiResponse.success(academicService.examPage(query));
    }

    @Operation(summary = "创建考试")
    @PostMapping("/exams")
    public ApiResponse<Map<String, Long>> createExam(
            @Valid @RequestBody ExamRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(Map.of(
                "id",
                academicService.createExam(request, principal)));
    }

    @Operation(summary = "更新考试")
    @PutMapping("/exams/{examId}")
    public ApiResponse<Void> updateExam(
            @PathVariable Long examId,
            @Valid @RequestBody ExamRequest request) {
        academicService.updateExam(examId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "删除无成绩考试")
    @DeleteMapping("/exams/{examId}")
    public ApiResponse<Void> deleteExam(@PathVariable Long examId) {
        academicService.deleteExam(examId);
        return ApiResponse.success();
    }

    @Operation(summary = "分页查询学科")
    @GetMapping("/subjects")
    public ApiResponse<PageResponse<SubjectVO>> subjects(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.success(
                academicService.subjectPage(keyword, status, page, size));
    }

    @Operation(summary = "创建学科")
    @PostMapping("/subjects")
    public ApiResponse<Map<String, Long>> createSubject(
            @Valid @RequestBody SubjectRequest request) {
        return ApiResponse.success(Map.of(
                "id",
                academicService.createSubject(request)));
    }

    @Operation(summary = "更新学科")
    @PutMapping("/subjects/{subjectId}")
    public ApiResponse<Void> updateSubject(
            @PathVariable Long subjectId,
            @Valid @RequestBody SubjectRequest request) {
        academicService.updateSubject(subjectId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "删除无关联学科")
    @DeleteMapping("/subjects/{subjectId}")
    public ApiResponse<Void> deleteSubject(@PathVariable Long subjectId) {
        academicService.deleteSubject(subjectId);
        return ApiResponse.success();
    }
}

