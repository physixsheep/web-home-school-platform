package com.homeschool.platform.module.file.vo;

public record FileAttachmentVO(
        Long id,
        String originalName,
        String extension,
        String mimeType,
        Long fileSize) {
}

