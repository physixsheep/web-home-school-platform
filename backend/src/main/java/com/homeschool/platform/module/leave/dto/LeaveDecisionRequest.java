package com.homeschool.platform.module.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LeaveDecisionRequest(
        @NotBlank(message = "审批结果不能为空")
        @Pattern(
                regexp = "APPROVED|REJECTED",
                message = "审批结果不正确")
        String decision,

        @NotBlank(message = "审批意见不能为空")
        @Size(max = 500, message = "审批意见不能超过 500 个字符")
        String opinion) {
}

