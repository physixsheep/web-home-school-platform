package com.homeschool.platform.module.organization.controller;

import com.homeschool.platform.common.api.ApiResponse;
import com.homeschool.platform.common.api.PageResponse;
import com.homeschool.platform.module.organization.dto.GradeQuery;
import com.homeschool.platform.module.organization.dto.GradeRequest;
import com.homeschool.platform.module.organization.dto.SchoolClassQuery;
import com.homeschool.platform.module.organization.dto.SchoolClassRequest;
import com.homeschool.platform.module.organization.service.GradeService;
import com.homeschool.platform.module.organization.service.OrganizationOptionService;
import com.homeschool.platform.module.organization.service.SchoolClassService;
import com.homeschool.platform.module.organization.vo.EntityOptionVO;
import com.homeschool.platform.module.organization.vo.GradeVO;
import com.homeschool.platform.module.organization.vo.SchoolClassVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "管理员-年级与班级")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class OrganizationAdminController {

    private final GradeService gradeService;
    private final SchoolClassService schoolClassService;
    private final OrganizationOptionService optionService;

    public OrganizationAdminController(
            GradeService gradeService,
            SchoolClassService schoolClassService,
            OrganizationOptionService optionService) {
        this.gradeService = gradeService;
        this.schoolClassService = schoolClassService;
        this.optionService = optionService;
    }

    @Operation(summary = "分页查询年级")
    @GetMapping("/grades")
    public ApiResponse<PageResponse<GradeVO>> gradePage(GradeQuery query) {
        return ApiResponse.success(gradeService.page(query));
    }

    @Operation(summary = "创建年级")
    @PostMapping("/grades")
    public ApiResponse<Map<String, Long>> createGrade(@Valid @RequestBody GradeRequest request) {
        return ApiResponse.success(Map.of("id", gradeService.create(request)));
    }

    @Operation(summary = "更新年级")
    @PutMapping("/grades/{gradeId}")
    public ApiResponse<Void> updateGrade(
            @PathVariable Long gradeId,
            @Valid @RequestBody GradeRequest request) {
        gradeService.update(gradeId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "删除空年级")
    @DeleteMapping("/grades/{gradeId}")
    public ApiResponse<Void> deleteGrade(@PathVariable Long gradeId) {
        gradeService.delete(gradeId);
        return ApiResponse.success();
    }

    @Operation(summary = "分页查询班级")
    @GetMapping("/classes")
    public ApiResponse<PageResponse<SchoolClassVO>> classPage(SchoolClassQuery query) {
        return ApiResponse.success(schoolClassService.page(query));
    }

    @Operation(summary = "创建班级")
    @PostMapping("/classes")
    public ApiResponse<Map<String, Long>> createClass(
            @Valid @RequestBody SchoolClassRequest request) {
        return ApiResponse.success(Map.of("id", schoolClassService.create(request)));
    }

    @Operation(summary = "更新班级")
    @PutMapping("/classes/{classId}")
    public ApiResponse<Void> updateClass(
            @PathVariable Long classId,
            @Valid @RequestBody SchoolClassRequest request) {
        schoolClassService.update(classId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "删除无关联班级")
    @DeleteMapping("/classes/{classId}")
    public ApiResponse<Void> deleteClass(@PathVariable Long classId) {
        schoolClassService.delete(classId);
        return ApiResponse.success();
    }

    @Operation(summary = "查询班级选择项")
    @GetMapping("/options/classes")
    public ApiResponse<List<EntityOptionVO>> classOptions() {
        return ApiResponse.success(optionService.classes());
    }

    @Operation(summary = "查询教师选择项")
    @GetMapping("/options/teachers")
    public ApiResponse<List<EntityOptionVO>> teacherOptions() {
        return ApiResponse.success(optionService.teachers());
    }

    @Operation(summary = "查询学生选择项")
    @GetMapping("/options/students")
    public ApiResponse<List<EntityOptionVO>> studentOptions() {
        return ApiResponse.success(optionService.students());
    }

    @Operation(summary = "查询家长选择项")
    @GetMapping("/options/parents")
    public ApiResponse<List<EntityOptionVO>> parentOptions() {
        return ApiResponse.success(optionService.parents());
    }

    @Operation(summary = "查询学科选择项")
    @GetMapping("/options/subjects")
    public ApiResponse<List<EntityOptionVO>> subjectOptions() {
        return ApiResponse.success(optionService.subjects());
    }
}

