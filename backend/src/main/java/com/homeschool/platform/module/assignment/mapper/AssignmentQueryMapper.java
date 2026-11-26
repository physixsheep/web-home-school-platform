package com.homeschool.platform.module.assignment.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homeschool.platform.module.assignment.dto.AssignmentQuery;
import com.homeschool.platform.module.assignment.dto.SubmissionQuery;
import com.homeschool.platform.module.assignment.vo.AssignmentVO;
import com.homeschool.platform.module.assignment.vo.SubmissionRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AssignmentQueryMapper {

    @Select("""
            <script>
            SELECT a.id,
                   a.class_id,
                   c.class_name,
                   g.grade_name,
                   a.teacher_id,
                   u.real_name AS teacher_name,
                   a.subject_id,
                   sub.subject_name,
                   a.title,
                   a.content,
                   a.deadline,
                   a.max_score,
                   a.status,
                   a.published_at,
                   (SELECT COUNT(*) FROM student_class sc
                    WHERE sc.class_id = a.class_id AND sc.status = 'CURRENT' AND sc.deleted = 0)
                       AS student_count,
                   (SELECT COUNT(*) FROM assignment_submission s1
                    WHERE s1.assignment_id = a.id)
                       AS submitted_count,
                   (SELECT COUNT(*) FROM assignment_submission s2
                    WHERE s2.assignment_id = a.id AND s2.status = 'GRADED')
                       AS graded_count,
                   (
                     (SELECT COUNT(*) FROM student_class sc2
                      WHERE sc2.class_id = a.class_id AND sc2.status = 'CURRENT' AND sc2.deleted = 0)
                     -
                     (SELECT COUNT(*) FROM assignment_submission s3
                      WHERE s3.assignment_id = a.id)
                   ) AS unsubmitted_count
            FROM assignment a
            JOIN school_class c ON c.id = a.class_id AND c.deleted = 0
            JOIN grade g ON g.id = c.grade_id AND g.deleted = 0
            JOIN teacher t ON t.id = a.teacher_id AND t.deleted = 0
            JOIN sys_user u ON u.id = t.user_id AND u.deleted = 0
            JOIN subject sub ON sub.id = a.subject_id AND sub.deleted = 0
            WHERE a.deleted = 0
            <if test="!admin">AND a.teacher_id = #{teacherId}</if>
            <if test="query.status != null and query.status != ''">
                AND a.status = #{query.status}
            </if>
            <if test="query.keyword != null and query.keyword != ''">
                AND (a.title LIKE CONCAT('%', #{query.keyword}, '%')
                     OR a.content LIKE CONCAT('%', #{query.keyword}, '%')
                     OR c.class_name LIKE CONCAT('%', #{query.keyword}, '%'))
            </if>
            ORDER BY a.deadline DESC, a.id DESC
            </script>
            """)
    IPage<AssignmentVO> selectManagePage(
            Page<AssignmentVO> page,
            @Param("query") AssignmentQuery query,
            @Param("teacherId") Long teacherId,
            @Param("admin") boolean admin);

    @Select("""
            <script>
            SELECT a.id,
                   a.class_id,
                   c.class_name,
                   g.grade_name,
                   a.teacher_id,
                   u.real_name AS teacher_name,
                   a.subject_id,
                   sub.subject_name,
                   a.title,
                   a.content,
                   a.deadline,
                   a.max_score,
                   a.status,
                   a.published_at,
                   s.id AS submission_id,
                   #{studentId} AS target_student_id,
                   CASE WHEN s.id IS NULL THEN 'UNSUBMITTED' ELSE s.status END
                       AS submission_status,
                   s.content AS submission_content,
                   s.attempt_count,
                   s.submitted_at,
                   CASE WHEN s.status = 'GRADED' THEN cm.score END AS score,
                   CASE WHEN s.status = 'GRADED' THEN cm.comment END AS teacher_comment,
                   CASE WHEN s.status = 'GRADED' THEN cm.graded_at END AS graded_at,
                   s.return_reason
            FROM assignment a
            JOIN school_class c ON c.id = a.class_id AND c.deleted = 0
            JOIN grade g ON g.id = c.grade_id AND g.deleted = 0
            JOIN teacher t ON t.id = a.teacher_id AND t.deleted = 0
            JOIN sys_user u ON u.id = t.user_id AND u.deleted = 0
            JOIN subject sub ON sub.id = a.subject_id AND sub.deleted = 0
            LEFT JOIN assignment_submission s
                ON s.assignment_id = a.id AND s.student_id = #{studentId}
            LEFT JOIN assignment_comment cm
                ON cm.submission_id = s.id AND cm.deleted = 0
            WHERE a.deleted = 0
              AND a.status IN ('PUBLISHED', 'CLOSED')
              AND (
                #{admin} = TRUE
                <if test="classIds != null and classIds.size() > 0">
                    OR a.class_id IN
                    <foreach collection="classIds" item="classId"
                             open="(" separator="," close=")">#{classId}</foreach>
                </if>
                OR 1 = 0
              )
            <if test="query.keyword != null and query.keyword != ''">
                AND (a.title LIKE CONCAT('%', #{query.keyword}, '%')
                     OR a.content LIKE CONCAT('%', #{query.keyword}, '%'))
            </if>
            ORDER BY a.deadline DESC, a.id DESC
            </script>
            """)
    IPage<AssignmentVO> selectReadablePage(
            Page<AssignmentVO> page,
            @Param("query") AssignmentQuery query,
            @Param("classIds") List<Long> classIds,
            @Param("admin") boolean admin,
            @Param("studentId") Long studentId);

    @Select("""
            SELECT a.id,
                   a.class_id,
                   c.class_name,
                   g.grade_name,
                   a.teacher_id,
                   u.real_name AS teacher_name,
                   a.subject_id,
                   sub.subject_name,
                   a.title,
                   a.content,
                   a.deadline,
                   a.max_score,
                   a.status,
                   a.published_at,
                   s.id AS submission_id,
                   #{studentId} AS target_student_id,
                   CASE WHEN s.id IS NULL THEN 'UNSUBMITTED' ELSE s.status END
                       AS submission_status,
                   s.content AS submission_content,
                   s.attempt_count,
                   s.submitted_at,
                   CASE WHEN s.status = 'GRADED' THEN cm.score END AS score,
                   CASE WHEN s.status = 'GRADED' THEN cm.comment END AS teacher_comment,
                   CASE WHEN s.status = 'GRADED' THEN cm.graded_at END AS graded_at,
                   s.return_reason
            FROM assignment a
            JOIN school_class c ON c.id = a.class_id AND c.deleted = 0
            JOIN grade g ON g.id = c.grade_id AND g.deleted = 0
            JOIN teacher t ON t.id = a.teacher_id AND t.deleted = 0
            JOIN sys_user u ON u.id = t.user_id AND u.deleted = 0
            JOIN subject sub ON sub.id = a.subject_id AND sub.deleted = 0
            LEFT JOIN assignment_submission s
                ON s.assignment_id = a.id
               AND s.student_id = #{studentId}
            LEFT JOIN assignment_comment cm
                ON cm.submission_id = s.id AND cm.deleted = 0
            WHERE a.id = #{assignmentId} AND a.deleted = 0
            """)
    AssignmentVO selectDetail(
            @Param("assignmentId") Long assignmentId,
            @Param("studentId") Long studentId);

    @Select("""
            <script>
            SELECT s.id AS student_id,
                   u.real_name AS student_name,
                   s.student_no,
                   sub.id AS submission_id,
                   CASE WHEN sub.id IS NULL THEN 'UNSUBMITTED' ELSE sub.status END AS status,
                   sub.attempt_count,
                   sub.content,
                   sub.submitted_at,
                   CASE WHEN sub.status = 'GRADED' THEN cm.score END AS score,
                   CASE WHEN sub.status = 'GRADED' THEN cm.comment END AS comment,
                   CASE WHEN sub.status = 'GRADED' THEN cm.graded_at END AS graded_at,
                   sub.return_reason
            FROM student_class sc
            JOIN student s ON s.id = sc.student_id AND s.deleted = 0
            JOIN sys_user u ON u.id = s.user_id AND u.deleted = 0
            LEFT JOIN assignment_submission sub
                ON sub.assignment_id = #{assignmentId} AND sub.student_id = s.id
            LEFT JOIN assignment_comment cm
                ON cm.submission_id = sub.id AND cm.deleted = 0
            WHERE sc.class_id = #{classId}
              AND sc.status = 'CURRENT'
              AND sc.deleted = 0
            <if test="query.keyword != null and query.keyword != ''">
                AND (u.real_name LIKE CONCAT('%', #{query.keyword}, '%')
                     OR s.student_no LIKE CONCAT('%', #{query.keyword}, '%'))
            </if>
            <if test="query.status != null and query.status != ''">
                AND (CASE WHEN sub.id IS NULL THEN 'UNSUBMITTED' ELSE sub.status END)
                    = #{query.status}
            </if>
            ORDER BY s.student_no
            </script>
            """)
    IPage<SubmissionRowVO> selectSubmissionPage(
            Page<SubmissionRowVO> page,
            @Param("assignmentId") Long assignmentId,
            @Param("classId") Long classId,
            @Param("query") SubmissionQuery query);
}

