package com.homeschool.platform.module.organization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homeschool.platform.module.organization.entity.SchoolClass;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SchoolClassMapper extends BaseMapper<SchoolClass> {

    @Select("""
            SELECT *
            FROM school_class
            WHERE id = #{classId} AND deleted = 0
            FOR UPDATE
            """)
    SchoolClass selectByIdForUpdate(@Param("classId") Long classId);
}

