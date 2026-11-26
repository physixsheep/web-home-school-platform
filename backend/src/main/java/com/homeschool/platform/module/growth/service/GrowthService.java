package com.homeschool.platform.module.growth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homeschool.platform.common.api.PageResponse;
import com.homeschool.platform.common.constant.RoleCodes;
import com.homeschool.platform.common.exception.BusinessException;
import com.homeschool.platform.module.file.FileAttachmentService;
import com.homeschool.platform.module.file.entity.FileAttachment;
import com.homeschool.platform.module.file.mapper.FileAttachmentMapper;
import com.homeschool.platform.module.growth.dto.GrowthQuery;
import com.homeschool.platform.module.growth.dto.GrowthSaveRequest;
import com.homeschool.platform.module.growth.entity.GrowthRecord;
import com.homeschool.platform.module.growth.mapper.GrowthQueryMapper;
import com.homeschool.platform.module.growth.mapper.GrowthRecordMapper;
import com.homeschool.platform.module.growth.vo.GrowthVO;
import com.homeschool.platform.module.organization.entity.Student;
import com.homeschool.platform.module.organization.entity.StudentClass;
import com.homeschool.platform.module.organization.entity.Teacher;
import com.homeschool.platform.module.organization.vo.EntityOptionVO;
import com.homeschool.platform.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class GrowthService {

    private final GrowthRecordMapper growthMapper;
    private final GrowthQueryMapper queryMapper;
    private final GrowthAccessService accessService;
    private final FileAttachmentMapper attachmentMapper;
    private final FileAttachmentService attachmentService;

    public GrowthService(
            GrowthRecordMapper growthMapper,
            GrowthQueryMapper queryMapper,
            GrowthAccessService accessService,
            FileAttachmentMapper attachmentMapper,
            FileAttachmentService attachmentService) {
        this.growthMapper = growthMapper;
        this.queryMapper = queryMapper;
        this.accessService = accessService;
        this.attachmentMapper = attachmentMapper;
        this.attachmentService = attachmentService;
    }

    public PageResponse<GrowthVO> readablePage(
            GrowthQuery query,
            UserPrincipal principal) {
        validateQuery(query);
        boolean admin = principal.getRoles().contains(RoleCodes.ADMIN);
        boolean parent = principal.getRoles().contains(RoleCodes.PARENT);
        Long studentId = null;
        if (!admin) {
            if (principal.getRoles().contains(RoleCodes.STUDENT)) {
                studentId = accessService.requireStudent(principal).getId();
            } else if (parent) {
                studentId = accessService.resolveParentStudentId(
                        principal,
                        query.getStudentId());
            } else {
                throw new BusinessException(403, "当前角色不能使用成长档案阅读列表");
            }
        }
        var result = queryMapper.selectReadablePage(
                new Page<>(query.normalizedPage(), query.normalizedSize()),
                query,
                studentId,
                admin,
                parent);
        result.getRecords().forEach(this::attachFiles);
        return PageResponse.from(result);
    }

    public PageResponse<GrowthVO> managePage(
            GrowthQuery query,
            UserPrincipal principal) {
        validateQuery(query);
        List<Long> classIds = accessService.homeroomClassIds(principal);
        if (classIds.isEmpty()) {
            return new PageResponse<>(List.of(), 0, query.normalizedPage(), query.normalizedSize());
        }
        var result = queryMapper.selectManagePage(
                new Page<>(query.normalizedPage(), query.normalizedSize()),
                query,
                classIds);
        result.getRecords().forEach(this::attachFiles);
        return PageResponse.from(result);
    }

    public GrowthVO detail(Long recordId, UserPrincipal principal) {
        GrowthRecord record = requireRecord(recordId);
        accessService.requireAccess(record, principal);
        GrowthVO result = queryMapper.selectDetail(recordId);
        if (result == null) {
            throw new BusinessException(404, "成长记录不存在");
        }
        attachFiles(result);
        return result;
    }

    public List<EntityOptionVO> studentOptions(UserPrincipal principal) {
        boolean admin = principal.getRoles().contains(RoleCodes.ADMIN);
        List<Long> classIds = admin
                ? List.of()
                : accessService.homeroomClassIds(principal);
        if (!admin && classIds.isEmpty()) {
            return List.of();
        }
        return queryMapper.selectStudentOptions(classIds, admin);
    }

    @Transactional
    public Long create(GrowthSaveRequest request, UserPrincipal principal) {
        Teacher teacher = accessService.requireTeacher(principal);
        Student student = accessService.requireStudentById(request.studentId());
        accessService.requireHomeroomForStudent(principal, student.getId());
        StudentClass current = accessService.currentClass(student.getId());
        validateOccurredAt(request.occurredAt());

        GrowthRecord record = new GrowthRecord();
        record.setStudentId(student.getId());
        record.setClassId(current.getClassId());
        record.setRecorderTeacherId(teacher.getId());
        apply(record, request);
        growthMapper.insert(record);
        syncAttachments(record.getId(), principal.getUserId(), request.attachmentIds());
        return record.getId();
    }

    @Transactional
    public void update(
            Long recordId,
            GrowthSaveRequest request,
            UserPrincipal principal) {
        GrowthRecord record = requireRecord(recordId);
        if (!accessService.canManage(record, principal)) {
            throw new BusinessException(403, "只能修改本人创建的成长记录");
        }
        if (!record.getStudentId().equals(request.studentId())) {
            throw new BusinessException(409, "成长记录不能转移给其他学生");
        }
        validateOccurredAt(request.occurredAt());
        apply(record, request);
        growthMapper.updateById(record);
        syncAttachments(recordId, principal.getUserId(), request.attachmentIds());
    }

    @Transactional
    public void delete(Long recordId, UserPrincipal principal) {
        GrowthRecord record = requireRecord(recordId);
        if (!accessService.canManage(record, principal)) {
            throw new BusinessException(403, "只能删除本人创建的成长记录");
        }
        syncAttachments(recordId, principal.getUserId(), List.of());
        growthMapper.deleteById(recordId);
    }

    private void apply(GrowthRecord record, GrowthSaveRequest request) {
        record.setCategory(request.category());
        record.setTitle(request.title().trim());
        record.setContent(request.content().trim());
        record.setOccurredAt(request.occurredAt());
        record.setVisibleToParent(Boolean.FALSE.equals(request.visibleToParent()) ? 0 : 1);
    }

    private void syncAttachments(Long recordId, Long uploaderId, List<Long> requestedIds) {
        Set<Long> targetIds = requestedIds == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(requestedIds);
        List<FileAttachment> existing = attachmentMapper.selectList(
                Wrappers.<FileAttachment>lambdaQuery()
                        .eq(FileAttachment::getBizType, "GROWTH")
                        .eq(FileAttachment::getBizId, recordId)
                        .eq(FileAttachment::getStatus, "BOUND"));
        for (FileAttachment attachment : existing) {
            if (!targetIds.contains(attachment.getId())) {
                attachment.setStatus("DELETED");
                attachmentMapper.updateById(attachment);
            }
        }
        for (Long attachmentId : targetIds) {
            FileAttachment attachment = attachmentMapper.selectById(attachmentId);
            if (attachment == null) {
                throw new BusinessException(404, "附件不存在：" + attachmentId);
            }
            boolean temporaryOwned = "TEMP".equals(attachment.getStatus())
                    && attachment.getUploaderUserId().equals(uploaderId);
            boolean alreadyBound = "BOUND".equals(attachment.getStatus())
                    && "GROWTH".equals(attachment.getBizType())
                    && recordId.equals(attachment.getBizId());
            if (!temporaryOwned && !alreadyBound) {
                throw new BusinessException(403, "附件无权绑定到当前成长记录");
            }
            attachment.setBizType("GROWTH");
            attachment.setBizId(recordId);
            attachment.setStatus("BOUND");
            attachmentMapper.updateById(attachment);
        }
    }

    private void attachFiles(GrowthVO record) {
        record.setAttachments(attachmentService.listForBusiness("GROWTH", record.getId()));
    }

    private GrowthRecord requireRecord(Long recordId) {
        GrowthRecord record = growthMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(404, "成长记录不存在");
        }
        return record;
    }

    private void validateOccurredAt(LocalDate occurredAt) {
        if (occurredAt.isAfter(LocalDate.now())) {
            throw new BusinessException("成长记录日期不能晚于今天");
        }
    }

    private void validateQuery(GrowthQuery query) {
        if (StringUtils.hasText(query.getCategory())
                && !Set.of("STUDY", "MORAL", "ACTIVITY", "HONOR", "COMPREHENSIVE")
                .contains(query.getCategory())) {
            throw new BusinessException("成长记录分类筛选值不正确");
        }
        if (query.getDateFrom() != null
                && query.getDateTo() != null
                && query.getDateFrom().isAfter(query.getDateTo())) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
    }
}

