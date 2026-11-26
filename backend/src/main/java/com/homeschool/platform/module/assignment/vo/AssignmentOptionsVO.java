package com.homeschool.platform.module.assignment.vo;

import com.homeschool.platform.module.organization.vo.EntityOptionVO;

import java.util.List;

public record AssignmentOptionsVO(
        List<TeachingOptionVO> teachings,
        List<EntityOptionVO> children) {
}

