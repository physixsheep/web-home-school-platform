package com.homeschool.platform.module.score.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ScoreRecordRequest(
        @NotNull(message = "学生不能为空")
        Long studentId,

        BigDecimal score,

        Boolean absent,

        @Size(max = 255, message = "备注不能超过 255 个字符")
        String remark) {
}

