package com.homeschool.platform.module.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homeschool.platform.module.interaction.entity.InteractionMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InteractionMessageMapper extends BaseMapper<InteractionMessage> {
}

