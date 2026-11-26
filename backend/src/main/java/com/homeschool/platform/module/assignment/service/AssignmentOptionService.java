package com.homeschool.platform.module.assignment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.homeschool.platform.common.constant.RoleCodes;
import com.homeschool.platform.module.assignment.vo.AssignmentOptionsVO;
import com.homeschool.platform.module.assignment.vo.TeachingOptionVO;
import com.homeschool.platform.module.organization.entity.Grade;
import com.homeschool.platform.module.organization.entity.Parent;
import com.homeschool.platform.module.organization.entity.ParentStudent;
import com.homeschool.platform.module.organization.entity.SchoolClass;
import com.homeschool.platform.module.organization.entity.Student;
import com.homeschool.platform.module.organization.entity.Subject;
import com.homeschool.platform.module.organization.entity.Teacher;
import com.homeschool.platform.module.organization.entity.TeacherClass;
import com.homeschool.platform.module.organization.mapper.GradeMapper;
import com.homeschool.platform.module.organization.mapper.ParentMapper;
import com.homeschool.platform.module.organization.mapper.ParentStudentMapper;
import com.homeschool.platform.module.organization.mapper.SchoolClassMapper;
import com.homeschool.platform.module.organization.mapper.StudentMapper;
import com.homeschool.platform.module.organization.mapper.SubjectMapper;
import com.homeschool.platform.module.organization.mapper.TeacherClassMapper;
import com.homeschool.platform.module.organization.mapper.TeacherMapper;
import com.homeschool.platform.module.organization.vo.EntityOptionVO;
import com.homeschool.platform.module.user.entity.SysUser;
import com.homeschool.platform.module.user.mapper.SysUserMapper;
import com.homeschool.platform.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssignmentOptionService {

    private final TeacherMapper teacherMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final SchoolClassMapper schoolClassMapper;
    private final GradeMapper gradeMapper;
    private final SubjectMapper subjectMapper;
    private final ParentMapper parentMapper;
    private final ParentStudentMapper parentStudentMapper;
    private final StudentMapper studentMapper;
    private final SysUserMapper userMapper;

    public AssignmentOptionService(
            TeacherMapper teacherMapper,
            TeacherClassMapper teacherClassMapper,
            SchoolClassMapper schoolClassMapper,
            GradeMapper gradeMapper,
            SubjectMapper subjectMapper,
            ParentMapper parentMapper,
            ParentStudentMapper parentStudentMapper,
            StudentMapper studentMapper,
            SysUserMapper userMapper) {
        this.teacherMapper = teacherMapper;
        this.teacherClassMapper = teacherClassMapper;
        this.schoolClassMapper = schoolClassMapper;
        this.gradeMapper = gradeMapper;
        this.subjectMapper = subjectMapper;
        this.parentMapper = parentMapper;
        this.parentStudentMapper = parentStudentMapper;
        this.studentMapper = studentMapper;
        this.userMapper = userMapper;
    }

    public AssignmentOptionsVO options(UserPrincipal principal) {
        return new AssignmentOptionsVO(
                principal.getRoles().contains(RoleCodes.TEACHER)
                        ? teachingOptions(principal.getUserId())
                        : List.of(),
                principal.getRoles().contains(RoleCodes.PARENT)
                        ? childOptions(principal.getUserId())
                        : List.of());
    }

    private List<TeachingOptionVO> teachingOptions(Long userId) {
        Teacher teacher = teacherMapper.selectOne(
                Wrappers.<Teacher>lambdaQuery().eq(Teacher::getUserId, userId));
        if (teacher == null) {
            return List.of();
        }
        List<TeacherClass> relations = teacherClassMapper.selectList(
                Wrappers.<TeacherClass>lambdaQuery()
                        .eq(TeacherClass::getTeacherId, teacher.getId())
                        .eq(TeacherClass::getRelationType, "SUBJECT")
                        .eq(TeacherClass::getStatus, "ACTIVE"));
        if (relations.isEmpty()) {
            return List.of();
        }
        Map<Long, SchoolClass> classes = new HashMap<>();
        schoolClassMapper.selectBatchIds(
                        relations.stream().map(TeacherClass::getClassId).distinct().toList())
                .forEach(item -> classes.put(item.getId(), item));
        if (classes.isEmpty()) {
            return List.of();
        }
        Map<Long, Grade> grades = new HashMap<>();
        gradeMapper.selectBatchIds(
                        classes.values().stream().map(SchoolClass::getGradeId).distinct().toList())
                .forEach(item -> grades.put(item.getId(), item));
        Map<Long, Subject> subjects = new HashMap<>();
        subjectMapper.selectBatchIds(
                        relations.stream().map(TeacherClass::getSubjectId).distinct().toList())
                .forEach(item -> subjects.put(item.getId(), item));

        List<TeachingOptionVO> result = new ArrayList<>();
        for (TeacherClass relation : relations) {
            SchoolClass schoolClass = classes.get(relation.getClassId());
            Subject subject = subjects.get(relation.getSubjectId());
            if (schoolClass == null || subject == null) {
                continue;
            }
            Grade grade = grades.get(schoolClass.getGradeId());
            result.add(new TeachingOptionVO(
                    schoolClass.getId(),
                    schoolClass.getClassName(),
                    schoolClass.getGradeId(),
                    grade == null ? "" : grade.getGradeName(),
                    subject.getId(),
                    subject.getSubjectName(),
                    subject.getDefaultFullScore()));
        }
        return result;
    }

    private List<EntityOptionVO> childOptions(Long userId) {
        Parent parent = parentMapper.selectOne(
                Wrappers.<Parent>lambdaQuery().eq(Parent::getUserId, userId));
        if (parent == null) {
            return List.of();
        }
        List<Long> studentIds = parentStudentMapper.selectList(
                        Wrappers.<ParentStudent>lambdaQuery()
                                .eq(ParentStudent::getParentId, parent.getId())
                                .eq(ParentStudent::getStatus, "ACTIVE"))
                .stream()
                .map(ParentStudent::getStudentId)
                .toList();
        if (studentIds.isEmpty()) {
            return List.of();
        }
        List<Student> students = studentMapper.selectBatchIds(studentIds);
        Map<Long, SysUser> users = new HashMap<>();
        userMapper.selectBatchIds(
                        students.stream().map(Student::getUserId).toList())
                .forEach(item -> users.put(item.getId(), item));
        return students.stream().map(student -> new EntityOptionVO(
                student.getId(),
                users.containsKey(student.getUserId())
                        ? users.get(student.getUserId()).getRealName()
                        : student.getStudentNo(),
                student.getStudentNo())).toList();
    }
}

