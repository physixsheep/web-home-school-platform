package com.homeschool.platform.module.leave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homeschool.platform.module.leave.entity.LeaveRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface LeaveRequestMapper extends BaseMapper<LeaveRequest> {

    @Update("""
            UPDATE leave_request
            SET status = #{status},
                approver_teacher_id = #{teacherId},
                approved_at = CURRENT_TIMESTAMP(3),
                approval_opinion = #{opinion},
                version = version + 1
            WHERE id = #{leaveId}
              AND status = 'PENDING'
              AND version = #{version}
              AND deleted = 0
            """)
    int decide(
            @Param("leaveId") Long leaveId,
            @Param("status") String status,
            @Param("teacherId") Long teacherId,
            @Param("opinion") String opinion,
            @Param("version") int version);

    @Update("""
            UPDATE leave_request
            SET status = 'CANCELLED',
                cancelled_at = CURRENT_TIMESTAMP(3),
                version = version + 1
            WHERE id = #{leaveId}
              AND student_id = #{studentId}
              AND status = 'PENDING'
              AND version = #{version}
              AND deleted = 0
            """)
    int cancel(
            @Param("leaveId") Long leaveId,
            @Param("studentId") Long studentId,
            @Param("version") int version);
}

