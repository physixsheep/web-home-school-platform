package com.homeschool.platform.module.assignment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AssignmentGradeRequest(
        @DecimalMin(value = "0.00", message = "作业成绩不能小于 0")
        BigDecimal score,

        @Size(max = 5000, message = "评语不能超过 5000 个字符")
        String comment) {
}

