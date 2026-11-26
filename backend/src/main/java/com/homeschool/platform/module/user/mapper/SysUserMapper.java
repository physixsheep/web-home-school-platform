package com.homeschool.platform.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homeschool.platform.module.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("""
            SELECT r.role_code
            FROM sys_user_role ur
            JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0
            WHERE ur.user_id = #{userId} AND r.status = 'ENABLED'
            ORDER BY r.id
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    @Update("""
            UPDATE sys_user
            SET last_login_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{userId} AND deleted = 0
            """)
    int touchLastLogin(@Param("userId") Long userId);

    @Update("""
            UPDATE sys_user
            SET token_version = token_version + 1
            WHERE id = #{userId} AND deleted = 0
            """)
    int incrementTokenVersion(@Param("userId") Long userId);
}

