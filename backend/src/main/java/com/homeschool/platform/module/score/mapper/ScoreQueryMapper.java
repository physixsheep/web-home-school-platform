package com.homeschool.platform.module.score.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homeschool.platform.module.score.dto.ExamQuery;
import com.homeschool.platform.module.score.vo.ExamOptionVO;
import com.homeschool.platform.module.score.vo.ExamVO;
import com.homeschool.platform.module.score.vo.ScoreEntryRowVO;
import com.homeschool.platform.module.score.vo.ScoreItemVO;
import com.homeschool.platform.module.score.vo.ScoreStatsVO;
import com.homeschool.platform.module.score.vo.ScoreTrendVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScoreQueryMapper {

    @Select("""
            <script>
            SELECT e.id,
                   e.name,
                   e.exam_type,
                   e.grade_id,
                   g.grade_name,
                   e.class_id,
                   c.class_name,
                   e.academic_year,
                   e.term,
                   e.exam_date,
                   e.status,
                   e.created_at
            FROM examination e
            JOIN grade g ON g.id = e.grade_id AND g.deleted = 0
            LEFT JOIN school_class c ON c.id = e.class_id AND c.deleted = 0
            WHERE e.deleted = 0
            <if test="query.status != null and query.status != ''">
                AND e.status = #{query.status}
            </if>
            <if test="query.academicYear != null and query.academicYear != ''">
                AND e.academic_year = #{query.academicYear}
            </if>
            <if test="query.keyword != null and query.keyword != ''">
                AND (e.name LIKE CONCAT('%', #{query.keyword}, '%')
                     OR e.exam_type LIKE CONCAT('%', #{query.keyword}, '%'))
            </if>
            ORDER BY e.exam_date DESC, e.id DESC
            </script>
            """)
    IPage<ExamVO> selectExamPage(
            Page<ExamVO> page,
            @Param("query") ExamQuery query);

    @Select("""
            SELECT e.id,
                   e.name,
                   e.exam_date,
                   e.grade_id,
                   e.class_id,
                   g.grade_name,
                   c.class_name
            FROM examination e
            JOIN grade g ON g.id = e.grade_id AND g.deleted = 0
            LEFT JOIN school_class c ON c.id = e.class_id AND c.deleted = 0
            WHERE e.deleted = 0 AND e.status = 'PUBLISHED'
            ORDER BY e.exam_date DESC, e.id DESC
            """)
    List<ExamOptionVO> selectPublishedExamOptions();

    @Select("""
            SELECT s.id AS student_id,
                   u.real_name AS student_name,
                   s.student_no,
                   sc.score,
                   COALESCE(sc.is_absent, 0) AS is_absent,
                   sc.remark
            FROM student_class stc
            JOIN student s ON s.id = stc.student_id AND s.deleted = 0
            JOIN sys_user u ON u.id = s.user_id AND u.deleted = 0
            LEFT JOIN score sc
                ON sc.exam_id = #{examId}
               AND sc.student_id = s.id
               AND sc.subject_id = #{subjectId}
               AND sc.deleted = 0
            WHERE stc.class_id = #{classId}
              AND stc.status = 'CURRENT'
              AND stc.deleted = 0
            ORDER BY s.student_no
            """)
    List<ScoreEntryRowVO> selectEntryRows(
            @Param("examId") Long examId,
            @Param("classId") Long classId,
            @Param("subjectId") Long subjectId);

    @Select("""
            SELECT e.id AS exam_id,
                   e.name AS exam_name,
                   e.exam_type,
                   e.exam_date,
                   e.academic_year,
                   e.term,
                   sc.class_id,
                   c.class_name,
                   sc.subject_id,
                   sub.subject_name,
                   sc.score,
                   sub.default_full_score AS full_score,
                   sc.is_absent,
                   sc.remark
            FROM score sc
            JOIN examination e ON e.id = sc.exam_id AND e.deleted = 0
            JOIN school_class c ON c.id = sc.class_id AND c.deleted = 0
            JOIN subject sub ON sub.id = sc.subject_id AND sub.deleted = 0
            WHERE sc.student_id = #{studentId}
              AND sc.deleted = 0
              AND e.status = 'PUBLISHED'
            ORDER BY e.exam_date DESC, sub.sort_order, sc.id
            """)
    List<ScoreItemVO> selectStudentScoreItems(@Param("studentId") Long studentId);

    @Select("""
            SELECT e.id AS exam_id,
                   e.name AS exam_name,
                   e.exam_date,
                   e.academic_year,
                   e.term,
                   SUM(sc.score) AS total_score,
                   AVG(sc.score) AS average_score,
                   COUNT(sc.score) AS subject_count
            FROM score sc
            JOIN examination e ON e.id = sc.exam_id AND e.deleted = 0
            WHERE sc.student_id = #{studentId}
              AND sc.deleted = 0
              AND e.status = 'PUBLISHED'
            GROUP BY e.id, e.name, e.exam_date, e.academic_year, e.term
            ORDER BY e.exam_date, e.id
            """)
    List<ScoreTrendVO> selectStudentTrends(@Param("studentId") Long studentId);

    @Select("""
            SELECT COUNT(*) AS entered_count,
                   COALESCE(SUM(CASE WHEN is_absent = 1 THEN 1 ELSE 0 END), 0)
                       AS absent_count,
                   AVG(CASE WHEN is_absent = 0 THEN score END) AS average_score,
                   MAX(CASE WHEN is_absent = 0 THEN score END) AS highest_score,
                   MIN(CASE WHEN is_absent = 0 THEN score END) AS lowest_score
            FROM score
            WHERE exam_id = #{examId}
              AND class_id = #{classId}
              AND subject_id = #{subjectId}
              AND deleted = 0
            """)
    ScoreStatsVO selectClassStatistics(
            @Param("examId") Long examId,
            @Param("classId") Long classId,
            @Param("subjectId") Long subjectId);
}

