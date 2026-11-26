package com.homeschool.platform.module.organization.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homeschool.platform.common.api.PageResponse;
import com.homeschool.platform.common.exception.BusinessException;
import com.homeschool.platform.module.organization.dto.ParentBindingQuery;
import com.homeschool.platform.module.organization.dto.ParentBindingRequest;
import com.homeschool.platform.module.organization.entity.Parent;
import com.homeschool.platform.module.organization.entity.ParentStudent;
import com.homeschool.platform.module.organization.entity.Student;
import com.homeschool.platform.module.organization.mapper.OrganizationQueryMapper;
import com.homeschool.platform.module.organization.mapper.ParentMapper;
import com.homeschool.platform.module.organization.mapper.ParentStudentMapper;
import com.homeschool.platform.module.organization.mapper.StudentMapper;
import com.homeschool.platform.module.organization.vo.ParentBindingVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class ParentBindingService {

    private final ParentStudentMapper parentStudentMapper;
    private final ParentMapper parentMapper;
    private final StudentMapper studentMapper;
    private final OrganizationQueryMapper queryMapper;

    public ParentBindingService(
            ParentStudentMapper parentStudentMapper,
            ParentMapper parentMapper,
            StudentMapper studentMapper,
            OrganizationQueryMapper queryMapper) {
        this.parentStudentMapper = parentStudentMapper;
        this.parentMapper = parentMapper;
        this.studentMapper = studentMapper;
        this.queryMapper = queryMapper;
    }

    public PageResponse<ParentBindingVO> page(ParentBindingQuery query) {
        validateStatus(query.getStatus());
        query.setKeyword(trimToNull(query.getKeyword()));
        return PageResponse.from(queryMapper.selectParentBindingPage(
                new Page<>(query.normalizedPage(), query.normalizedSize()),
                query));
    }

    @Transactional
    public Long create(ParentBindingRequest request, Long operatorId) {
        Parent parent = parentMapper.selectById(request.parentId());
        if (parent == null) {
            throw new BusinessException(404, "家长档案不存在");
        }
        Student student = studentMapper.selectById(request.studentId());
        if (student == null) {
            throw new BusinessException(404, "学生档案不存在");
        }

        ParentStudent binding = parentStudentMapper.selectOne(
                Wrappers.<ParentStudent>lambdaQuery()
                        .eq(ParentStudent::getParentId, request.parentId())
                        .eq(ParentStudent::getStudentId, request.studentId()));
        if (binding != null && !"REVOKED".equals(binding.getStatus())) {
            throw new BusinessException(409, "该家长与学生已经存在有效或待审核绑定");
        }

        int primary = request.isPrimary() != null && request.isPrimary() == 1 ? 1 : 0;
        if (primary == 1) {
            clearOtherPrimary(request.studentId(), binding == null ? null : binding.getId());
        }
        if (binding == null) {
            binding = new ParentStudent();
            binding.setParentId(request.parentId());
            binding.setStudentId(request.studentId());
        }
        binding.setRelationship(request.relationship());
        binding.setIsPrimary(primary);
        binding.setStatus("ACTIVE");
        binding.setVerifiedBy(operatorId);
        binding.setVerifiedAt(LocalDateTime.now());
        if (binding.getId() == null) {
            parentStudentMapper.insert(binding);
        } else {
            parentStudentMapper.updateById(binding);
        }
        return binding.getId();
    }

    @Transactional
    public void updateStatus(Long bindingId, String status, Long operatorId) {
        ParentStudent binding = parentStudentMapper.selectById(bindingId);
        if (binding == null) {
            throw new BusinessException(404, "家长学生绑定不存在");
        }
        binding.setStatus(status);
        if ("ACTIVE".equals(status)) {
            binding.setVerifiedBy(operatorId);
            binding.setVerifiedAt(LocalDateTime.now());
        } else {
            binding.setIsPrimary(0);
        }
        parentStudentMapper.updateById(binding);
    }

    private void clearOtherPrimary(Long studentId, Long excludeId) {
        var wrapper = Wrappers.<ParentStudent>lambdaUpdate()
                .set(ParentStudent::getIsPrimary, 0)
                .eq(ParentStudent::getStudentId, studentId)
                .eq(ParentStudent::getStatus, "ACTIVE")
                .eq(ParentStudent::getIsPrimary, 1);
        if (excludeId != null) {
            wrapper.ne(ParentStudent::getId, excludeId);
        }
        parentStudentMapper.update(null, wrapper);
    }

    private void validateStatus(String status) {
        if (StringUtils.hasText(status)
                && !"PENDING".equals(status)
                && !"ACTIVE".equals(status)
                && !"REVOKED".equals(status)) {
            throw new BusinessException("绑定状态值不正确");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

