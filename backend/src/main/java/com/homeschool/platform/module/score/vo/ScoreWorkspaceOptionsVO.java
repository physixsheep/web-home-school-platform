package com.homeschool.platform.module.score.vo;

import com.homeschool.platform.module.assignment.vo.TeachingOptionVO;

import java.util.List;

public record ScoreWorkspaceOptionsVO(
        List<ExamOptionVO> exams,
        List<TeachingOptionVO> teachings) {
}

