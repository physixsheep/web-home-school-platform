package com.homeschool.platform.module.interaction.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.homeschool.platform.common.constant.RoleCodes;
import com.homeschool.platform.common.exception.BusinessException;
import com.homeschool.platform.module.interaction.entity.InteractionMessage;
import com.homeschool.platform.module.organization.entity.Parent;
import com.homeschool.platform.module.organization.entity.ParentStudent;
import com.homeschool.platform.module.organization.entity.Student;
import com.homeschool.platform.module.organization.entity.StudentClass;
import com.homeschool.platform.module.organization.entity.Teacher;
import com.homeschool.platform.module.organization.entity.TeacherClass;
import com.homeschool.platform.module.organization.mapper.ParentMapper;
import com.homeschool.platform.module.organization.mapper.ParentStudentMapper;
import com.homeschool.platform.module.organization.mapper.StudentClassMapper;
import com.homeschool.platform.module.organization.mapper.StudentMapper;
import com.homeschool.platform.module.organization.mapper.TeacherClassMapper;
import com.homeschool.platform.module.organization.mapper.TeacherMapper;
import com.homeschool.platform.security.UserPrincipal;
import org.springframework.stereotype.Service;

@Service
public class MessageAccessService {

    private final StudentMapper studentMapper;
    private final StudentClassMapper studentClassMapper;
    private final ParentMapper parentMapper;
    private final ParentStudentMapper parentStudentMapper;
    private final TeacherMapper teacherMapper;
    private final TeacherClassMapper teacherClassMapper;

    public MessageAccessService(
            StudentMapper studentMapper,
            StudentClassMapper studentClassMapper,
            ParentMapper parentMapper,
            ParentStudentMapper parentStudentMapper,
            TeacherMapper teacherMapper,
            TeacherClassMapper teacherClassMapper) {
        this.studentMapper = studentMapper;
        this.studentClassMapper = studentClassMapper;
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

    public Teacher requireTeacher(UserPrincipal principal) {
        Teacher teacher = teacherMapper.selectOne(
                Wrappers.<Teacher>lambdaQuery()
                        .eq(Teacher::getUserId, principal.getUserId()));
        if (teacher == null || !"ACTIVE".equals(teacher.getStatus())) {
            throw new BusinessException(403, "教师档案不存在或已停用");
        }
        return teacher;
    }

    public Student resolveCreateStudent(UserPrincipal principal, Long studentId) {
        if (principal.getRoles().contains(RoleCodes.STUDENT)) {
            return requireStudent(principal);
        }
        if (principal.getRoles().contains(RoleCodes.PARENT)) {
            if (studentId == null) {
                throw new BusinessException("家长发起留言时必须选择孩子");
            }
            Parent parent = parentMapper.selectOne(
                    Wrappers.<Parent>lambdaQuery()
                            .eq(Parent::getUserId, principal.getUserId()));
            if (parent == null || parentStudentMapper.selectCount(
                    Wrappers.<ParentStudent>lambdaQuery()
                            .eq(ParentStudent::getParentId, parent.getId())
                            .eq(ParentStudent::getStudentId, studentId)
                            .eq(ParentStudent::getStatus, "ACTIVE")) == 0) {
                throw new BusinessException(403, "只能为本人绑定孩子发起留言");
            }
            Student student = studentMapper.selectById(studentId);
            if (student == null) {
                throw new BusinessException(404, "学生不存在");
            }
            return student;
        }
        throw new BusinessException(403, "当前角色不能发起家校留言");
    }

    public StudentClass currentClass(Long studentId) {
        StudentClass current = studentClassMapper.selectOne(
                Wrappers.<StudentClass>lambdaQuery()
                        .eq(StudentClass::getStudentId, studentId)
                        .eq(StudentClass::getStatus, "CURRENT")
                        .last("LIMIT 1"));
        if (current == null) {
            throw new BusinessException(409, "学生尚未分班，不能发起家校留言");
        }
        return current;
    }

    public void requireTeacherForClass(Long teacherId, Long classId) {
        long count = teacherClassMapper.selectCount(
                Wrappers.<TeacherClass>lambdaQuery()
                        .eq(TeacherClass::getTeacherId, teacherId)
                        .eq(TeacherClass::getClassId, classId)
                        .eq(TeacherClass::getStatus, "ACTIVE"));
        if (count == 0) {
            throw new BusinessException(403, "只能向学生当前班级的任课教师留言");
        }
    }

    public boolean canParticipate(InteractionMessage message, UserPrincipal principal) {
        if (principal.getRoles().contains(RoleCodes.ADMIN)) {
            return true;
        }
        if (message.getSenderUserId().equals(principal.getUserId())) {
            return true;
        }
        if (principal.getRoles().contains(RoleCodes.TEACHER)) {
            Teacher teacher = requireTeacher(principal);
            return teacher.getId().equals(message.getReceiverTeacherId());
        }
        return false;
    }

    public void requireParticipant(InteractionMessage message, UserPrincipal principal) {
        if (!canParticipate(message, principal)) {
            throw new BusinessException(403, "没有权限访问该留言会话");
        }
    }
}

