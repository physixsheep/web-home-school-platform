package com.homeschool.platform.module.notice.vo;

import com.homeschool.platform.module.organization.vo.EntityOptionVO;

import java.util.List;

public record NoticeOptionsVO(
        List<EntityOptionVO> grades,
        List<EntityOptionVO> classes) {
}

