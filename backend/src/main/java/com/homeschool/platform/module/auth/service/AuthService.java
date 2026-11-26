package com.homeschool.platform.module.auth.service;

import com.homeschool.platform.common.exception.BusinessException;
import com.homeschool.platform.module.auth.dto.LoginRequest;
import com.homeschool.platform.module.auth.vo.CurrentUserResponse;
import com.homeschool.platform.module.auth.vo.LoginResponse;
import com.homeschool.platform.module.user.mapper.SysUserMapper;
import com.homeschool.platform.security.JwtService;
import com.homeschool.platform.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SysUserMapper userMapper;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            SysUserMapper userMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username().trim(),
                            request.password()));
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            userMapper.touchLastLogin(principal.getUserId());
            String token = jwtService.generateToken(principal);
            return new LoginResponse(
                    token,
                    "Bearer",
                    jwtService.getExpirationSeconds(),
                    toCurrentUser(principal));
        } catch (AuthenticationException exception) {
            throw new BusinessException(401, "账号或密码错误，或账号已被禁用");
        }
    }

    @Transactional
    public void logout(UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "登录状态无效");
        }
        userMapper.incrementTokenVersion(principal.getUserId());
        SecurityContextHolder.clearContext();
    }

    public CurrentUserResponse currentUser(UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "登录状态无效");
        }
        return toCurrentUser(principal);
    }

    private CurrentUserResponse toCurrentUser(UserPrincipal principal) {
        return new CurrentUserResponse(
                principal.getUserId(),
                principal.getUsername(),
                principal.getRealName(),
                principal.getRoles());
    }
}

