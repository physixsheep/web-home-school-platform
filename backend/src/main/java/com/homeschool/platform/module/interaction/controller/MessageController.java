package com.homeschool.platform.module.interaction.controller;

import com.homeschool.platform.common.api.ApiResponse;
import com.homeschool.platform.common.api.PageResponse;
import com.homeschool.platform.module.interaction.dto.MessageCreateRequest;
import com.homeschool.platform.module.interaction.dto.MessageQuery;
import com.homeschool.platform.module.interaction.dto.MessageReplyRequest;
import com.homeschool.platform.module.interaction.service.MessageService;
import com.homeschool.platform.module.interaction.vo.MessageDetailVO;
import com.homeschool.platform.module.interaction.vo.MessageVO;
import com.homeschool.platform.module.organization.vo.EntityOptionVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "家校互动")
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @Operation(summary = "学生或家长查询自己发起的留言")
    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('STUDENT', 'PARENT')")
    public ApiResponse<PageResponse<MessageVO>> mine(
            MessageQuery query,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(messageService.minePage(query, principal));
    }

    @Operation(summary = "教师查询收件箱或管理员审计全部留言")
    @GetMapping("/received")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<PageResponse<MessageVO>> received(
            MessageQuery query,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(messageService.receivedPage(query, principal));
    }

    @Operation(summary = "查询可留言的本班教师")
    @GetMapping("/options/teachers")
    @PreAuthorize("hasAnyRole('STUDENT', 'PARENT')")
    public ApiResponse<List<EntityOptionVO>> teacherOptions(
            @RequestParam(required = false) Long studentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(messageService.teacherOptions(studentId, principal));
    }

    @Operation(summary = "查询留言会话详情")
    @GetMapping("/{messageId}")
    public ApiResponse<MessageDetailVO> detail(
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(messageService.detail(messageId, principal));
    }

    @Operation(summary = "学生或家长发起留言")
    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'PARENT')")
    public ApiResponse<Map<String, Long>> create(
            @Valid @RequestBody MessageCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(Map.of(
                "id",
                messageService.create(request, principal)));
    }

    @Operation(summary = "接收教师标记留言已读")
    @PostMapping("/{messageId}/read")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> markRead(
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserPrincipal principal) {
        messageService.markRead(messageId, principal);
        return ApiResponse.success();
    }

    @Operation(summary = "会话双方回复留言")
    @PostMapping("/{messageId}/replies")
    public ApiResponse<Void> reply(
            @PathVariable Long messageId,
            @Valid @RequestBody MessageReplyRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        messageService.reply(messageId, request, principal);
        return ApiResponse.success();
    }

    @Operation(summary = "接收教师关闭留言会话")
    @PostMapping("/{messageId}/close")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> close(
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserPrincipal principal) {
        messageService.close(messageId, principal);
        return ApiResponse.success();
    }
}

