package com.homeschool.platform.module.organization.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homeschool.platform.module.organization.dto.ParentBindingQuery;
import com.homeschool.platform.module.organization.dto.SchoolClassQuery;
import com.homeschool.platform.module.organization.dto.StudentClassQuery;
import com.homeschool.platform.module.organization.dto.TeacherAssignmentQuery;
import com.homeschool.platform.module.organization.vo.EntityOptionVO;
import com.homeschool.platform.module.organization.vo.ParentBindingVO;
import com.homeschool.platform.module.organization.vo.SchoolClassVO;
import com.homeschool.platform.module.organization.vo.StudentClassVO;
import com.homeschool.platform.module.organization.vo.TeacherAssignmentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrganizationQueryMapper {

    @Select("""
            <script>
            SELECT c.id,
                   c.grade_id,
                   g.grade_name,
                   c.class_name,
                   c.academic_year,
                   c.graduation_year,
                   c.status,
                   c.created_at,
                   (SELECT COUNT(*) FROM student_class sc
                    WHERE sc.class_id = c.id AND sc.status = 'CURRENT' AND sc.deleted = 0)
                       AS student_count,
                   (SELECT COUNT(*) FROM teacher_class tc
                    WHERE tc.class_id = c.id AND tc.status = 'ACTIVE' AND tc.deleted = 0)
                       AS teacher_count
            FROM school_class c
            JOIN grade g ON g.id = c.grade_id AND g.deleted = 0
            WHERE c.deleted = 0
            <if test="query.gradeId != null">AND c.grade_id = #{query.gradeId}</if>
            <if test="query.status != null and query.status != ''">AND c.status = #{query.status}</if>
            <if test="query.academicYear != null and query.academicYear != ''">
                AND c.academic_year = #{query.academicYear}
            </if>
            <if test="query.keyword != null and query.keyword != ''">
                AND (
                    c.class_name LIKE CONCAT('%', #{query.keyword}, '%')
                    OR g.grade_name LIKE CONCAT('%', #{query.keyword}, '%')
                    OR c.academic_year LIKE CONCAT('%', #{query.keyword}, '%')
                )
            </if>
            ORDER BY g.sort_order, c.class_name, c.id DESC
            </script>
            """)
    IPage<SchoolClassVO> selectClassPage(
            Page<SchoolClassVO> page,
            @Param("query") SchoolClassQuery query);

    @Select("""
            <script>
            SELECT ps.id,
                   ps.parent_id,
                   pu.real_name AS parent_name,
                   pu.username AS parent_account,
                   ps.student_id,
                   su.real_name AS student_name,
                   s.student_no,
                   ps.relationship,
                   ps.is_primary,
                   ps.status,
                   ps.verified_at,
                   ps.created_at
            FROM parent_student ps
            JOIN parent p ON p.id = ps.parent_id AND p.deleted = 0
            JOIN sys_user pu ON pu.id = p.user_id AND pu.deleted = 0
            JOIN student s ON s.id = ps.student_id AND s.deleted = 0
            JOIN sys_user su ON su.id = s.user_id AND su.deleted = 0
            WHERE 1 = 1
            <if test="query.status != null and query.status != ''">AND ps.status = #{query.status}</if>
            <if test="query.parentId != null">AND ps.parent_id = #{query.parentId}</if>
            <if test="query.studentId != null">AND ps.student_id = #{query.studentId}</if>
            <if test="query.keyword != null and query.keyword != ''">
                AND (
                    pu.real_name LIKE CONCAT('%', #{query.keyword}, '%')
                    OR pu.username LIKE CONCAT('%', #{query.keyword}, '%')
                    OR su.real_name LIKE CONCAT('%', #{query.keyword}, '%')
                    OR s.student_no LIKE CONCAT('%', #{query.keyword}, '%')
                )
            </if>
            ORDER BY ps.id DESC
            </script>
            """)
    IPage<ParentBindingVO> selectParentBindingPage(
            Page<ParentBindingVO> page,
            @Param("query") ParentBindingQuery query);

    @Select("""
            <script>
            SELECT tc.id,
                   tc.teacher_id,
                   u.real_name AS teacher_name,
                   t.employee_no,
                   tc.class_id,
                   c.class_name,
                   g.grade_name,
                   tc.subject_id,
                   sub.subject_name,
                   tc.relation_type,
                   tc.start_date,
                   tc.end_date,
                   tc.status,
                   tc.created_at
            FROM teacher_class tc
            JOIN teacher t ON t.id = tc.teacher_id AND t.deleted = 0
            JOIN sys_user u ON u.id = t.user_id AND u.deleted = 0
            JOIN school_class c ON c.id = tc.class_id AND c.deleted = 0
            JOIN grade g ON g.id = c.grade_id AND g.deleted = 0
            LEFT JOIN subject sub ON sub.id = tc.subject_id AND sub.deleted = 0
            WHERE tc.deleted = 0
            <if test="query.status != null and query.status != ''">AND tc.status = #{query.status}</if>
            <if test="query.teacherId != null">AND tc.teacher_id = #{query.teacherId}</if>
            <if test="query.classId != null">AND tc.class_id = #{query.classId}</if>
            <if test="query.relationType != null and query.relationType != ''">
                AND tc.relation_type = #{query.relationType}
            </if>
            <if test="query.keyword != null and query.keyword != ''">
                AND (
                    u.real_name LIKE CONCAT('%', #{query.keyword}, '%')
                    OR t.employee_no LIKE CONCAT('%', #{query.keyword}, '%')
                    OR c.class_name LIKE CONCAT('%', #{query.keyword}, '%')
                    OR g.grade_name LIKE CONCAT('%', #{query.keyword}, '%')
                )
            </if>
            ORDER BY tc.id DESC
            </script>
            """)
    IPage<TeacherAssignmentVO> selectTeacherAssignmentPage(
            Page<TeacherAssignmentVO> page,
            @Param("query") TeacherAssignmentQuery query);

    @Select("""
            <script>
            SELECT sc.id,
                   sc.student_id,
                   u.real_name AS student_name,
                   s.student_no,
                   sc.class_id,
                   c.class_name,
                   g.grade_name,
                   c.academic_year,
                   sc.start_date,
                   sc.end_date,
                   sc.status,
                   sc.created_at
            FROM student_class sc
            JOIN student s ON s.id = sc.student_id AND s.deleted = 0
            JOIN sys_user u ON u.id = s.user_id AND u.deleted = 0
            JOIN school_class c ON c.id = sc.class_id AND c.deleted = 0
            JOIN grade g ON g.id = c.grade_id AND g.deleted = 0
            WHERE sc.deleted = 0
            <if test="query.status != null and query.status != ''">AND sc.status = #{query.status}</if>
            <if test="query.studentId != null">AND sc.student_id = #{query.studentId}</if>
            <if test="query.classId != null">AND sc.class_id = #{query.classId}</if>
            <if test="query.keyword != null and query.keyword != ''">
                AND (
                    u.real_name LIKE CONCAT('%', #{query.keyword}, '%')
                    OR s.student_no LIKE CONCAT('%', #{query.keyword}, '%')
                    OR c.class_name LIKE CONCAT('%', #{query.keyword}, '%')
                    OR g.grade_name LIKE CONCAT('%', #{query.keyword}, '%')
                )
            </if>
            ORDER BY sc.id DESC
            </script>
            """)
    IPage<StudentClassVO> selectStudentClassPage(
            Page<StudentClassVO> page,
            @Param("query") StudentClassQuery query);

    @Select("""
            SELECT t.id,
                   u.real_name AS label,
                   CONCAT(t.employee_no, ' · ', COALESCE(t.specialty, '未设置'))
                       AS secondary
            FROM teacher t
            JOIN sys_user u ON u.id = t.user_id AND u.deleted = 0
            WHERE t.deleted = 0 AND t.status = 'ACTIVE' AND u.status = 'ENABLED'
            ORDER BY u.real_name
            """)
    List<EntityOptionVO> selectTeacherOptions();

    @Select("""
            SELECT s.id,
                   u.real_name AS label,
                   s.student_no AS secondary
            FROM student s
            JOIN sys_user u ON u.id = s.user_id AND u.deleted = 0
            WHERE s.deleted = 0 AND s.status = 'ACTIVE' AND u.status = 'ENABLED'
            ORDER BY s.student_no
            """)
    List<EntityOptionVO> selectStudentOptions();

    @Select("""
            SELECT p.id,
                   u.real_name AS label,
                   u.username AS secondary
            FROM parent p
            JOIN sys_user u ON u.id = p.user_id AND u.deleted = 0
            WHERE p.deleted = 0 AND p.status = 'ACTIVE' AND u.status = 'ENABLED'
            ORDER BY u.real_name
            """)
    List<EntityOptionVO> selectParentOptions();

    @Select("""
            SELECT c.id,
                   CONCAT(g.grade_name, c.class_name) AS label,
                   c.academic_year AS secondary
            FROM school_class c
            JOIN grade g ON g.id = c.grade_id AND g.deleted = 0
            WHERE c.deleted = 0 AND c.status = 'ENABLED' AND g.status = 'ENABLED'
            ORDER BY g.sort_order, c.class_name
            """)
    List<EntityOptionVO> selectClassOptions();

    @Select("""
            SELECT id, subject_name AS label, subject_code AS secondary
            FROM subject
            WHERE deleted = 0 AND status = 'ENABLED'
            ORDER BY sort_order, id
            """)
    List<EntityOptionVO> selectSubjectOptions();
}

