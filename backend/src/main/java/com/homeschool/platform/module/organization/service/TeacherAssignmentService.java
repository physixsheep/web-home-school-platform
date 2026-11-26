package com.homeschool.platform.module.organization.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homeschool.platform.common.api.PageResponse;
import com.homeschool.platform.common.exception.BusinessException;
import com.homeschool.platform.module.organization.dto.TeacherAssignmentQuery;
import com.homeschool.platform.module.organization.dto.TeacherAssignmentRequest;
import com.homeschool.platform.module.organization.entity.SchoolClass;
import com.homeschool.platform.module.organization.entity.Subject;
import com.homeschool.platform.module.organization.entity.Teacher;
import com.homeschool.platform.module.organization.entity.TeacherClass;
import com.homeschool.platform.module.organization.mapper.OrganizationQueryMapper;
import com.homeschool.platform.module.organization.mapper.SchoolClassMapper;
import com.homeschool.platform.module.organization.mapper.SubjectMapper;
import com.homeschool.platform.module.organization.mapper.TeacherClassMapper;
import com.homeschool.platform.module.organization.mapper.TeacherMapper;
import com.homeschool.platform.module.organization.vo.TeacherAssignmentVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
public class TeacherAssignmentService {

    private final TeacherClassMapper teacherClassMapper;
    private final TeacherMapper teacherMapper;
    private final SchoolClassMapper schoolClassMapper;
    private final SubjectMapper subjectMapper;
    private final OrganizationQueryMapper queryMapper;

    public TeacherAssignmentService(
            TeacherClassMapper teacherClassMapper,
            TeacherMapper teacherMapper,
            SchoolClassMapper schoolClassMapper,
            SubjectMapper subjectMapper,
            OrganizationQueryMapper queryMapper) {
        this.teacherClassMapper = teacherClassMapper;
        this.teacherMapper = teacherMapper;
        this.schoolClassMapper = schoolClassMapper;
        this.subjectMapper = subjectMapper;
        this.queryMapper = queryMapper;
    }

    public PageResponse<TeacherAssignmentVO> page(TeacherAssignmentQuery query) {
        validateQuery(query);
        query.setKeyword(trimToNull(query.getKeyword()));
        return PageResponse.from(queryMapper.selectTeacherAssignmentPage(
                new Page<>(query.normalizedPage(), query.normalizedSize()),
                query));
    }

    @Transactional
    public Long create(TeacherAssignmentRequest request) {
        Teacher teacher = teacherMapper.selectById(request.teacherId());
        if (teacher == null || !"ACTIVE".equals(teacher.getStatus())) {
            throw new BusinessException(404, "教师不存在或已停用");
        }
        SchoolClass schoolClass = schoolClassMapper.selectByIdForUpdate(request.classId());
        if (schoolClass == null) {
            throw new BusinessException(404, "班级不存在");
        }
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BusinessException("结束日期不能早于开始日期");
        }

        if ("HOMEROOM".equals(request.relationType())) {
            if (request.subjectId() != null) {
                throw new BusinessException("班主任关联不能设置学科");
            }
            long homeroomCount = teacherClassMapper.selectCount(
                    Wrappers.<TeacherClass>lambdaQuery()
                            .eq(TeacherClass::getClassId, request.classId())
                            .eq(TeacherClass::getRelationType, "HOMEROOM")
                            .eq(TeacherClass::getStatus, "ACTIVE"));
            if (homeroomCount > 0) {
                throw new BusinessException(409, "该班级已有在任班主任");
            }
        } else {
            if (request.subjectId() == null) {
                throw new BusinessException("任课教师关联必须选择学科");
            }
            Subject subject = subjectMapper.selectById(request.subjectId());
            if (subject == null || !"ENABLED".equals(subject.getStatus())) {
                throw new BusinessException(404, "学科不存在或已停用");
            }
            long duplicateCount = teacherClassMapper.selectCount(
                    Wrappers.<TeacherClass>lambdaQuery()
                            .eq(TeacherClass::getTeacherId, request.teacherId())
                            .eq(TeacherClass::getClassId, request.classId())
                            .eq(TeacherClass::getSubjectId, request.subjectId())
                            .eq(TeacherClass::getRelationType, "SUBJECT")
                            .eq(TeacherClass::getStatus, "ACTIVE"));
            if (duplicateCount > 0) {
                throw new BusinessException(409, "该教师已任教此班级的该学科");
            }
        }

        TeacherClass assignment = new TeacherClass();
        assignment.setTeacherId(request.teacherId());
        assignment.setClassId(request.classId());
        assignment.setSubjectId(request.subjectId());
        assignment.setRelationType(request.relationType());
        assignment.setStartDate(request.startDate());
        assignment.setEndDate(request.endDate());
        assignment.setStatus("ACTIVE");
        teacherClassMapper.insert(assignment);
        return assignment.getId();
    }

    @Transactional
    public void end(Long assignmentId) {
        TeacherClass assignment = teacherClassMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException(404, "教师班级关联不存在");
        }
        if (!"ACTIVE".equals(assignment.getStatus())) {
            throw new BusinessException(409, "该关联已经结束");
        }
        assignment.setStatus("ENDED");
        assignment.setEndDate(LocalDate.now());
        teacherClassMapper.updateById(assignment);
    }

    private void validateQuery(TeacherAssignmentQuery query) {
        if (StringUtils.hasText(query.getRelationType())
                && !"HOMEROOM".equals(query.getRelationType())
                && !"SUBJECT".equals(query.getRelationType())) {
            throw new BusinessException("关联类型筛选值不正确");
        }
        if (StringUtils.hasText(query.getStatus())
                && !"ACTIVE".equals(query.getStatus())
                && !"ENDED".equals(query.getStatus())) {
            throw new BusinessException("关联状态筛选值不正确");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

