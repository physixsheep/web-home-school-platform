package com.homeschool.platform.module.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BindingStatusRequest(
        @NotBlank(message = "绑定状态不能为空")
        @Pattern(
                regexp = "ACTIVE|REVOKED",
                message = "绑定状态必须为 ACTIVE 或 REVOKED")
        String status) {
}

