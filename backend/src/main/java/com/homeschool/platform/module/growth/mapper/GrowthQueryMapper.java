package com.homeschool.platform.module.growth.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homeschool.platform.module.growth.dto.GrowthQuery;
import com.homeschool.platform.module.growth.vo.GrowthVO;
import com.homeschool.platform.module.organization.vo.EntityOptionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GrowthQueryMapper {

    @Select("""
            <script>
            SELECT gr.id,
                   gr.student_id,
                   su.real_name AS student_name,
                   st.student_no,
                   gr.class_id,
                   c.class_name,
                   g.grade_name,
                   gr.category,
                   gr.title,
                   gr.content,
                   gr.occurred_at,
                   gr.recorder_teacher_id,
                   tu.real_name AS recorder_name,
                   gr.visible_to_parent,
                   gr.created_at,
                   gr.updated_at
            FROM growth_record gr
            JOIN student st ON st.id = gr.student_id AND st.deleted = 0
            JOIN sys_user su ON su.id = st.user_id AND su.deleted = 0
            JOIN school_class c ON c.id = gr.class_id AND c.deleted = 0
            JOIN grade g ON g.id = c.grade_id AND g.deleted = 0
            JOIN teacher t ON t.id = gr.recorder_teacher_id AND t.deleted = 0
            JOIN sys_user tu ON tu.id = t.user_id AND tu.deleted = 0
            WHERE gr.deleted = 0
            <if test="!admin">AND gr.student_id = #{studentId}</if>
            <if test="parentOnly">AND gr.visible_to_parent = 1</if>
            <if test="query.category != null and query.category != ''">
                AND gr.category = #{query.category}
            </if>
            <if test="query.dateFrom != null">AND gr.occurred_at &gt;= #{query.dateFrom}</if>
            <if test="query.dateTo != null">AND gr.occurred_at &lt;= #{query.dateTo}</if>
            <if test="query.keyword != null and query.keyword != ''">
                AND (gr.title LIKE CONCAT('%', #{query.keyword}, '%')
                     OR gr.content LIKE CONCAT('%', #{query.keyword}, '%'))
            </if>
            ORDER BY gr.occurred_at DESC, gr.id DESC
            </script>
            """)
    IPage<GrowthVO> selectReadablePage(
            Page<GrowthVO> page,
            @Param("query") GrowthQuery query,
            @Param("studentId") Long studentId,
            @Param("admin") boolean admin,
            @Param("parentOnly") boolean parentOnly);

    @Select("""
            <script>
            SELECT gr.id,
                   gr.student_id,
                   su.real_name AS student_name,
                   st.student_no,
                   gr.class_id,
                   c.class_name,
                   g.grade_name,
                   gr.category,
                   gr.title,
                   gr.content,
                   gr.occurred_at,
                   gr.recorder_teacher_id,
                   tu.real_name AS recorder_name,
                   gr.visible_to_parent,
                   gr.created_at,
                   gr.updated_at
            FROM growth_record gr
            JOIN student st ON st.id = gr.student_id AND st.deleted = 0
            JOIN sys_user su ON su.id = st.user_id AND su.deleted = 0
            JOIN school_class c ON c.id = gr.class_id AND c.deleted = 0
            JOIN grade g ON g.id = c.grade_id AND g.deleted = 0
            JOIN teacher t ON t.id = gr.recorder_teacher_id AND t.deleted = 0
            JOIN sys_user tu ON tu.id = t.user_id AND tu.deleted = 0
            WHERE gr.deleted = 0
              AND gr.class_id IN
              <foreach collection="classIds" item="classId"
                       open="(" separator="," close=")">#{classId}</foreach>
            <if test="query.studentId != null">AND gr.student_id = #{query.studentId}</if>
            <if test="query.category != null and query.category != ''">
                AND gr.category = #{query.category}
            </if>
            <if test="query.dateFrom != null">AND gr.occurred_at &gt;= #{query.dateFrom}</if>
            <if test="query.dateTo != null">AND gr.occurred_at &lt;= #{query.dateTo}</if>
            <if test="query.keyword != null and query.keyword != ''">
                AND (gr.title LIKE CONCAT('%', #{query.keyword}, '%')
                     OR gr.content LIKE CONCAT('%', #{query.keyword}, '%')
                     OR su.real_name LIKE CONCAT('%', #{query.keyword}, '%')
                     OR st.student_no LIKE CONCAT('%', #{query.keyword}, '%'))
            </if>
            ORDER BY gr.occurred_at DESC, gr.id DESC
            </script>
            """)
    IPage<GrowthVO> selectManagePage(
            Page<GrowthVO> page,
            @Param("query") GrowthQuery query,
            @Param("classIds") List<Long> classIds);

    @Select("""
            SELECT gr.id,
                   gr.student_id,
                   su.real_name AS student_name,
                   st.student_no,
                   gr.class_id,
                   c.class_name,
                   g.grade_name,
                   gr.category,
                   gr.title,
                   gr.content,
                   gr.occurred_at,
                   gr.recorder_teacher_id,
                   tu.real_name AS recorder_name,
                   gr.visible_to_parent,
                   gr.created_at,
                   gr.updated_at
            FROM growth_record gr
            JOIN student st ON st.id = gr.student_id AND st.deleted = 0
            JOIN sys_user su ON su.id = st.user_id AND su.deleted = 0
            JOIN school_class c ON c.id = gr.class_id AND c.deleted = 0
            JOIN grade g ON g.id = c.grade_id AND g.deleted = 0
            JOIN teacher t ON t.id = gr.recorder_teacher_id AND t.deleted = 0
            JOIN sys_user tu ON tu.id = t.user_id AND tu.deleted = 0
            WHERE gr.id = #{recordId} AND gr.deleted = 0
            """)
    GrowthVO selectDetail(@Param("recordId") Long recordId);

    @Select("""
            <script>
            SELECT st.id,
                   CONCAT(su.real_name, ' · ', st.student_no) AS label,
                   CONCAT(g.grade_name, c.class_name) AS secondary
            FROM student_class sc
            JOIN student st ON st.id = sc.student_id AND st.deleted = 0
            JOIN sys_user su ON su.id = st.user_id AND su.deleted = 0
            JOIN school_class c ON c.id = sc.class_id AND c.deleted = 0
            JOIN grade g ON g.id = c.grade_id AND g.deleted = 0
            WHERE sc.status = 'CURRENT' AND sc.deleted = 0
            <if test="!admin">
              AND sc.class_id IN
              <foreach collection="classIds" item="classId"
                       open="(" separator="," close=")">#{classId}</foreach>
            </if>
            ORDER BY su.real_name, st.student_no
            </script>
            """)
    List<EntityOptionVO> selectStudentOptions(
            @Param("classIds") List<Long> classIds,
            @Param("admin") boolean admin);
}

