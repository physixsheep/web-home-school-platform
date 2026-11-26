package com.homeschool.platform.module.notice.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.homeschool.platform.common.constant.RoleCodes;
import com.homeschool.platform.common.exception.BusinessException;
import com.homeschool.platform.module.notice.entity.Notice;
import com.homeschool.platform.module.organization.entity.Parent;
import com.homeschool.platform.module.organization.entity.ParentStudent;
import com.homeschool.platform.module.organization.entity.SchoolClass;
import com.homeschool.platform.module.organization.entity.Student;
import com.homeschool.platform.module.organization.entity.StudentClass;
import com.homeschool.platform.module.organization.entity.Teacher;
import com.homeschool.platform.module.organization.entity.TeacherClass;
import com.homeschool.platform.module.organization.mapper.ParentMapper;
import com.homeschool.platform.module.organization.mapper.ParentStudentMapper;
import com.homeschool.platform.module.organization.mapper.SchoolClassMapper;
import com.homeschool.platform.module.organization.mapper.StudentClassMapper;
import com.homeschool.platform.module.organization.mapper.StudentMapper;
import com.homeschool.platform.module.organization.mapper.TeacherClassMapper;
import com.homeschool.platform.module.organization.mapper.TeacherMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class NoticeAccessService {

    private final TeacherMapper teacherMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final StudentMapper studentMapper;
    private final StudentClassMapper studentClassMapper;
    private final ParentMapper parentMapper;
    private final ParentStudentMapper parentStudentMapper;
    private final SchoolClassMapper schoolClassMapper;

    public NoticeAccessService(
            TeacherMapper teacherMapper,
            TeacherClassMapper teacherClassMapper,
            StudentMapper studentMapper,
            StudentClassMapper studentClassMapper,
            ParentMapper parentMapper,
            ParentStudentMapper parentStudentMapper,
            SchoolClassMapper schoolClassMapper) {
        this.teacherMapper = teacherMapper;
        this.teacherClassMapper = teacherClassMapper;
        this.studentMapper = studentMapper;
        this.studentClassMapper = studentClassMapper;
        this.parentMapper = parentMapper;
        this.parentStudentMapper = parentStudentMapper;
        this.schoolClassMapper = schoolClassMapper;
    }

    public NoticeAccessScope resolveScope(Long userId, List<String> roles) {
        boolean admin = roles.contains(RoleCodes.ADMIN);
        Set<Long> classIds = new LinkedHashSet<>();
        Set<Long> gradeIds = new LinkedHashSet<>();

        if (admin) {
            return new NoticeAccessScope(true, classIds, gradeIds);
        }
        if (roles.contains(RoleCodes.TEACHER)) {
            Teacher teacher = teacherMapper.selectOne(
                    Wrappers.<Teacher>lambdaQuery().eq(Teacher::getUserId, userId));
            if (teacher != null) {
                teacherClassMapper.selectList(
                                Wrappers.<TeacherClass>lambdaQuery()
                                        .eq(TeacherClass::getTeacherId, teacher.getId())
                                        .eq(TeacherClass::getStatus, "ACTIVE"))
                        .forEach(item -> classIds.add(item.getClassId()));
            }
        }
        if (roles.contains(RoleCodes.STUDENT)) {
            Student student = studentMapper.selectOne(
                    Wrappers.<Student>lambdaQuery().eq(Student::getUserId, userId));
            if (student != null) {
                addCurrentStudentClass(student.getId(), classIds);
            }
        }
        if (roles.contains(RoleCodes.PARENT)) {
            Parent parent = parentMapper.selectOne(
                    Wrappers.<Parent>lambdaQuery().eq(Parent::getUserId, userId));
            if (parent != null) {
                parentStudentMapper.selectList(
                                Wrappers.<ParentStudent>lambdaQuery()
                                        .eq(ParentStudent::getParentId, parent.getId())
                                        .eq(ParentStudent::getStatus, "ACTIVE"))
                        .forEach(binding ->
                                addCurrentStudentClass(binding.getStudentId(), classIds));
            }
        }

        for (Long classId : classIds) {
            SchoolClass schoolClass = schoolClassMapper.selectById(classId);
            if (schoolClass != null) {
                gradeIds.add(schoolClass.getGradeId());
            }
        }
        return new NoticeAccessScope(false, classIds, gradeIds);
    }

    public boolean canManage(Notice notice, Long userId, List<String> roles) {
        return roles.contains(RoleCodes.ADMIN)
                || notice.getPublisherId().equals(userId);
    }

    public boolean canAccess(
            Notice notice,
            Long userId,
            List<String> roles,
            NoticeAccessScope scope) {
        if (canManage(notice, userId, roles)) {
            return true;
        }
        if (!"PUBLISHED".equals(notice.getStatus())) {
            return false;
        }
        return switch (notice.getScopeType()) {
            case "ALL" -> true;
            case "GRADE" -> scope.gradeIds().contains(notice.getGradeId());
            case "CLASS" -> scope.classIds().contains(notice.getClassId());
            default -> false;
        };
    }

    public void requireAccess(
            Notice notice,
            Long userId,
            List<String> roles,
            NoticeAccessScope scope) {
        if (!canAccess(notice, userId, roles, scope)) {
            throw new BusinessException(403, "没有权限访问该通知");
        }
    }

    private void addCurrentStudentClass(Long studentId, Set<Long> classIds) {
        StudentClass current = studentClassMapper.selectOne(
                Wrappers.<StudentClass>lambdaQuery()
                        .eq(StudentClass::getStudentId, studentId)
                        .eq(StudentClass::getStatus, "CURRENT")
                        .last("LIMIT 1"));
        if (current != null) {
            classIds.add(current.getClassId());
        }
    }
}

