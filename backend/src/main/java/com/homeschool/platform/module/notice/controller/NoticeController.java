package com.homeschool.platform.module.notice.controller;

import com.homeschool.platform.common.api.ApiResponse;
import com.homeschool.platform.common.api.PageResponse;
import com.homeschool.platform.module.notice.dto.NoticeQuery;
import com.homeschool.platform.module.notice.dto.NoticeSaveRequest;
import com.homeschool.platform.module.notice.service.NoticeService;
import com.homeschool.platform.module.notice.vo.NoticeOptionsVO;
import com.homeschool.platform.module.notice.vo.NoticeReaderVO;
import com.homeschool.platform.module.notice.vo.NoticeVO;
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

@Tag(name = "校园通知")
@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Operation(summary = "分页查询当前用户可阅读的已发布通知")
    @GetMapping
    public ApiResponse<PageResponse<NoticeVO>> readablePage(
            NoticeQuery query,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(noticeService.readablePage(query, principal));
    }

    @Operation(summary = "分页查询当前管理员或教师可管理的通知")
    @GetMapping("/manage")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<PageResponse<NoticeVO>> managePage(
            NoticeQuery query,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(noticeService.managePage(query, principal));
    }

    @Operation(summary = "查询通知发布范围和班级选项")
    @GetMapping("/options")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<NoticeOptionsVO> options(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(noticeService.options(principal));
    }

    @Operation(summary = "查询通知详情")
    @GetMapping("/{noticeId}")
    public ApiResponse<NoticeVO> detail(
            @PathVariable Long noticeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(noticeService.detail(noticeId, principal));
    }

    @Operation(summary = "创建通知草稿")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<Map<String, Long>> create(
            @Valid @RequestBody NoticeSaveRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(Map.of("id", noticeService.create(request, principal)));
    }

    @Operation(summary = "更新草稿或已发布通知")
    @PutMapping("/{noticeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<Void> update(
            @PathVariable Long noticeId,
            @Valid @RequestBody NoticeSaveRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        noticeService.update(noticeId, request, principal);
        return ApiResponse.success();
    }

    @Operation(summary = "发布通知")
    @PostMapping("/{noticeId}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<Void> publish(
            @PathVariable Long noticeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        noticeService.publish(noticeId, principal);
        return ApiResponse.success();
    }

    @Operation(summary = "撤回通知")
    @PostMapping("/{noticeId}/withdraw")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<Void> withdraw(
            @PathVariable Long noticeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        noticeService.withdraw(noticeId, principal);
        return ApiResponse.success();
    }

    @Operation(summary = "删除通知草稿")
    @DeleteMapping("/{noticeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<Void> deleteDraft(
            @PathVariable Long noticeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        noticeService.deleteDraft(noticeId, principal);
        return ApiResponse.success();
    }

    @Operation(summary = "标记通知已读，重复调用幂等")
    @PostMapping("/{noticeId}/read")
    public ApiResponse<Void> markRead(
            @PathVariable Long noticeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        noticeService.markRead(noticeId, principal);
        return ApiResponse.success();
    }

    @Operation(summary = "分页查询通知阅读记录")
    @GetMapping("/{noticeId}/readers")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<PageResponse<NoticeReaderVO>> readers(
            @PathVariable Long noticeId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(
                noticeService.readers(noticeId, page, size, principal));
    }
}

