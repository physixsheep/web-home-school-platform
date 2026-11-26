package com.homeschool.platform.module.notice.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.homeschool.platform.common.constant.RoleCodes;
import com.homeschool.platform.module.notice.vo.NoticeOptionsVO;
import com.homeschool.platform.module.organization.entity.Grade;
import com.homeschool.platform.module.organization.entity.SchoolClass;
import com.homeschool.platform.module.organization.entity.Teacher;
import com.homeschool.platform.module.organization.entity.TeacherClass;
import com.homeschool.platform.module.organization.mapper.GradeMapper;
import com.homeschool.platform.module.organization.mapper.SchoolClassMapper;
import com.homeschool.platform.module.organization.mapper.TeacherClassMapper;
import com.homeschool.platform.module.organization.mapper.TeacherMapper;
import com.homeschool.platform.module.organization.vo.EntityOptionVO;
import com.homeschool.platform.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class NoticeOptionService {

    private final GradeMapper gradeMapper;
    private final SchoolClassMapper schoolClassMapper;
    private final TeacherMapper teacherMapper;
    private final TeacherClassMapper teacherClassMapper;

    public NoticeOptionService(
            GradeMapper gradeMapper,
            SchoolClassMapper schoolClassMapper,
            TeacherMapper teacherMapper,
            TeacherClassMapper teacherClassMapper) {
        this.gradeMapper = gradeMapper;
        this.schoolClassMapper = schoolClassMapper;
        this.teacherMapper = teacherMapper;
        this.teacherClassMapper = teacherClassMapper;
    }

    public NoticeOptionsVO options(UserPrincipal principal) {
        List<SchoolClass> classes;
        if (principal.getRoles().contains(RoleCodes.ADMIN)) {
            classes = schoolClassMapper.selectList(
                    Wrappers.<SchoolClass>lambdaQuery()
                            .eq(SchoolClass::getStatus, "ENABLED")
                            .orderByAsc(SchoolClass::getGradeId)
                            .orderByAsc(SchoolClass::getClassName));
        } else if (principal.getRoles().contains(RoleCodes.TEACHER)) {
            Teacher teacher = teacherMapper.selectOne(
                    Wrappers.<Teacher>lambdaQuery()
                            .eq(Teacher::getUserId, principal.getUserId()));
            Set<Long> classIds = new LinkedHashSet<>();
            if (teacher != null) {
                teacherClassMapper.selectList(
                                Wrappers.<TeacherClass>lambdaQuery()
                                        .eq(TeacherClass::getTeacherId, teacher.getId())
                                        .eq(TeacherClass::getStatus, "ACTIVE"))
                        .forEach(item -> classIds.add(item.getClassId()));
            }
            classes = classIds.isEmpty()
                    ? List.of()
                    : schoolClassMapper.selectList(
                            Wrappers.<SchoolClass>lambdaQuery()
                                    .in(SchoolClass::getId, classIds)
                                    .eq(SchoolClass::getStatus, "ENABLED"));
        } else {
            classes = List.of();
        }

        Set<Long> gradeIds = new LinkedHashSet<>();
        classes.forEach(item -> gradeIds.add(item.getGradeId()));
        List<Grade> grades = gradeIds.isEmpty()
                ? List.of()
                : gradeMapper.selectList(
                        Wrappers.<Grade>lambdaQuery()
                                .in(Grade::getId, gradeIds)
                                .eq(Grade::getStatus, "ENABLED")
                                .orderByAsc(Grade::getSortOrder));
        var gradeMap = grades.stream().collect(
                java.util.stream.Collectors.toMap(Grade::getId, Grade::getGradeName));

        List<EntityOptionVO> gradeOptions = grades.stream()
                .map(grade -> new EntityOptionVO(
                        grade.getId(),
                        grade.getGradeName(),
                        String.valueOf(grade.getGradeLevel())))
                .toList();
        List<EntityOptionVO> classOptions = classes.stream()
                .map(item -> new EntityOptionVO(
                        item.getId(),
                        gradeMap.getOrDefault(item.getGradeId(), "") + item.getClassName(),
                        item.getAcademicYear()))
                .toList();
        return new NoticeOptionsVO(gradeOptions, classOptions);
    }
}

