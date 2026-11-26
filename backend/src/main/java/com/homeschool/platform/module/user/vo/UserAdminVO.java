package com.homeschool.platform.module.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserAdminVO {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String status;
    private LocalDateTime lastLoginAt;
    private String roleCode;
    private String roleName;
    private String identifier;
    private String gender;
    private String extraInfo;
}

