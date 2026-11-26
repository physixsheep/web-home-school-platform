package com.homeschool.platform.module.leave.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homeschool.platform.module.leave.dto.LeaveQuery;
import com.homeschool.platform.module.leave.vo.LeaveVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LeaveQueryMapper {

    @Select("""
            <script>
            SELECT lr.id,
                   lr.student_id,
                   su.real_name AS student_name,
                   s.student_no,
                   lr.class_id,
                   c.class_name,
                   g.grade_name,
                   lr.leave_type,
                   lr.start_at,
                   lr.end_at,
                   lr.reason,
                   lr.status,
                   lr.approver_teacher_id,
                   tu.real_name AS approver_name,
                   lr.approved_at,
                   lr.approval_opinion,
                   lr.cancelled_at,
                   lr.version,
                   lr.created_at
            FROM leave_request lr
            JOIN student s ON s.id = lr.student_id AND s.deleted = 0
            JOIN sys_user su ON su.id = s.user_id AND su.deleted = 0
            JOIN school_class c ON c.id = lr.class_id AND c.deleted = 0
            JOIN grade g ON g.id = c.grade_id AND g.deleted = 0
            LEFT JOIN teacher t ON t.id = lr.approver_teacher_id AND t.deleted = 0
            LEFT JOIN sys_user tu ON tu.id = t.user_id AND tu.deleted = 0
            WHERE lr.deleted = 0
            <if test="!admin">AND lr.student_id = #{studentId}</if>
            <if test="query.status != null and query.status != ''">
                AND lr.status = #{query.status}
            </if>
            <if test="query.keyword != null and query.keyword != ''">
                AND (lr.reason LIKE CONCAT('%', #{query.keyword}, '%')
                     OR su.real_name LIKE CONCAT('%', #{query.keyword}, '%')
                     OR s.student_no LIKE CONCAT('%', #{query.keyword}, '%'))
            </if>
            ORDER BY lr.start_at DESC, lr.id DESC
            </script>
            """)
    IPage<LeaveVO> selectLeavePage(
            Page<LeaveVO> page,
            @Param("query") LeaveQuery query,
            @Param("studentId") Long studentId,
            @Param("admin") boolean admin);

    @Select("""
            <script>
            SELECT lr.id,
                   lr.student_id,
                   su.real_name AS student_name,
                   s.student_no,
                   lr.class_id,
                   c.class_name,
                   g.grade_name,
                   lr.leave_type,
                   lr.start_at,
                   lr.end_at,
                   lr.reason,
                   lr.status,
                   lr.approver_teacher_id,
                   tu.real_name AS approver_name,
                   lr.approved_at,
                   lr.approval_opinion,
                   lr.cancelled_at,
                   lr.version,
                   lr.created_at
            FROM leave_request lr
            JOIN student s ON s.id = lr.student_id AND s.deleted = 0
            JOIN sys_user su ON su.id = s.user_id AND su.deleted = 0
            JOIN school_class c ON c.id = lr.class_id AND c.deleted = 0
            JOIN grade g ON g.id = c.grade_id AND g.deleted = 0
            LEFT JOIN teacher t ON t.id = lr.approver_teacher_id AND t.deleted = 0
            LEFT JOIN sys_user tu ON tu.id = t.user_id AND tu.deleted = 0
            WHERE lr.deleted = 0
              AND lr.class_id IN
              <foreach collection="classIds" item="classId"
                       open="(" separator="," close=")">#{classId}</foreach>
            <if test="query.status != null and query.status != ''">
                AND lr.status = #{query.status}
            </if>
            <if test="query.keyword != null and query.keyword != ''">
                AND (lr.reason LIKE CONCAT('%', #{query.keyword}, '%')
                     OR su.real_name LIKE CONCAT('%', #{query.keyword}, '%')
                     OR s.student_no LIKE CONCAT('%', #{query.keyword}, '%'))
            </if>
            ORDER BY CASE WHEN lr.status = 'PENDING' THEN 0 ELSE 1 END,
                     lr.start_at DESC, lr.id DESC
            </script>
            """)
    IPage<LeaveVO> selectApprovalPage(
            Page<LeaveVO> page,
            @Param("query") LeaveQuery query,
            @Param("classIds") List<Long> classIds);

    @Select("""
            SELECT lr.id,
                   lr.student_id,
                   su.real_name AS student_name,
                   s.student_no,
                   lr.class_id,
                   c.class_name,
                   g.grade_name,
                   lr.leave_type,
                   lr.start_at,
                   lr.end_at,
                   lr.reason,
                   lr.status,
                   lr.approver_teacher_id,
                   tu.real_name AS approver_name,
                   lr.approved_at,
                   lr.approval_opinion,
                   lr.cancelled_at,
                   lr.version,
                   lr.created_at
            FROM leave_request lr
            JOIN student s ON s.id = lr.student_id AND s.deleted = 0
            JOIN sys_user su ON su.id = s.user_id AND su.deleted = 0
            JOIN school_class c ON c.id = lr.class_id AND c.deleted = 0
            JOIN grade g ON g.id = c.grade_id AND g.deleted = 0
            LEFT JOIN teacher t ON t.id = lr.approver_teacher_id AND t.deleted = 0
            LEFT JOIN sys_user tu ON tu.id = t.user_id AND tu.deleted = 0
            WHERE lr.id = #{leaveId} AND lr.deleted = 0
            """)
    LeaveVO selectDetail(@Param("leaveId") Long leaveId);
}

