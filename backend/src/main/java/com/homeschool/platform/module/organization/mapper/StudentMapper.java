package com.homeschool.platform.module.organization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homeschool.platform.module.organization.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StudentMapper extends BaseMapper<Student> {

    @Select("""
            SELECT *
            FROM student
            WHERE id = #{studentId} AND deleted = 0
            FOR UPDATE
            """)
    Student selectByIdForUpdate(@Param("studentId") Long studentId);
}

