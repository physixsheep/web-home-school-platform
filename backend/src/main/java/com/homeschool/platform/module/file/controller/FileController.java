package com.homeschool.platform.module.file.controller;

import com.homeschool.platform.common.api.ApiResponse;
import com.homeschool.platform.module.file.FileAttachmentService;
import com.homeschool.platform.module.file.vo.FileAttachmentVO;
import com.homeschool.platform.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@Tag(name = "附件")
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileAttachmentService attachmentService;

    public FileController(FileAttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @Operation(summary = "上传临时附件")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ApiResponse<FileAttachmentVO> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(attachmentService.upload(file, principal));
    }

    @Operation(summary = "删除本人未绑定临时附件")
    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ApiResponse<Void> deleteTemp(
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        attachmentService.deleteTemp(attachmentId, principal);
        return ApiResponse.success();
    }

    @Operation(summary = "鉴权下载附件")
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> download(
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        var file = attachmentService.download(attachmentId, principal);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(file.mimeType());
        } catch (IllegalArgumentException exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(file.fileSize())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(file.originalName(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(file.resource());
    }
}

