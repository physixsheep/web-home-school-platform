package com.homeschool.platform.module.assignment.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record AssignmentSubmissionRequest(
        @Size(max = 50000, message = "提交内容不能超过 50000 个字符")
        String content,

        List<Long> attachmentIds) {
}

