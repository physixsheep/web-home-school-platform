package com.homeschool.platform.module.organization.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homeschool.platform.common.api.PageResponse;
import com.homeschool.platform.common.exception.BusinessException;
import com.homeschool.platform.module.organization.dto.SchoolClassQuery;
import com.homeschool.platform.module.organization.dto.SchoolClassRequest;
import com.homeschool.platform.module.organization.entity.Grade;
import com.homeschool.platform.module.organization.entity.SchoolClass;
import com.homeschool.platform.module.organization.entity.StudentClass;
import com.homeschool.platform.module.organization.entity.TeacherClass;
import com.homeschool.platform.module.organization.mapper.GradeMapper;
import com.homeschool.platform.module.organization.mapper.OrganizationQueryMapper;
import com.homeschool.platform.module.organization.mapper.SchoolClassMapper;
import com.homeschool.platform.module.organization.mapper.StudentClassMapper;
import com.homeschool.platform.module.organization.mapper.TeacherClassMapper;
import com.homeschool.platform.module.organization.vo.SchoolClassVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SchoolClassService {

    private final SchoolClassMapper schoolClassMapper;
    private final GradeMapper gradeMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final StudentClassMapper studentClassMapper;
    private final OrganizationQueryMapper queryMapper;

    public SchoolClassService(
            SchoolClassMapper schoolClassMapper,
            GradeMapper gradeMapper,
            TeacherClassMapper teacherClassMapper,
            StudentClassMapper studentClassMapper,
            OrganizationQueryMapper queryMapper) {
        this.schoolClassMapper = schoolClassMapper;
        this.gradeMapper = gradeMapper;
        this.teacherClassMapper = teacherClassMapper;
        this.studentClassMapper = studentClassMapper;
        this.queryMapper = queryMapper;
    }

    public PageResponse<SchoolClassVO> page(SchoolClassQuery query) {
        validateStatus(query.getStatus());
        query.setKeyword(trimToNull(query.getKeyword()));
        query.setAcademicYear(trimToNull(query.getAcademicYear()));
        return PageResponse.from(queryMapper.selectClassPage(
                new Page<>(query.normalizedPage(), query.normalizedSize()),
                query));
    }

    @Transactional
    public Long create(SchoolClassRequest request) {
        requireGrade(request.gradeId());
        SchoolClass schoolClass = new SchoolClass();
        apply(schoolClass, request);
        schoolClassMapper.insert(schoolClass);
        return schoolClass.getId();
    }

    @Transactional
    public void update(Long classId, SchoolClassRequest request) {
        SchoolClass schoolClass = requireClass(classId);
        requireGrade(request.gradeId());
        apply(schoolClass, request);
        schoolClassMapper.updateById(schoolClass);
    }

    @Transactional
    public void delete(Long classId) {
        requireClass(classId);
        long teacherCount = teacherClassMapper.selectCount(
                Wrappers.<TeacherClass>lambdaQuery().eq(TeacherClass::getClassId, classId));
        long studentCount = studentClassMapper.selectCount(
                Wrappers.<StudentClass>lambdaQuery().eq(StudentClass::getClassId, classId));
        if (teacherCount > 0 || studentCount > 0) {
            throw new BusinessException(409, "该班级仍有关联教师或学生履历，不能删除");
        }
        schoolClassMapper.deleteById(classId);
    }

    private void apply(SchoolClass schoolClass, SchoolClassRequest request) {
        schoolClass.setGradeId(request.gradeId());
        schoolClass.setClassName(request.className().trim());
        schoolClass.setAcademicYear(request.academicYear().trim());
        schoolClass.setGraduationYear(request.graduationYear());
        schoolClass.setStatus(StringUtils.hasText(request.status()) ? request.status() : "ENABLED");
    }

    private Grade requireGrade(Long gradeId) {
        Grade grade = gradeMapper.selectById(gradeId);
        if (grade == null) {
            throw new BusinessException(404, "年级不存在");
        }
        return grade;
    }

    private SchoolClass requireClass(Long classId) {
        SchoolClass schoolClass = schoolClassMapper.selectById(classId);
        if (schoolClass == null) {
            throw new BusinessException(404, "班级不存在");
        }
        return schoolClass;
    }

    private void validateStatus(String status) {
        if (StringUtils.hasText(status)
                && !"ENABLED".equals(status)
                && !"DISABLED".equals(status)) {
            throw new BusinessException("班级状态值不正确");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

