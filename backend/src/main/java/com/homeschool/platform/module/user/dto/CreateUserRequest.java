package com.homeschool.platform.module.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateUserRequest(
        @NotBlank(message = "登录账号不能为空")
        @Size(max = 50, message = "登录账号不能超过 50 个字符")
        String username,

        @NotBlank(message = "初始密码不能为空")
        @Size(min = 8, max = 72, message = "密码长度必须为 8 到 72 个字符")
        String password,

        @NotBlank(message = "姓名不能为空")
        @Size(max = 50, message = "姓名不能超过 50 个字符")
        String realName,

        @Size(max = 20, message = "手机号不能超过 20 个字符")
        String phone,

        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱不能超过 100 个字符")
        String email,

        @NotBlank(message = "角色不能为空")
        @Pattern(
                regexp = "ADMIN|TEACHER|STUDENT|PARENT",
                message = "角色编码不正确")
        String roleCode,

        @Size(max = 30, message = "工号或学号不能超过 30 个字符")
        String identifier,

        @Size(max = 10, message = "性别字段过长")
        String gender,

        @Size(max = 50, message = "职称不能超过 50 个字符")
        String title,

        @Size(max = 100, message = "专业方向不能超过 100 个字符")
        String specialty,

        LocalDate hireDate,
        LocalDate birthDate,
        LocalDate enrollmentDate,

        @Size(max = 100, message = "职业不能超过 100 个字符")
        String occupation,

        @Size(max = 20, message = "紧急联系电话不能超过 20 个字符")
        String emergencyContact) {
}

