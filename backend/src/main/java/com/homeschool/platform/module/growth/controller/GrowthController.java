package com.homeschool.platform.module.growth.controller;

import com.homeschool.platform.common.api.ApiResponse;
import com.homeschool.platform.common.api.PageResponse;
import com.homeschool.platform.module.growth.dto.GrowthQuery;
import com.homeschool.platform.module.growth.dto.GrowthSaveRequest;
import com.homeschool.platform.module.growth.service.GrowthService;
import com.homeschool.platform.module.growth.vo.GrowthVO;
import com.homeschool.platform.module.organization.vo.EntityOptionVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "学生成长档案")
@RestController
@RequestMapping("/api/growth-records")
public class GrowthController {

    private final GrowthService growthService;

    public GrowthController(GrowthService growthService) {
        this.growthService = growthService;
    }

    @Operation(summary = "查询本人、绑定孩子或全校成长档案")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT', 'PARENT')")
    public ApiResponse<PageResponse<GrowthVO>> page(
            GrowthQuery query,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(growthService.readablePage(query, principal));
    }

    @Operation(summary = "班主任查询本班成长档案")
    @GetMapping("/manage")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<PageResponse<GrowthVO>> manage(
            GrowthQuery query,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(growthService.managePage(query, principal));
    }

    @Operation(summary = "查询班主任可维护学生选项")
    @GetMapping("/options/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<List<EntityOptionVO>> studentOptions(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(growthService.studentOptions(principal));
    }

    @Operation(summary = "查询成长记录详情")
    @GetMapping("/{recordId}")
    public ApiResponse<GrowthVO> detail(
            @PathVariable Long recordId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(growthService.detail(recordId, principal));
    }

    @Operation(summary = "班主任创建成长记录")
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Map<String, Long>> create(
            @Valid @RequestBody GrowthSaveRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(Map.of(
                "id",
                growthService.create(request, principal)));
    }

    @Operation(summary = "更新成长记录")
    @PutMapping("/{recordId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<Void> update(
            @PathVariable Long recordId,
            @Valid @RequestBody GrowthSaveRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        growthService.update(recordId, request, principal);
        return ApiResponse.success();
    }

    @Operation(summary = "删除成长记录")
    @DeleteMapping("/{recordId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<Void> delete(
            @PathVariable Long recordId,
            @AuthenticationPrincipal UserPrincipal principal) {
        growthService.delete(recordId, principal);
        return ApiResponse.success();
    }
}

