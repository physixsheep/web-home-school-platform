package com.homeschool.platform.module.score.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ScoreBatchRequest(
        @NotNull(message = "考试不能为空")
        Long examId,

        @NotNull(message = "班级不能为空")
        Long classId,

        @NotNull(message = "学科不能为空")
        Long subjectId,

        @NotEmpty(message = "至少录入一条成绩")
        List<@Valid ScoreRecordRequest> records) {
}

