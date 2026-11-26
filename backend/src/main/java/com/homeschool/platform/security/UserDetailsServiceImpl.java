package com.homeschool.platform.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.homeschool.platform.module.user.entity.SysUser;
import com.homeschool.platform.module.user.mapper.SysUserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;

    public UserDetailsServiceImpl(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("账号不存在");
        }

        List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
        if (roles.isEmpty()) {
            throw new UsernameNotFoundException("账号未分配有效角色");
        }
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getRealName(),
                user.getStatus(),
                user.getTokenVersion(),
                roles);
    }
}

