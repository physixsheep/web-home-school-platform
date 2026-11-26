package com.homeschool.platform.module.interaction.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homeschool.platform.module.interaction.dto.MessageQuery;
import com.homeschool.platform.module.interaction.vo.MessageReplyVO;
import com.homeschool.platform.module.interaction.vo.MessageVO;
import com.homeschool.platform.module.organization.vo.EntityOptionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageQueryMapper {

    @Select("""
            <script>
            SELECT m.id,
                   m.sender_user_id,
                   su.real_name AS sender_name,
                   m.sender_role,
                   m.student_id,
                   stu.real_name AS student_name,
                   s.student_no,
                   c.class_name,
                   g.grade_name,
                   m.receiver_teacher_id,
                   tu.real_name AS receiver_name,
                   m.subject,
                   m.content,
                   m.status,
                   m.last_reply_at,
                   m.closed_at,
                   m.created_at,
                   m.updated_at,
                   (SELECT COUNT(*) FROM message_reply mr
                    WHERE mr.message_id = m.id) AS reply_count,
                   (SELECT mr2.content FROM message_reply mr2
                    WHERE mr2.message_id = m.id
                    ORDER BY mr2.created_at DESC, mr2.id DESC LIMIT 1)
                       AS last_reply_preview
            FROM interaction_message m
            JOIN sys_user su ON su.id = m.sender_user_id AND su.deleted = 0
            JOIN student s ON s.id = m.student_id AND s.deleted = 0
            JOIN sys_user stu ON stu.id = s.user_id AND stu.deleted = 0
            JOIN school_class c ON c.id = m.class_id AND c.deleted = 0
            JOIN grade g ON g.id = c.grade_id AND g.deleted = 0
            JOIN teacher t ON t.id = m.receiver_teacher_id AND t.deleted = 0
            JOIN sys_user tu ON tu.id = t.user_id AND tu.deleted = 0
            WHERE m.deleted = 0 AND m.sender_user_id = #{userId}
            <if test="query.status != null and query.status != ''">
                AND m.status = #{query.status}
            </if>
            <if test="query.keyword != null and query.keyword != ''">
                AND (m.subject LIKE CONCAT('%', #{query.keyword}, '%')
                     OR m.content LIKE CONCAT('%', #{query.keyword}, '%'))
            </if>
            ORDER BY COALESCE(m.last_reply_at, m.created_at) DESC, m.id DESC
            </script>
            """)
    IPage<MessageVO> selectMinePage(
            Page<MessageVO> page,
            @Param("query") MessageQuery query,
            @Param("userId") Long userId);

    @Select("""
            <script>
            SELECT m.id,
                   m.sender_user_id,
                   su.real_name AS sender_name,
                   m.sender_role,
                   m.student_id,
                   stu.real_name AS student_name,
                   s.student_no,
                   c.class_name,
                   g.grade_name,
                   m.receiver_teacher_id,
                   tu.real_name AS receiver_name,
                   m.subject,
                   m.content,
                   m.status,
                   m.last_reply_at,
                   m.closed_at,
                   m.created_at,
                   m.updated_at,
                   (SELECT COUNT(*) FROM message_reply mr
                    WHERE mr.message_id = m.id) AS reply_count,
                   (SELECT mr2.content FROM message_reply mr2
                    WHERE mr2.message_id = m.id
                    ORDER BY mr2.created_at DESC, mr2.id DESC LIMIT 1)
                       AS last_reply_preview
            FROM interaction_message m
            JOIN sys_user su ON su.id = m.sender_user_id AND su.deleted = 0
            JOIN student s ON s.id = m.student_id AND s.deleted = 0
            JOIN sys_user stu ON stu.id = s.user_id AND stu.deleted = 0
            JOIN school_class c ON c.id = m.class_id AND c.deleted = 0
            JOIN grade g ON g.id = c.grade_id AND g.deleted = 0
            JOIN teacher t ON t.id = m.receiver_teacher_id AND t.deleted = 0
            JOIN sys_user tu ON tu.id = t.user_id AND tu.deleted = 0
            WHERE m.deleted = 0
            <if test="!admin">AND m.receiver_teacher_id = #{teacherId}</if>
            <if test="query.status != null and query.status != ''">
                AND m.status = #{query.status}
            </if>
            <if test="query.keyword != null and query.keyword != ''">
                AND (m.subject LIKE CONCAT('%', #{query.keyword}, '%')
                     OR m.content LIKE CONCAT('%', #{query.keyword}, '%')
                     OR su.real_name LIKE CONCAT('%', #{query.keyword}, '%')
                     OR stu.real_name LIKE CONCAT('%', #{query.keyword}, '%'))
            </if>
            ORDER BY CASE WHEN m.status = 'UNREAD' THEN 0 ELSE 1 END,
                     COALESCE(m.last_reply_at, m.created_at) DESC, m.id DESC
            </script>
            """)
    IPage<MessageVO> selectReceivedPage(
            Page<MessageVO> page,
            @Param("query") MessageQuery query,
            @Param("teacherId") Long teacherId,
            @Param("admin") boolean admin);

    @Select("""
            SELECT m.id,
                   m.sender_user_id,
                   su.real_name AS sender_name,
                   m.sender_role,
                   m.student_id,
                   stu.real_name AS student_name,
                   s.student_no,
                   c.class_name,
                   g.grade_name,
                   m.receiver_teacher_id,
                   tu.real_name AS receiver_name,
                   m.subject,
                   m.content,
                   m.status,
                   m.last_reply_at,
                   m.closed_at,
                   m.created_at,
                   m.updated_at,
                   (SELECT COUNT(*) FROM message_reply mr
                    WHERE mr.message_id = m.id) AS reply_count,
                   (SELECT mr2.content FROM message_reply mr2
                    WHERE mr2.message_id = m.id
                    ORDER BY mr2.created_at DESC, mr2.id DESC LIMIT 1)
                       AS last_reply_preview
            FROM interaction_message m
            JOIN sys_user su ON su.id = m.sender_user_id AND su.deleted = 0
            JOIN student s ON s.id = m.student_id AND s.deleted = 0
            JOIN sys_user stu ON stu.id = s.user_id AND stu.deleted = 0
            JOIN school_class c ON c.id = m.class_id AND c.deleted = 0
            JOIN grade g ON g.id = c.grade_id AND g.deleted = 0
            JOIN teacher t ON t.id = m.receiver_teacher_id AND t.deleted = 0
            JOIN sys_user tu ON tu.id = t.user_id AND tu.deleted = 0
            WHERE m.id = #{messageId} AND m.deleted = 0
            """)
    MessageVO selectDetail(@Param("messageId") Long messageId);

    @Select("""
            SELECT mr.id,
                   mr.replier_user_id,
                   u.real_name AS replier_name,
                   mr.replier_role,
                   mr.content,
                   mr.created_at
            FROM message_reply mr
            JOIN sys_user u ON u.id = mr.replier_user_id AND u.deleted = 0
            WHERE mr.message_id = #{messageId}
            ORDER BY mr.created_at, mr.id
            """)
    List<MessageReplyVO> selectReplies(@Param("messageId") Long messageId);

    @Select("""
            SELECT t.id,
                   u.real_name AS label,
                   CONCAT(
                     CASE tc.relation_type
                       WHEN 'HOMEROOM' THEN '班主任'
                       ELSE COALESCE(sub.subject_name, '任课教师')
                     END,
                     ' · ',
                     g.grade_name,
                     c.class_name
                   ) AS secondary
            FROM teacher_class tc
            JOIN teacher t ON t.id = tc.teacher_id AND t.deleted = 0
            JOIN sys_user u ON u.id = t.user_id AND u.deleted = 0
            JOIN school_class c ON c.id = tc.class_id AND c.deleted = 0
            JOIN grade g ON g.id = c.grade_id AND g.deleted = 0
            LEFT JOIN subject sub ON sub.id = tc.subject_id AND sub.deleted = 0
            WHERE tc.status = 'ACTIVE'
              AND tc.class_id = (
                SELECT sc.class_id FROM student_class sc
                WHERE sc.student_id = #{studentId}
                  AND sc.status = 'CURRENT' AND sc.deleted = 0 LIMIT 1
              )
            ORDER BY CASE WHEN tc.relation_type = 'HOMEROOM' THEN 0 ELSE 1 END,
                     u.real_name
            """)
    List<EntityOptionVO> selectTeacherOptions(@Param("studentId") Long studentId);
}

