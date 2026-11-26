package com.homeschool.platform.common.constant;

import java.util.Set;

public final class RoleCodes {

    public static final String ADMIN = "ADMIN";
    public static final String TEACHER = "TEACHER";
    public static final String STUDENT = "STUDENT";
    public static final String PARENT = "PARENT";
    public static final Set<String> BUSINESS_ROLES = Set.of(TEACHER, STUDENT, PARENT);

    private RoleCodes() {
    }
}

