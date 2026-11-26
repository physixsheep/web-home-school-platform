package com.homeschool.platform.module.leave.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.homeschool.platform.common.constant.RoleCodes;
import com.homeschool.platform.common.exception.BusinessException;
import com.homeschool.platform.module.leave.entity.LeaveRequest;
import com.homeschool.platform.module.organization.entity.Parent;
import com.homeschool.platform.module.organization.entity.ParentStudent;
import com.homeschool.platform.module.organization.entity.Student;
import com.homeschool.platform.module.organization.entity.Teacher;
import com.homeschool.platform.module.organization.entity.TeacherClass;
import com.homeschool.platform.module.organization.mapper.ParentMapper;
import com.homeschool.platform.module.organization.mapper.ParentStudentMapper;
import com.homeschool.platform.module.organization.mapper.StudentMapper;
import com.homeschool.platform.module.organization.mapper.TeacherClassMapper;
import com.homeschool.platform.module.organization.mapper.TeacherMapper;
import com.homeschool.platform.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveAccessService {

    private final StudentMapper studentMapper;
    private final ParentMapper parentMapper;
    private final ParentStudentMapper parentStudentMapper;
    private final TeacherMapper teacherMapper;
    private final TeacherClassMapper teacherClassMapper;

    public LeaveAccessService(
            StudentMapper studentMapper,
            ParentMapper parentMapper,
            ParentStudentMapper parentStudentMapper,
            TeacherMapper teacherMapper,
            TeacherClassMapper teacherClassMapper) {
        this.studentMapper = studentMapper;
        this.parentMapper = parentMapper;
        this.parentStudentMapper = parentStudentMapper;
        this.teacherMapper = teacherMapper;
        this.teacherClassMapper = teacherClassMapper;
    }

    public Student requireStudent(UserPrincipal principal) {
        Student student = studentMapper.selectOne(
                Wrappers.<Student>lambdaQuery()
                        .eq(Student::getUserId, principal.getUserId()));
        if (student == null) {
            throw new BusinessException(403, "学生档案不存在");
        }
        return student;
    }

    public Long resolveParentStudentId(UserPrincipal principal, Long requestedStudentId) {
        if (requestedStudentId == null) {
            throw new BusinessException("家长查询请假记录时必须选择孩子");
        }
        Parent parent = parentMapper.selectOne(
                Wrappers.<Parent>lambdaQuery()
                        .eq(Parent::getUserId, principal.getUserId()));
        if (parent == null || parentStudentMapper.selectCount(
                Wrappers.<ParentStudent>lambdaQuery()
                        .eq(ParentStudent::getParentId, parent.getId())
                        .eq(ParentStudent::getStudentId, requestedStudentId)
                        .eq(ParentStudent::getStatus, "ACTIVE")) == 0) {
            throw new BusinessException(403, "只能查看本人绑定孩子的请假记录");
        }
        return requestedStudentId;
    }

    public List<Long> homeroomClassIds(UserPrincipal principal) {
        Teacher teacher = requireTeacher(principal);
        LocalDate today = LocalDate.now();
        return teacherClassMapper.selectList(
                        Wrappers.<TeacherClass>lambdaQuery()
                                .eq(TeacherClass::getTeacherId, teacher.getId())
                                .eq(TeacherClass::getRelationType, "HOMEROOM")
                                .eq(TeacherClass::getStatus, "ACTIVE")
                                .le(TeacherClass::getStartDate, today)
                                .and(query -> query.isNull(TeacherClass::getEndDate)
                                        .or()
                                        .ge(TeacherClass::getEndDate, today)))
                .stream()
                .map(TeacherClass::getClassId)
                .distinct()
                .toList();
    }

    public Teacher requireTeacher(UserPrincipal principal) {
        Teacher teacher = teacherMapper.selectOne(
                Wrappers.<Teacher>lambdaQuery()
                        .eq(Teacher::getUserId, principal.getUserId()));
        if (teacher == null || !"ACTIVE".equals(teacher.getStatus())) {
            throw new BusinessException(403, "教师档案不存在或已停用");
        }
        return teacher;
    }

    public boolean canAccess(LeaveRequest leave, UserPrincipal principal) {
        if (principal.getRoles().contains(RoleCodes.ADMIN)) {
            return true;
        }
        if (principal.getRoles().contains(RoleCodes.STUDENT)) {
            Student student = studentMapper.selectOne(
                    Wrappers.<Student>lambdaQuery()
                            .eq(Student::getUserId, principal.getUserId()));
            return student != null && student.getId().equals(leave.getStudentId());
        }
        if (principal.getRoles().contains(RoleCodes.PARENT)) {
            Parent parent = parentMapper.selectOne(
                    Wrappers.<Parent>lambdaQuery()
                            .eq(Parent::getUserId, principal.getUserId()));
            return parent != null && parentStudentMapper.selectCount(
                    Wrappers.<ParentStudent>lambdaQuery()
                            .eq(ParentStudent::getParentId, parent.getId())
                            .eq(ParentStudent::getStudentId, leave.getStudentId())
                            .eq(ParentStudent::getStatus, "ACTIVE")) > 0;
        }
        if (principal.getRoles().contains(RoleCodes.TEACHER)) {
            return homeroomClassIds(principal).contains(leave.getClassId());
        }
        return false;
    }

    public void requireAccess(LeaveRequest leave, UserPrincipal principal) {
        if (!canAccess(leave, principal)) {
            throw new BusinessException(403, "没有权限访问该请假申请");
        }
    }

    public boolean isHomeroom(Long userId, Long classId) {
        UserPrincipal synthetic = new UserPrincipal(
                userId,
                "",
                "",
                "",
                "ENABLED",
                0,
                List.of(RoleCodes.TEACHER));
        return homeroomClassIds(synthetic).contains(classId);
    }
}

