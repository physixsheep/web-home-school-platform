package com.homeschool.platform.module.dashboard.mapper;

import com.homeschool.platform.module.dashboard.vo.AdminDashboardVO;
import com.homeschool.platform.module.dashboard.vo.NameValueVO;
import com.homeschool.platform.module.dashboard.vo.TeacherClassStatVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DashboardMapper {

    @Select("""
            SELECT
              (SELECT COUNT(*) FROM student WHERE status = 'ACTIVE' AND deleted = 0)
                AS student_count,
              (SELECT COUNT(*) FROM teacher WHERE status = 'ACTIVE' AND deleted = 0)
                AS teacher_count,
              (SELECT COUNT(*) FROM parent WHERE status = 'ACTIVE' AND deleted = 0)
                AS parent_count,
              (SELECT COUNT(*) FROM school_class WHERE status = 'ENABLED' AND deleted = 0)
                AS class_count,
              (SELECT COUNT(*) FROM notice WHERE status = 'PUBLISHED' AND deleted = 0)
                AS published_notice_count,
              (SELECT COUNT(*) FROM leave_request WHERE status = 'PENDING' AND deleted = 0)
                AS pending_leave_count
            """)
    AdminDashboardVO selectAdminSummary();

    @Select("""
            SELECT g.grade_name AS name,
                   COUNT(DISTINCT sc.student_id) AS value
            FROM grade g
            LEFT JOIN school_class c
              ON c.grade_id = g.id AND c.deleted = 0 AND c.status = 'ENABLED'
            LEFT JOIN student_class sc
              ON sc.class_id = c.id AND sc.status = 'CURRENT' AND sc.deleted = 0
            WHERE g.deleted = 0 AND g.status = 'ENABLED'
            GROUP BY g.id, g.grade_name, g.sort_order
            ORDER BY g.sort_order
            """)
    List<NameValueVO> selectStudentsByGrade();

    @Select("""
            SELECT CASE scope_type
                     WHEN 'ALL' THEN '全校'
                     WHEN 'GRADE' THEN '年级'
                     WHEN 'CLASS' THEN '班级'
                     ELSE scope_type
                   END AS name,
                   COUNT(*) AS value
            FROM notice
            WHERE status = 'PUBLISHED' AND deleted = 0
            GROUP BY scope_type
            """)
    List<NameValueVO> selectNoticesByScope();

    @Select("""
            SELECT c.id AS class_id,
                   c.class_name,
                   g.grade_name,
                   COUNT(DISTINCT sc.student_id) AS student_count,
                   COUNT(DISTINCT a.id) AS assignment_count,
                   COUNT(DISTINCT sub.id) AS submitted_count
            FROM (
                SELECT DISTINCT class_id
                FROM teacher_class
                WHERE teacher_id = #{teacherId}
                  AND status = 'ACTIVE'
                  AND deleted = 0
            ) tc
            JOIN school_class c ON c.id = tc.class_id AND c.deleted = 0
            JOIN grade g ON g.id = c.grade_id AND g.deleted = 0
            LEFT JOIN student_class sc
              ON sc.class_id = c.id AND sc.status = 'CURRENT' AND sc.deleted = 0
            LEFT JOIN assignment a
              ON a.class_id = c.id
             AND a.teacher_id = #{teacherId}
             AND a.status IN ('PUBLISHED', 'CLOSED')
             AND a.deleted = 0
            LEFT JOIN assignment_submission sub
              ON sub.assignment_id = a.id
            GROUP BY c.id, c.class_name, g.grade_name, g.sort_order
            ORDER BY g.sort_order, c.class_name
            """)
    List<TeacherClassStatVO> selectTeacherClassStats(@Param("teacherId") Long teacherId);

    @Select("""
            SELECT COUNT(*)
            FROM assignment_submission sub
            JOIN assignment a ON a.id = sub.assignment_id AND a.deleted = 0
            WHERE a.teacher_id = #{teacherId}
              AND sub.status = 'SUBMITTED'
            """)
    long selectTeacherPendingGrading(@Param("teacherId") Long teacherId);

    @Select("""
            SELECT COUNT(*)
            FROM leave_request lr
            JOIN teacher_class tc
              ON tc.class_id = lr.class_id
             AND tc.teacher_id = #{teacherId}
             AND tc.relation_type = 'HOMEROOM'
             AND tc.status = 'ACTIVE'
             AND tc.deleted = 0
            WHERE lr.status = 'PENDING' AND lr.deleted = 0
            """)
    long selectTeacherPendingLeave(@Param("teacherId") Long teacherId);
}

