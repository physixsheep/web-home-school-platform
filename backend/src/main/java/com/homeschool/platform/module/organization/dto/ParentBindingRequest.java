package com.homeschool.platform.module.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ParentBindingRequest(
        @NotNull(message = "家长不能为空")
        Long parentId,

        @NotNull(message = "学生不能为空")
        Long studentId,

        @NotBlank(message = "家庭关系不能为空")
        @Pattern(
                regexp = "FATHER|MOTHER|GUARDIAN",
                message = "家庭关系必须为 FATHER、MOTHER 或 GUARDIAN")
        String relationship,

        Integer isPrimary) {
}

