package com.homeschool.platform.module.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homeschool.platform.module.notice.entity.NoticeReadRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NoticeReadMapper extends BaseMapper<NoticeReadRecord> {

    @Insert("""
            INSERT IGNORE INTO notice_read (notice_id, user_id, read_at)
            VALUES (#{noticeId}, #{userId}, CURRENT_TIMESTAMP(3))
            """)
    int insertIgnore(@Param("noticeId") Long noticeId, @Param("userId") Long userId);
}

