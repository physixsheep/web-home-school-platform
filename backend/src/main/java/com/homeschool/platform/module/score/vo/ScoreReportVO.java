package com.homeschool.platform.module.score.vo;

import java.util.List;

public record ScoreReportVO(
        Long studentId,
        String studentName,
        String studentNo,
        List<ScoreItemVO> items,
        List<ScoreTrendVO> trends) {
}

