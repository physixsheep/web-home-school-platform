package com.homeschool.platform.module.score.controller;

import com.homeschool.platform.common.api.ApiResponse;
import com.homeschool.platform.module.score.dto.ScoreBatchRequest;
import com.homeschool.platform.module.score.service.ScoreService;
import com.homeschool.platform.module.score.vo.ScoreEntryRowVO;
import com.homeschool.platform.module.score.vo.ScoreReportVO;
import com.homeschool.platform.module.score.vo.ScoreStatsVO;
import com.homeschool.platform.module.score.vo.ScoreWorkspaceOptionsVO;
import com.homeschool.platform.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "学业成绩")
@RestController
@RequestMapping("/api/scores")
public class ScoreController {

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @Operation(summary = "查询教师成绩录入工作区选项")
    @GetMapping("/workspace-options")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<ScoreWorkspaceOptionsVO> workspaceOptions(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(scoreService.workspaceOptions(principal));
    }

    @Operation(summary = "查询班级学生成绩录入表")
    @GetMapping("/entry")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<List<ScoreEntryRowVO>> entryRows(
            @RequestParam Long examId,
            @RequestParam Long classId,
            @RequestParam Long subjectId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(
                scoreService.entryRows(examId, classId, subjectId, principal));
    }

    @Operation(summary = "批量保存或修改成绩")
    @PutMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<Void> batchSave(
            @Valid @RequestBody ScoreBatchRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        scoreService.batchSave(request, principal);
        return ApiResponse.success();
    }

    @Operation(summary = "查询班级单科成绩统计")
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<ScoreStatsVO> statistics(
            @RequestParam Long examId,
            @RequestParam Long classId,
            @RequestParam Long subjectId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(
                scoreService.statistics(examId, classId, subjectId, principal));
    }

    @Operation(summary = "查询学生或绑定孩子的成绩报告")
    @GetMapping("/report")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT', 'PARENT')")
    public ApiResponse<ScoreReportVO> report(
            @RequestParam(required = false) Long studentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(scoreService.report(studentId, principal));
    }
}

