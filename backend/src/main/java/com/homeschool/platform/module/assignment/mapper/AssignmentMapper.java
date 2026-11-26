package com.homeschool.platform.module.assignment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homeschool.platform.module.assignment.entity.Assignment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AssignmentMapper extends BaseMapper<Assignment> {
}

