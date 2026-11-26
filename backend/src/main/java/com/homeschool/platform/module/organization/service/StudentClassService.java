package com.homeschool.platform.module.organization.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homeschool.platform.common.api.PageResponse;
import com.homeschool.platform.common.exception.BusinessException;
import com.homeschool.platform.module.organization.dto.StudentClassQuery;
import com.homeschool.platform.module.organization.dto.StudentClassRequest;
import com.homeschool.platform.module.organization.entity.SchoolClass;
import com.homeschool.platform.module.organization.entity.Student;
import com.homeschool.platform.module.organization.entity.StudentClass;
import com.homeschool.platform.module.organization.mapper.OrganizationQueryMapper;
import com.homeschool.platform.module.organization.mapper.SchoolClassMapper;
import com.homeschool.platform.module.organization.mapper.StudentClassMapper;
import com.homeschool.platform.module.organization.mapper.StudentMapper;
import com.homeschool.platform.module.organization.vo.StudentClassVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StudentClassService {

    private final StudentClassMapper studentClassMapper;
    private final StudentMapper studentMapper;
    private final SchoolClassMapper schoolClassMapper;
    private final OrganizationQueryMapper queryMapper;

    public StudentClassService(
            StudentClassMapper studentClassMapper,
            StudentMapper studentMapper,
            SchoolClassMapper schoolClassMapper,
            OrganizationQueryMapper queryMapper) {
        this.studentClassMapper = studentClassMapper;
        this.studentMapper = studentMapper;
        this.schoolClassMapper = schoolClassMapper;
        this.queryMapper = queryMapper;
    }

    public PageResponse<StudentClassVO> page(StudentClassQuery query) {
        validateStatus(query.getStatus());
        query.setKeyword(trimToNull(query.getKeyword()));
        return PageResponse.from(queryMapper.selectStudentClassPage(
                new Page<>(query.normalizedPage(), query.normalizedSize()),
                query));
    }

    @Transactional
    public Long assignOrTransfer(StudentClassRequest request) {
        Student student = studentMapper.selectByIdForUpdate(request.studentId());
        if (student == null || !"ACTIVE".equals(student.getStatus())) {
            throw new BusinessException(404, "学生不存在或已停用");
        }
        SchoolClass targetClass = schoolClassMapper.selectById(request.classId());
        if (targetClass == null || !"ENABLED".equals(targetClass.getStatus())) {
            throw new BusinessException(404, "目标班级不存在或已停用");
        }

        StudentClass current = studentClassMapper.selectOne(
                Wrappers.<StudentClass>lambdaQuery()
                        .eq(StudentClass::getStudentId, request.studentId())
                        .eq(StudentClass::getStatus, "CURRENT")
                        .last("LIMIT 1"));
        if (current != null) {
            if (current.getClassId().equals(request.classId())) {
                throw new BusinessException(409, "学生已经在该班级");
            }
            if (!request.startDate().isAfter(current.getStartDate())) {
                throw new BusinessException("调班开始日期必须晚于当前入班日期");
            }
            current.setStatus("ENDED");
            current.setEndDate(request.startDate().minusDays(1));
            studentClassMapper.updateById(current);
        }

        StudentClass history = new StudentClass();
        history.setStudentId(request.studentId());
        history.setClassId(request.classId());
        history.setStartDate(request.startDate());
        history.setStatus("CURRENT");
        studentClassMapper.insert(history);
        return history.getId();
    }

    private void validateStatus(String status) {
        if (StringUtils.hasText(status)
                && !"CURRENT".equals(status)
                && !"ENDED".equals(status)) {
            throw new BusinessException("学生班级状态筛选值不正确");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

