package com.homeschool.platform.module.notice.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homeschool.platform.module.notice.dto.NoticeQuery;
import com.homeschool.platform.module.notice.service.NoticeAccessScope;
import com.homeschool.platform.module.notice.vo.NoticeReaderVO;
import com.homeschool.platform.module.notice.vo.NoticeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NoticeQueryMapper {

    @Select("""
            <script>
            SELECT n.id,
                   n.publisher_id,
                   u.real_name AS publisher_name,
                   n.scope_type,
                   n.grade_id,
                   g.grade_name,
                   n.class_id,
                   c.class_name,
                   n.title,
                   n.content,
                   n.status,
                   n.published_at,
                   n.withdrawn_at,
                   n.created_at,
                   n.updated_at,
                   EXISTS (
                       SELECT 1 FROM notice_read nr
                       WHERE nr.notice_id = n.id AND nr.user_id = #{userId}
                   ) AS is_read,
                   (SELECT nr2.read_at FROM notice_read nr2
                    WHERE nr2.notice_id = n.id AND nr2.user_id = #{userId} LIMIT 1)
                       AS read_at,
                   (SELECT COUNT(*) FROM notice_read nrc
                    WHERE nrc.notice_id = n.id) AS read_count
            FROM notice n
            JOIN sys_user u ON u.id = n.publisher_id AND u.deleted = 0
            LEFT JOIN grade g ON g.id = n.grade_id AND g.deleted = 0
            LEFT JOIN school_class c ON c.id = n.class_id AND c.deleted = 0
            WHERE n.deleted = 0
              AND n.status = 'PUBLISHED'
            <if test="query.keyword != null and query.keyword != ''">
                AND (n.title LIKE CONCAT('%', #{query.keyword}, '%')
                     OR n.content LIKE CONCAT('%', #{query.keyword}, '%'))
            </if>
              AND (
                #{scope.admin} = TRUE
                OR n.scope_type = 'ALL'
                <if test="scope.classIds != null and scope.classIds.size() > 0">
                    OR (n.scope_type = 'CLASS' AND n.class_id IN
                    <foreach collection="scope.classIds" item="classId"
                             open="(" separator="," close=")">#{classId}</foreach>)
                </if>
                <if test="scope.gradeIds != null and scope.gradeIds.size() > 0">
                    OR (n.scope_type = 'GRADE' AND n.grade_id IN
                    <foreach collection="scope.gradeIds" item="gradeId"
                             open="(" separator="," close=")">#{gradeId}</foreach>)
                </if>
                OR 1 = 0
              )
            ORDER BY n.published_at DESC, n.id DESC
            </script>
            """)
    IPage<NoticeVO> selectAccessiblePage(
            Page<NoticeVO> page,
            @Param("query") NoticeQuery query,
            @Param("scope") NoticeAccessScope scope,
            @Param("userId") Long userId);

    @Select("""
            <script>
            SELECT n.id,
                   n.publisher_id,
                   u.real_name AS publisher_name,
                   n.scope_type,
                   n.grade_id,
                   g.grade_name,
                   n.class_id,
                   c.class_name,
                   n.title,
                   n.content,
                   n.status,
                   n.published_at,
                   n.withdrawn_at,
                   n.created_at,
                   n.updated_at,
                   EXISTS (
                       SELECT 1 FROM notice_read nr
                       WHERE nr.notice_id = n.id AND nr.user_id = #{userId}
                   ) AS is_read,
                   (SELECT nr2.read_at FROM notice_read nr2
                    WHERE nr2.notice_id = n.id AND nr2.user_id = #{userId} LIMIT 1)
                       AS read_at,
                   (SELECT COUNT(*) FROM notice_read nrc
                    WHERE nrc.notice_id = n.id) AS read_count
            FROM notice n
            JOIN sys_user u ON u.id = n.publisher_id AND u.deleted = 0
            LEFT JOIN grade g ON g.id = n.grade_id AND g.deleted = 0
            LEFT JOIN school_class c ON c.id = n.class_id AND c.deleted = 0
            WHERE n.deleted = 0
            <if test="!admin">AND n.publisher_id = #{userId}</if>
            <if test="query.status != null and query.status != ''">
                AND n.status = #{query.status}
            </if>
            <if test="query.scopeType != null and query.scopeType != ''">
                AND n.scope_type = #{query.scopeType}
            </if>
            <if test="query.keyword != null and query.keyword != ''">
                AND (n.title LIKE CONCAT('%', #{query.keyword}, '%')
                     OR n.content LIKE CONCAT('%', #{query.keyword}, '%'))
            </if>
            ORDER BY n.id DESC
            </script>
            """)
    IPage<NoticeVO> selectManagePage(
            Page<NoticeVO> page,
            @Param("query") NoticeQuery query,
            @Param("userId") Long userId,
            @Param("admin") boolean admin);

    @Select("""
            SELECT n.id,
                   n.publisher_id,
                   u.real_name AS publisher_name,
                   n.scope_type,
                   n.grade_id,
                   g.grade_name,
                   n.class_id,
                   c.class_name,
                   n.title,
                   n.content,
                   n.status,
                   n.published_at,
                   n.withdrawn_at,
                   n.created_at,
                   n.updated_at,
                   EXISTS (
                       SELECT 1 FROM notice_read nr
                       WHERE nr.notice_id = n.id AND nr.user_id = #{userId}
                   ) AS is_read,
                   (SELECT nr2.read_at FROM notice_read nr2
                    WHERE nr2.notice_id = n.id AND nr2.user_id = #{userId} LIMIT 1)
                       AS read_at,
                   (SELECT COUNT(*) FROM notice_read nrc
                    WHERE nrc.notice_id = n.id) AS read_count
            FROM notice n
            JOIN sys_user u ON u.id = n.publisher_id AND u.deleted = 0
            LEFT JOIN grade g ON g.id = n.grade_id AND g.deleted = 0
            LEFT JOIN school_class c ON c.id = n.class_id AND c.deleted = 0
            WHERE n.id = #{noticeId} AND n.deleted = 0
            """)
    NoticeVO selectDetail(@Param("noticeId") Long noticeId, @Param("userId") Long userId);

    @Select("""
            SELECT nr.user_id,
                   u.username,
                   u.real_name,
                   MAX(r.role_code) AS role_code,
                   MAX(r.role_name) AS role_name,
                   nr.read_at
            FROM notice_read nr
            JOIN sys_user u ON u.id = nr.user_id AND u.deleted = 0
            LEFT JOIN sys_user_role ur ON ur.user_id = u.id
            LEFT JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0
            WHERE nr.notice_id = #{noticeId}
            GROUP BY nr.user_id, u.username, u.real_name, nr.read_at
            ORDER BY nr.read_at DESC
            """)
    IPage<NoticeReaderVO> selectReaders(
            Page<NoticeReaderVO> page,
            @Param("noticeId") Long noticeId);
}

