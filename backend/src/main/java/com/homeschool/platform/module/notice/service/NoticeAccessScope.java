package com.homeschool.platform.module.notice.service;

import java.util.Set;

public record NoticeAccessScope(
        boolean admin,
        Set<Long> classIds,
        Set<Long> gradeIds) {

    public boolean getAdmin() {
        return admin;
    }

    public Set<Long> getClassIds() {
        return classIds;
    }

    public Set<Long> getGradeIds() {
        return gradeIds;
    }
}

