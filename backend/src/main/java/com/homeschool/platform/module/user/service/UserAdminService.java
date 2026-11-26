package com.homeschool.platform.module.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homeschool.platform.common.api.PageResponse;
import com.homeschool.platform.common.constant.RoleCodes;
import com.homeschool.platform.common.exception.BusinessException;
import com.homeschool.platform.module.organization.entity.Parent;
import com.homeschool.platform.module.organization.entity.Student;
import com.homeschool.platform.module.organization.entity.Teacher;
import com.homeschool.platform.module.organization.mapper.ParentMapper;
import com.homeschool.platform.module.organization.mapper.StudentMapper;
import com.homeschool.platform.module.organization.mapper.TeacherMapper;
import com.homeschool.platform.module.user.dto.CreateUserRequest;
import com.homeschool.platform.module.user.dto.ResetPasswordRequest;
import com.homeschool.platform.module.user.dto.UpdateUserRequest;
import com.homeschool.platform.module.user.dto.UserQuery;
import com.homeschool.platform.module.user.entity.SysRole;
import com.homeschool.platform.module.user.entity.SysUser;
import com.homeschool.platform.module.user.entity.SysUserRole;
import com.homeschool.platform.module.user.mapper.SysRoleMapper;
import com.homeschool.platform.module.user.mapper.SysUserMapper;
import com.homeschool.platform.module.user.mapper.SysUserRoleMapper;
import com.homeschool.platform.module.user.mapper.UserAdminMapper;
import com.homeschool.platform.module.user.vo.RoleOptionVO;
import com.homeschool.platform.module.user.vo.UserAdminVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserAdminService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final UserAdminMapper userAdminMapper;
    private final TeacherMapper teacherMapper;
    private final StudentMapper studentMapper;
    private final ParentMapper parentMapper;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(
            SysUserMapper userMapper,
            SysRoleMapper roleMapper,
            SysUserRoleMapper userRoleMapper,
            UserAdminMapper userAdminMapper,
            TeacherMapper teacherMapper,
            StudentMapper studentMapper,
            ParentMapper parentMapper,
            PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.userAdminMapper = userAdminMapper;
        this.teacherMapper = teacherMapper;
        this.studentMapper = studentMapper;
        this.parentMapper = parentMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResponse<UserAdminVO> page(UserQuery query) {
        validateStatus(query.getStatus());
        if (StringUtils.hasText(query.getRoleCode())
                && !List.of(
                        RoleCodes.ADMIN,
                        RoleCodes.TEACHER,
                        RoleCodes.STUDENT,
                        RoleCodes.PARENT)
                .contains(query.getRoleCode())) {
            throw new BusinessException("角色筛选值不正确");
        }
        query.setKeyword(trimToNull(query.getKeyword()));
        Page<UserAdminVO> page = new Page<>(
                query.normalizedPage(),
                query.normalizedSize());
        IPage<UserAdminVO> result = userAdminMapper.selectUserPage(page, query);
        return PageResponse.from(result);
    }

    public List<RoleOptionVO> roles() {
        return roleMapper.selectList(
                        Wrappers.<SysRole>lambdaQuery()
                                .eq(SysRole::getStatus, "ENABLED")
                                .orderByAsc(SysRole::getId))
                .stream()
                .map(role -> new RoleOptionVO(
                        role.getId(),
                        role.getRoleCode(),
                        role.getRoleName(),
                        role.getDescription()))
                .toList();
    }

    @Transactional
    public Long create(CreateUserRequest request, Long operatorId) {
        String username = request.username().trim();
        if (userMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username)) > 0) {
            throw new BusinessException(409, "登录账号已存在");
        }

        SysRole role = roleMapper.selectOne(
                Wrappers.<SysRole>lambdaQuery()
                        .eq(SysRole::getRoleCode, request.roleCode())
                        .eq(SysRole::getStatus, "ENABLED"));
        if (role == null) {
            throw new BusinessException("角色不存在或已停用");
        }
        validateProfileRequest(request);

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRealName(request.realName().trim());
        user.setPhone(trimToNull(request.phone()));
        user.setEmail(trimToNull(request.email()));
        user.setStatus("ENABLED");
        user.setTokenVersion(0);
        userMapper.insert(user);

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRole.setCreatedBy(operatorId);
        userRoleMapper.insert(userRole);

        switch (request.roleCode()) {
            case RoleCodes.TEACHER -> insertTeacher(user.getId(), request);
            case RoleCodes.STUDENT -> insertStudent(user.getId(), request);
            case RoleCodes.PARENT -> insertParent(user.getId(), request);
            default -> {
                // 管理员没有独立档案表。
            }
        }
        return user.getId();
    }

    @Transactional
    public void update(Long userId, UpdateUserRequest request) {
        SysUser user = requireUser(userId);
        String phone = trimToNull(request.phone());
        if (phone != null) {
            Long duplicateCount = userMapper.selectCount(
                    Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getPhone, phone)
                            .ne(SysUser::getId, userId));
            if (duplicateCount > 0) {
                throw new BusinessException(409, "手机号已被其他账号使用");
            }
        }
        user.setRealName(request.realName().trim());
        user.setPhone(phone);
        user.setEmail(trimToNull(request.email()));
        userMapper.updateById(user);
    }

    @Transactional
    public void updateStatus(Long userId, String status, Long operatorId) {
        validateStatus(status);
        if (userId.equals(operatorId) && "DISABLED".equals(status)) {
            throw new BusinessException("不能禁用当前登录的管理员账号");
        }
        SysUser user = requireUser(userId);
        user.setStatus(status);
        if ("DISABLED".equals(status)) {
            user.setTokenVersion(user.getTokenVersion() + 1);
        }
        userMapper.updateById(user);
    }

    @Transactional
    public void resetPassword(Long userId, ResetPasswordRequest request) {
        SysUser user = requireUser(userId);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userMapper.updateById(user);
    }

    private void validateProfileRequest(CreateUserRequest request) {
        if (RoleCodes.TEACHER.equals(request.roleCode())
                && !StringUtils.hasText(request.identifier())) {
            throw new BusinessException("教师工号不能为空");
        }
        if (RoleCodes.STUDENT.equals(request.roleCode())
                && !StringUtils.hasText(request.identifier())) {
            throw new BusinessException("学生学号不能为空");
        }
    }

    private void insertTeacher(Long userId, CreateUserRequest request) {
        Teacher teacher = new Teacher();
        teacher.setUserId(userId);
        teacher.setEmployeeNo(request.identifier().trim());
        teacher.setGender(request.gender());
        teacher.setTitle(trimToNull(request.title()));
        teacher.setSpecialty(trimToNull(request.specialty()));
        teacher.setHireDate(request.hireDate());
        teacher.setStatus("ACTIVE");
        teacherMapper.insert(teacher);
    }

    private void insertStudent(Long userId, CreateUserRequest request) {
        Student student = new Student();
        student.setUserId(userId);
        student.setStudentNo(request.identifier().trim());
        student.setGender(request.gender());
        student.setBirthDate(request.birthDate());
        student.setEnrollmentDate(request.enrollmentDate());
        student.setStatus("ACTIVE");
        studentMapper.insert(student);
    }

    private void insertParent(Long userId, CreateUserRequest request) {
        Parent parent = new Parent();
        parent.setUserId(userId);
        parent.setGender(request.gender());
        parent.setOccupation(trimToNull(request.occupation()));
        parent.setEmergencyContact(trimToNull(request.emergencyContact()));
        parent.setStatus("ACTIVE");
        parentMapper.insert(parent);
    }

    private SysUser requireUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private void validateStatus(String status) {
        if (status != null
                && !status.isBlank()
                && !"ENABLED".equals(status)
                && !"DISABLED".equals(status)) {
            throw new BusinessException("账号状态值不正确");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

