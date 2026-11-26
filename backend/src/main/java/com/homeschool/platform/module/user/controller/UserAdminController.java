package com.homeschool.platform.module.user.controller;

import com.homeschool.platform.common.api.ApiResponse;
import com.homeschool.platform.common.api.PageResponse;
import com.homeschool.platform.module.user.dto.CreateUserRequest;
import com.homeschool.platform.module.user.dto.ResetPasswordRequest;
import com.homeschool.platform.module.user.dto.UpdateUserRequest;
import com.homeschool.platform.module.user.dto.UserQuery;
import com.homeschool.platform.module.user.dto.UserStatusRequest;
import com.homeschool.platform.module.user.service.UserAdminService;
import com.homeschool.platform.module.user.vo.RoleOptionVO;
import com.homeschool.platform.module.user.vo.UserAdminVO;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "管理员-用户管理")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @Operation(summary = "分页查询用户")
    @GetMapping("/users")
    public ApiResponse<PageResponse<UserAdminVO>> page(UserQuery query) {
        return ApiResponse.success(userAdminService.page(query));
    }

    @Operation(summary = "查询角色选项")
    @GetMapping("/roles")
    public ApiResponse<List<RoleOptionVO>> roles() {
        return ApiResponse.success(userAdminService.roles());
    }

    @Operation(summary = "创建用户和对应人员档案")
    @PostMapping("/users")
    public ApiResponse<Map<String, Long>> create(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(Map.of(
                "id",
                userAdminService.create(request, principal.getUserId())));
    }

    @Operation(summary = "更新用户基础资料")
    @PutMapping("/users/{userId}")
    public ApiResponse<Void> update(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        userAdminService.update(userId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "启用或禁用用户")
    @PatchMapping("/users/{userId}/status")
    public ApiResponse<Void> updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UserStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        userAdminService.updateStatus(userId, request.status(), principal.getUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "重置用户密码并撤销旧 Token")
    @PostMapping("/users/{userId}/reset-password")
    public ApiResponse<Void> resetPassword(
            @PathVariable Long userId,
            @Valid @RequestBody ResetPasswordRequest request) {
        userAdminService.resetPassword(userId, request);
        return ApiResponse.success();
    }
}

