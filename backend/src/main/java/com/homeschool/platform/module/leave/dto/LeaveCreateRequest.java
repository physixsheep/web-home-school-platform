package com.homeschool.platform.module.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record LeaveCreateRequest(
        @NotBlank(message = "请假类型不能为空")
        @Pattern(
                regexp = "SICK|PERSONAL|OTHER",
                message = "请假类型不正确")
        String leaveType,

        @NotNull(message = "请假开始时间不能为空")
        LocalDateTime startAt,

        @NotNull(message = "请假结束时间不能为空")
        LocalDateTime endAt,

        @NotBlank(message = "请假原因不能为空")
        @Size(max = 5000, message = "请假原因不能超过 5000 个字符")
        String reason,

        List<Long> attachmentIds) {
}

