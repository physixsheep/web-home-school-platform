package com.homeschool.platform.module.assignment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homeschool.platform.module.assignment.entity.AssignmentSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AssignmentSubmissionMapper extends BaseMapper<AssignmentSubmission> {

    @Select("""
            SELECT *
            FROM assignment_submission
            WHERE assignment_id = #{assignmentId}
              AND student_id = #{studentId}
            FOR UPDATE
            """)
    AssignmentSubmission selectForUpdate(
            @Param("assignmentId") Long assignmentId,
            @Param("studentId") Long studentId);
}

