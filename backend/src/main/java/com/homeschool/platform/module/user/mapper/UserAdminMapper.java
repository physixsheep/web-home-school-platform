package com.homeschool.platform.module.user.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homeschool.platform.module.user.dto.UserQuery;
import com.homeschool.platform.module.user.vo.UserAdminVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserAdminMapper {

    @Select("""
            <script>
            SELECT u.id,
                   u.username,
                   u.real_name,
                   u.phone,
                   u.email,
                   u.status,
                   u.last_login_at,
                   MAX(r.role_code) AS role_code,
                   MAX(r.role_name) AS role_name,
                   COALESCE(MAX(t.employee_no), MAX(s.student_no), '') AS identifier,
                   COALESCE(MAX(t.gender), MAX(s.gender), MAX(p.gender), '') AS gender,
                   COALESCE(MAX(t.title), MAX(s.enrollment_date), MAX(p.occupation), '') AS extra_info
            FROM sys_user u
            LEFT JOIN sys_user_role ur ON ur.user_id = u.id
            LEFT JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0
            LEFT JOIN teacher t ON t.user_id = u.id AND t.deleted = 0
            LEFT JOIN student s ON s.user_id = u.id AND s.deleted = 0
            LEFT JOIN parent p ON p.user_id = u.id AND p.deleted = 0
            WHERE u.deleted = 0
            <if test="query.status != null and query.status != ''">
                AND u.status = #{query.status}
            </if>
            <if test="query.roleCode != null and query.roleCode != ''">
                AND EXISTS (
                    SELECT 1
                    FROM sys_user_role fur
                    JOIN sys_role fr ON fr.id = fur.role_id AND fr.deleted = 0
                    WHERE fur.user_id = u.id AND fr.role_code = #{query.roleCode}
                )
            </if>
            <if test="query.keyword != null and query.keyword != ''">
                AND (
                    u.username LIKE CONCAT('%', #{query.keyword}, '%')
                    OR u.real_name LIKE CONCAT('%', #{query.keyword}, '%')
                    OR u.phone LIKE CONCAT('%', #{query.keyword}, '%')
                    OR t.employee_no LIKE CONCAT('%', #{query.keyword}, '%')
                    OR s.student_no LIKE CONCAT('%', #{query.keyword}, '%')
                )
            </if>
            GROUP BY u.id, u.username, u.real_name, u.phone, u.email, u.status,
                     u.last_login_at, u.created_at
            ORDER BY u.id DESC
            </script>
            """)
    IPage<UserAdminVO> selectUserPage(
            Page<UserAdminVO> page,
            @Param("query") UserQuery query);
}

