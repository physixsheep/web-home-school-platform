package com.homeschool.platform.module.organization.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homeschool.platform.common.api.PageResponse;
import com.homeschool.platform.common.exception.BusinessException;
import com.homeschool.platform.module.organization.dto.GradeQuery;
import com.homeschool.platform.module.organization.dto.GradeRequest;
import com.homeschool.platform.module.organization.entity.Grade;
import com.homeschool.platform.module.organization.entity.SchoolClass;
import com.homeschool.platform.module.organization.mapper.GradeMapper;
import com.homeschool.platform.module.organization.mapper.SchoolClassMapper;
import com.homeschool.platform.module.organization.vo.GradeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class GradeService {

    private final GradeMapper gradeMapper;
    private final SchoolClassMapper schoolClassMapper;

    public GradeService(GradeMapper gradeMapper, SchoolClassMapper schoolClassMapper) {
        this.gradeMapper = gradeMapper;
        this.schoolClassMapper = schoolClassMapper;
    }

    public PageResponse<GradeVO> page(GradeQuery query) {
        validateStatus(query.getStatus());
        var wrapper = Wrappers.<Grade>lambdaQuery()
                .like(StringUtils.hasText(query.getKeyword()),
                        Grade::getGradeName,
                        query.getKeyword() == null ? null : query.getKeyword().trim())
                .eq(StringUtils.hasText(query.getStatus()), Grade::getStatus, query.getStatus())
                .orderByAsc(Grade::getSortOrder)
                .orderByAsc(Grade::getId);
        IPage<Grade> source = gradeMapper.selectPage(
                new Page<>(query.normalizedPage(), query.normalizedSize()),
                wrapper);
        Page<GradeVO> target = new Page<>(
                source.getCurrent(),
                source.getSize(),
                source.getTotal());
        target.setRecords(source.getRecords().stream().map(this::toVO).toList());
        return PageResponse.from(target);
    }

    @Transactional
    public Long create(GradeRequest request) {
        Grade grade = new Grade();
        apply(grade, request);
        gradeMapper.insert(grade);
        return grade.getId();
    }

    @Transactional
    public void update(Long gradeId, GradeRequest request) {
        Grade grade = requireGrade(gradeId);
        apply(grade, request);
        gradeMapper.updateById(grade);
    }

    @Transactional
    public void delete(Long gradeId) {
        requireGrade(gradeId);
        long classCount = schoolClassMapper.selectCount(
                Wrappers.<SchoolClass>lambdaQuery().eq(SchoolClass::getGradeId, gradeId));
        if (classCount > 0) {
            throw new BusinessException(409, "该年级仍有班级，不能删除");
        }
        gradeMapper.deleteById(gradeId);
    }

    private void apply(Grade grade, GradeRequest request) {
        grade.setGradeName(request.gradeName().trim());
        grade.setGradeLevel(request.gradeLevel());
        grade.setSortOrder(request.sortOrder());
        grade.setStatus(StringUtils.hasText(request.status()) ? request.status() : "ENABLED");
    }

    private Grade requireGrade(Long gradeId) {
        Grade grade = gradeMapper.selectById(gradeId);
        if (grade == null) {
            throw new BusinessException(404, "年级不存在");
        }
        return grade;
    }

    private void validateStatus(String status) {
        if (StringUtils.hasText(status)
                && !"ENABLED".equals(status)
                && !"DISABLED".equals(status)) {
            throw new BusinessException("年级状态值不正确");
        }
    }

    private GradeVO toVO(Grade grade) {
        return new GradeVO(
                grade.getId(),
                grade.getGradeName(),
                grade.getGradeLevel(),
                grade.getSortOrder(),
                grade.getStatus(),
                grade.getCreatedAt());
    }
}

