package com.homeschool.platform.module.assignment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.homeschool.platform.common.constant.RoleCodes;
import com.homeschool.platform.common.exception.BusinessException;
import com.homeschool.platform.module.assignment.entity.Assignment;
import com.homeschool.platform.module.assignment.entity.AssignmentSubmission;
import com.homeschool.platform.module.assignment.mapper.AssignmentMapper;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AssignmentAccessService {

    private final AssignmentMapper assignmentMapper;
    private final TeacherMapper teacherMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final StudentMapper studentMapper;
    private final StudentClassMapper studentClassMapper;
    private final ParentMapper parentMapper;
    private final ParentStudentMapper parentStudentMapper;

    public AssignmentAccessService(
            AssignmentMapper assignmentMapper,
            TeacherMapper teacherMapper,
            TeacherClassMapper teacherClassMapper,
            StudentMapper studentMapper,
            StudentClassMapper studentClassMapper,
            ParentMapper parentMapper,
            ParentStudentMapper parentStudentMapper) {
        this.assignmentMapper = assignmentMapper;
        this.teacherMapper = teacherMapper;
        this.teacherClassMapper = teacherClassMapper;
        this.studentMapper = studentMapper;
        this.studentClassMapper = studentClassMapper;
        this.parentMapper = parentMapper;
        this.parentStudentMapper = parentStudentMapper;
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

    public void requireTeaching(Long teacherId, Long classId, Long subjectId) {
        long count = teacherClassMapper.selectCount(
                Wrappers.<TeacherClass>lambdaQuery()
                        .eq(TeacherClass::getTeacherId, teacherId)
                        .eq(TeacherClass::getClassId, classId)
                        .eq(TeacherClass::getSubjectId, subjectId)
                        .eq(TeacherClass::getRelationType, "SUBJECT")
                        .eq(TeacherClass::getStatus, "ACTIVE"));
        if (count == 0) {
            throw new BusinessException(403, "只能向本人任教班级和学科发布作业");
        }
    }

    public Long resolveReadStudentId(UserPrincipal principal, Long requestedStudentId) {
        if (principal.getRoles().contains(RoleCodes.STUDENT)) {
            Student student = studentMapper.selectOne(
                    Wrappers.<Student>lambdaQuery()
                            .eq(Student::getUserId, principal.getUserId()));
            if (student == null) {
                throw new BusinessException(403, "学生档案不存在");
            }
            if (requestedStudentId != null && !student.getId().equals(requestedStudentId)) {
                throw new BusinessException(403, "学生只能查看本人作业");
            }
            return student.getId();
        }
        if (principal.getRoles().contains(RoleCodes.PARENT)) {
            if (requestedStudentId == null) {
                throw new BusinessException("家长查询作业时必须选择孩子");
            }
            requireParentBinding(principal.getUserId(), requestedStudentId);
            return requestedStudentId;
        }
        return requestedStudentId;
    }

    public List<Long> readableClassIds(UserPrincipal principal, Long targetStudentId) {
        if (principal.getRoles().contains(RoleCodes.ADMIN)) {
            return List.of();
        }
        if (principal.getRoles().contains(RoleCodes.TEACHER)) {
            Teacher teacher = requireTeacher(principal);
            Set<Long> ids = new LinkedHashSet<>();
            teacherClassMapper.selectList(
                            Wrappers.<TeacherClass>lambdaQuery()
                                    .eq(TeacherClass::getTeacherId, teacher.getId())
                                    .eq(TeacherClass::getStatus, "ACTIVE"))
                    .forEach(item -> ids.add(item.getClassId()));
            return List.copyOf(ids);
        }
        if (targetStudentId == null) {
            throw new BusinessException("当前查询缺少学生信息");
        }
        StudentClass current = studentClassMapper.selectOne(
                Wrappers.<StudentClass>lambdaQuery()
                        .eq(StudentClass::getStudentId, targetStudentId)
                        .eq(StudentClass::getStatus, "CURRENT")
                        .last("LIMIT 1"));
        return current == null ? List.of() : List.of(current.getClassId());
    }

    public boolean canManage(Assignment assignment, UserPrincipal principal) {
        if (principal.getRoles().contains(RoleCodes.ADMIN)) {
            return true;
        }
        if (!principal.getRoles().contains(RoleCodes.TEACHER)) {
            return false;
        }
        Teacher teacher = requireTeacher(principal);
        return assignment.getTeacherId().equals(teacher.getId());
    }

    public void requireManage(Assignment assignment, UserPrincipal principal) {
        if (!canManage(assignment, principal)) {
            throw new BusinessException(403, "只能管理本人发布的作业");
        }
    }

    public boolean canReadAssignment(
            Assignment assignment,
            UserPrincipal principal,
            Long targetStudentId) {
        if (principal.getRoles().contains(RoleCodes.ADMIN)) {
            return true;
        }
        if (principal.getRoles().contains(RoleCodes.TEACHER)) {
            try {
                Teacher teacher = requireTeacher(principal);
                if (assignment.getTeacherId().equals(teacher.getId())) {
                    return true;
                }
                return teacherClassMapper.selectCount(
                                Wrappers.<TeacherClass>lambdaQuery()
                                        .eq(TeacherClass::getTeacherId, teacher.getId())
                                        .eq(TeacherClass::getClassId, assignment.getClassId())
                                        .eq(TeacherClass::getStatus, "ACTIVE"))
                        > 0;
            } catch (BusinessException exception) {
                return false;
            }
        }
        if (!"PUBLISHED".equals(assignment.getStatus())
                && !"CLOSED".equals(assignment.getStatus())) {
            return false;
        }
        if (targetStudentId == null) {
            return false;
        }
        return readableClassIds(principal, targetStudentId)
                .contains(assignment.getClassId());
    }

    public boolean canAccessAssignmentAsset(
            Assignment assignment,
            UserPrincipal principal) {
        if (principal.getRoles().contains(RoleCodes.ADMIN)) {
            return true;
        }
        if (principal.getRoles().contains(RoleCodes.TEACHER)) {
            return canReadAssignment(assignment, principal, null);
        }
        if (principal.getRoles().contains(RoleCodes.STUDENT)) {
            Student student = studentMapper.selectOne(
                    Wrappers.<Student>lambdaQuery()
                            .eq(Student::getUserId, principal.getUserId()));
            if (student == null) {
                return false;
            }
            StudentClass current = studentClassMapper.selectOne(
                    Wrappers.<StudentClass>lambdaQuery()
                            .eq(StudentClass::getStudentId, student.getId())
                            .eq(StudentClass::getStatus, "CURRENT")
                            .last("LIMIT 1"));
            return current != null && current.getClassId().equals(assignment.getClassId());
        }
        if (principal.getRoles().contains(RoleCodes.PARENT)) {
            Parent parent = parentMapper.selectOne(
                    Wrappers.<Parent>lambdaQuery()
                            .eq(Parent::getUserId, principal.getUserId()));
            if (parent == null) {
                return false;
            }
            for (ParentStudent binding : parentStudentMapper.selectList(
                    Wrappers.<ParentStudent>lambdaQuery()
                            .eq(ParentStudent::getParentId, parent.getId())
                            .eq(ParentStudent::getStatus, "ACTIVE"))) {
                StudentClass current = studentClassMapper.selectOne(
                        Wrappers.<StudentClass>lambdaQuery()
                                .eq(StudentClass::getStudentId, binding.getStudentId())
                                .eq(StudentClass::getStatus, "CURRENT")
                                .last("LIMIT 1"));
                if (current != null && current.getClassId().equals(assignment.getClassId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canReadSubmission(
            AssignmentSubmission submission,
            UserPrincipal principal) {
        if (principal.getRoles().contains(RoleCodes.ADMIN)) {
            return true;
        }
        Assignment assignment = assignmentMapper.selectById(submission.getAssignmentId());
        if (assignment == null) {
            return false;
        }
        if (principal.getRoles().contains(RoleCodes.TEACHER)) {
            return canManage(assignment, principal);
        }
        if (principal.getRoles().contains(RoleCodes.STUDENT)) {
            Student student = studentMapper.selectOne(
                    Wrappers.<Student>lambdaQuery()
                            .eq(Student::getUserId, principal.getUserId()));
            return student != null && student.getId().equals(submission.getStudentId());
        }
        if (principal.getRoles().contains(RoleCodes.PARENT)) {
            return isActiveParentBinding(principal.getUserId(), submission.getStudentId());
        }
        return false;
    }

    public void requireParentBinding(Long userId, Long studentId) {
        if (!isActiveParentBinding(userId, studentId)) {
            throw new BusinessException(403, "只能查看本人绑定孩子的作业");
        }
    }

    private boolean isActiveParentBinding(Long userId, Long studentId) {
        Parent parent = parentMapper.selectOne(
                Wrappers.<Parent>lambdaQuery()
                        .eq(Parent::getUserId, userId));
        if (parent == null) {
            return false;
        }
        return parentStudentMapper.selectCount(
                Wrappers.<ParentStudent>lambdaQuery()
                        .eq(ParentStudent::getParentId, parent.getId())
                        .eq(ParentStudent::getStudentId, studentId)
                        .eq(ParentStudent::getStatus, "ACTIVE")) > 0;
    }
}

