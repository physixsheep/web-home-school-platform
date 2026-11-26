package com.homeschool.platform.module.organization.controller;

import com.homeschool.platform.common.api.ApiResponse;
import com.homeschool.platform.common.api.PageResponse;
import com.homeschool.platform.module.organization.dto.BindingStatusRequest;
import com.homeschool.platform.module.organization.dto.ParentBindingQuery;
import com.homeschool.platform.module.organization.dto.ParentBindingRequest;
import com.homeschool.platform.module.organization.dto.StudentClassQuery;
import com.homeschool.platform.module.organization.dto.StudentClassRequest;
import com.homeschool.platform.module.organization.dto.TeacherAssignmentQuery;
import com.homeschool.platform.module.organization.dto.TeacherAssignmentRequest;
import com.homeschool.platform.module.organization.service.ParentBindingService;
import com.homeschool.platform.module.organization.service.StudentClassService;
import com.homeschool.platform.module.organization.service.TeacherAssignmentService;
import com.homeschool.platform.module.organization.vo.ParentBindingVO;
import com.homeschool.platform.module.organization.vo.StudentClassVO;
import com.homeschool.platform.module.organization.vo.TeacherAssignmentVO;
import com.homeschool.platform.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "管理员-人员关系")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class RelationAdminController {

    private final ParentBindingService parentBindingService;
    private final TeacherAssignmentService teacherAssignmentService;
    private final StudentClassService studentClassService;

    public RelationAdminController(
            ParentBindingService parentBindingService,
            TeacherAssignmentService teacherAssignmentService,
            StudentClassService studentClassService) {
        this.parentBindingService = parentBindingService;
        this.teacherAssignmentService = teacherAssignmentService;
        this.studentClassService = studentClassService;
    }

    @Operation(summary = "分页查询家长学生绑定")
    @GetMapping("/parent-bindings")
    public ApiResponse<PageResponse<ParentBindingVO>> parentBindings(ParentBindingQuery query) {
        return ApiResponse.success(parentBindingService.page(query));
    }

    @Operation(summary = "管理员创建并核验家长学生绑定")
    @PostMapping("/parent-bindings")
    public ApiResponse<Map<String, Long>> createParentBinding(
            @Valid @RequestBody ParentBindingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(Map.of(
                "id",
                parentBindingService.create(request, principal.getUserId())));
    }

    @Operation(summary = "恢复或撤销家长学生绑定")
    @PatchMapping("/parent-bindings/{bindingId}/status")
    public ApiResponse<Void> updateParentBindingStatus(
            @PathVariable Long bindingId,
            @Valid @RequestBody BindingStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        parentBindingService.updateStatus(
                bindingId,
                request.status(),
                principal.getUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "分页查询教师任课")
    @GetMapping("/teacher-assignments")
    public ApiResponse<PageResponse<TeacherAssignmentVO>> teacherAssignments(
            TeacherAssignmentQuery query) {
        return ApiResponse.success(teacherAssignmentService.page(query));
    }

    @Operation(summary = "创建教师班主任或任课关联")
    @PostMapping("/teacher-assignments")
    public ApiResponse<Map<String, Long>> createTeacherAssignment(
            @Valid @RequestBody TeacherAssignmentRequest request) {
        return ApiResponse.success(Map.of(
                "id",
                teacherAssignmentService.create(request)));
    }

    @Operation(summary = "结束教师班级关联")
    @PatchMapping("/teacher-assignments/{assignmentId}/end")
    public ApiResponse<Void> endTeacherAssignment(@PathVariable Long assignmentId) {
        teacherAssignmentService.end(assignmentId);
        return ApiResponse.success();
    }

    @Operation(summary = "分页查询学生班级履历")
    @GetMapping("/student-classes")
    public ApiResponse<PageResponse<StudentClassVO>> studentClasses(StudentClassQuery query) {
        return ApiResponse.success(studentClassService.page(query));
    }

    @Operation(summary = "为学生首次分班或办理调班")
    @PostMapping("/student-classes")
    public ApiResponse<Map<String, Long>> assignStudentClass(
            @Valid @RequestBody StudentClassRequest request) {
        return ApiResponse.success(Map.of(
                "id",
                studentClassService.assignOrTransfer(request)));
    }
}

