package com.homeschool.platform.module.notice.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homeschool.platform.common.api.PageResponse;
import com.homeschool.platform.common.constant.RoleCodes;
import com.homeschool.platform.common.exception.BusinessException;
import com.homeschool.platform.module.file.FileAttachmentService;
import com.homeschool.platform.module.file.entity.FileAttachment;
import com.homeschool.platform.module.file.mapper.FileAttachmentMapper;
import com.homeschool.platform.module.notice.dto.NoticeQuery;
import com.homeschool.platform.module.notice.dto.NoticeSaveRequest;
import com.homeschool.platform.module.notice.entity.Notice;
import com.homeschool.platform.module.notice.mapper.NoticeMapper;
import com.homeschool.platform.module.notice.mapper.NoticeQueryMapper;
import com.homeschool.platform.module.notice.mapper.NoticeReadMapper;
import com.homeschool.platform.module.notice.vo.NoticeOptionsVO;
import com.homeschool.platform.module.notice.vo.NoticeReaderVO;
import com.homeschool.platform.module.notice.vo.NoticeVO;
import com.homeschool.platform.module.organization.entity.Grade;
import com.homeschool.platform.module.organization.entity.SchoolClass;
import com.homeschool.platform.module.organization.entity.Teacher;
import com.homeschool.platform.module.organization.entity.TeacherClass;
import com.homeschool.platform.module.organization.mapper.GradeMapper;
import com.homeschool.platform.module.organization.mapper.SchoolClassMapper;
import com.homeschool.platform.module.organization.mapper.TeacherClassMapper;
import com.homeschool.platform.module.organization.mapper.TeacherMapper;
import com.homeschool.platform.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class NoticeService {

    private final NoticeMapper noticeMapper;
    private final NoticeReadMapper noticeReadMapper;
    private final NoticeQueryMapper noticeQueryMapper;
    private final NoticeAccessService accessService;
    private final NoticeOptionService optionService;
    private final FileAttachmentMapper attachmentMapper;
    private final FileAttachmentService attachmentService;
    private final TeacherMapper teacherMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final GradeMapper gradeMapper;
    private final SchoolClassMapper schoolClassMapper;

    public NoticeService(
            NoticeMapper noticeMapper,
            NoticeReadMapper noticeReadMapper,
            NoticeQueryMapper noticeQueryMapper,
            NoticeAccessService accessService,
            NoticeOptionService optionService,
            FileAttachmentMapper attachmentMapper,
            FileAttachmentService attachmentService,
            TeacherMapper teacherMapper,
            TeacherClassMapper teacherClassMapper,
            GradeMapper gradeMapper,
            SchoolClassMapper schoolClassMapper) {
        this.noticeMapper = noticeMapper;
        this.noticeReadMapper = noticeReadMapper;
        this.noticeQueryMapper = noticeQueryMapper;
        this.accessService = accessService;
        this.optionService = optionService;
        this.attachmentMapper = attachmentMapper;
        this.attachmentService = attachmentService;
        this.teacherMapper = teacherMapper;
        this.teacherClassMapper = teacherClassMapper;
        this.gradeMapper = gradeMapper;
        this.schoolClassMapper = schoolClassMapper;
    }

    public PageResponse<NoticeVO> readablePage(NoticeQuery query, UserPrincipal principal) {
        query.setKeyword(trimToNull(query.getKeyword()));
        NoticeAccessScope scope = accessService.resolveScope(
                principal.getUserId(),
                principal.getRoles());
        var result = noticeQueryMapper.selectAccessiblePage(
                new Page<>(query.normalizedPage(), query.normalizedSize()),
                query,
                scope,
                principal.getUserId());
        result.getRecords().forEach(this::attachFiles);
        return PageResponse.from(result);
    }

    public PageResponse<NoticeVO> managePage(NoticeQuery query, UserPrincipal principal) {
        validateManageQuery(query);
        query.setKeyword(trimToNull(query.getKeyword()));
        boolean admin = principal.getRoles().contains(RoleCodes.ADMIN);
        var result = noticeQueryMapper.selectManagePage(
                new Page<>(query.normalizedPage(), query.normalizedSize()),
                query,
                principal.getUserId(),
                admin);
        result.getRecords().forEach(this::attachFiles);
        return PageResponse.from(result);
    }

    public NoticeVO detail(Long noticeId, UserPrincipal principal) {
        Notice notice = requireNotice(noticeId);
        NoticeAccessScope scope = accessService.resolveScope(
                principal.getUserId(),
                principal.getRoles());
        accessService.requireAccess(
                notice,
                principal.getUserId(),
                principal.getRoles(),
                scope);
        NoticeVO result = noticeQueryMapper.selectDetail(noticeId, principal.getUserId());
        if (result == null) {
            throw new BusinessException(404, "通知不存在");
        }
        attachFiles(result);
        return result;
    }

    @Transactional
    public Long create(NoticeSaveRequest request, UserPrincipal principal) {
        validatePublisher(principal);
        Notice notice = new Notice();
        notice.setPublisherId(principal.getUserId());
        notice.setStatus("DRAFT");
        applyNotice(notice, request, principal);
        noticeMapper.insert(notice);
        syncAttachments(notice.getId(), principal.getUserId(), request.attachmentIds());
        return notice.getId();
    }

    @Transactional
    public void update(Long noticeId, NoticeSaveRequest request, UserPrincipal principal) {
        Notice notice = requireNotice(noticeId);
        requireManage(notice, principal);
        if ("WITHDRAWN".equals(notice.getStatus())) {
            throw new BusinessException(409, "已撤回通知不能编辑");
        }
        applyNotice(notice, request, principal);
        noticeMapper.updateById(notice);
        syncAttachments(notice.getId(), principal.getUserId(), request.attachmentIds());
    }

    @Transactional
    public void publish(Long noticeId, UserPrincipal principal) {
        Notice notice = requireNotice(noticeId);
        requireManage(notice, principal);
        if (!"DRAFT".equals(notice.getStatus())) {
            throw new BusinessException(409, "只有草稿通知可以发布");
        }
        validateScope(notice.getScopeType(), notice.getGradeId(), notice.getClassId(), principal);
        notice.setStatus("PUBLISHED");
        notice.setPublishedAt(LocalDateTime.now());
        noticeMapper.updateById(notice);
    }

    @Transactional
    public void withdraw(Long noticeId, UserPrincipal principal) {
        Notice notice = requireNotice(noticeId);
        requireManage(notice, principal);
        if (!"PUBLISHED".equals(notice.getStatus())) {
            throw new BusinessException(409, "只有已发布通知可以撤回");
        }
        notice.setStatus("WITHDRAWN");
        notice.setWithdrawnAt(LocalDateTime.now());
        noticeMapper.updateById(notice);
    }

    @Transactional
    public void deleteDraft(Long noticeId, UserPrincipal principal) {
        Notice notice = requireNotice(noticeId);
        requireManage(notice, principal);
        if (!"DRAFT".equals(notice.getStatus())) {
            throw new BusinessException(409, "只有草稿通知可以删除");
        }
        syncAttachments(noticeId, principal.getUserId(), List.of());
        noticeMapper.deleteById(noticeId);
    }

    @Transactional
    public void markRead(Long noticeId, UserPrincipal principal) {
        Notice notice = requireNotice(noticeId);
        NoticeAccessScope scope = accessService.resolveScope(
                principal.getUserId(),
                principal.getRoles());
        accessService.requireAccess(
                notice,
                principal.getUserId(),
                principal.getRoles(),
                scope);
        if (!"PUBLISHED".equals(notice.getStatus())) {
            throw new BusinessException(409, "通知尚未发布或已撤回");
        }
        noticeReadMapper.insertIgnore(noticeId, principal.getUserId());
    }

    public PageResponse<NoticeReaderVO> readers(
            Long noticeId,
            long page,
            long size,
            UserPrincipal principal) {
        Notice notice = requireNotice(noticeId);
        requireManage(notice, principal);
        var result = noticeQueryMapper.selectReaders(
                new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size))),
                noticeId);
        return PageResponse.from(result);
    }

    public NoticeOptionsVO options(UserPrincipal principal) {
        validatePublisher(principal);
        return optionService.options(principal);
    }

    private void applyNotice(
            Notice notice,
            NoticeSaveRequest request,
            UserPrincipal principal) {
        validateScope(
                request.scopeType(),
                request.gradeId(),
                request.classId(),
                principal);
        notice.setTitle(request.title().trim());
        notice.setContent(request.content().trim());
        notice.setScopeType(request.scopeType());
        notice.setGradeId(request.gradeId());
        notice.setClassId(request.classId());
        if ("CLASS".equals(request.scopeType())) {
            SchoolClass schoolClass = schoolClassMapper.selectById(request.classId());
            if (schoolClass == null) {
                throw new BusinessException(404, "通知班级不存在");
            }
            notice.setGradeId(schoolClass.getGradeId());
        }
    }

    private void validateScope(
            String scopeType,
            Long gradeId,
            Long classId,
            UserPrincipal principal) {
        boolean admin = principal.getRoles().contains(RoleCodes.ADMIN);
        if (!admin && !"CLASS".equals(scopeType)) {
            throw new BusinessException(403, "教师只能发布本班班级通知");
        }
        switch (scopeType) {
            case "ALL" -> {
                if (gradeId != null || classId != null) {
                    throw new BusinessException("全校通知不能指定年级或班级");
                }
            }
            case "GRADE" -> {
                if (gradeId == null || classId != null) {
                    throw new BusinessException("年级通知必须且只能指定年级");
                }
                Grade grade = gradeMapper.selectById(gradeId);
                if (grade == null || !"ENABLED".equals(grade.getStatus())) {
                    throw new BusinessException(404, "通知年级不存在或已停用");
                }
            }
            case "CLASS" -> {
                if (classId == null) {
                    throw new BusinessException("班级通知必须指定班级");
                }
                SchoolClass schoolClass = schoolClassMapper.selectById(classId);
                if (schoolClass == null || !"ENABLED".equals(schoolClass.getStatus())) {
                    throw new BusinessException(404, "通知班级不存在或已停用");
                }
                if (!admin) {
                    Teacher teacher = teacherMapper.selectOne(
                            Wrappers.<Teacher>lambdaQuery()
                                    .eq(Teacher::getUserId, principal.getUserId()));
                    if (teacher == null) {
                        throw new BusinessException(403, "教师档案不存在");
                    }
                    long relationCount = teacherClassMapper.selectCount(
                            Wrappers.<TeacherClass>lambdaQuery()
                                    .eq(TeacherClass::getTeacherId, teacher.getId())
                                    .eq(TeacherClass::getClassId, classId)
                                    .eq(TeacherClass::getStatus, "ACTIVE"));
                    if (relationCount == 0) {
                        throw new BusinessException(403, "只能发布本人任教班级的通知");
                    }
                }
            }
            default -> throw new BusinessException("通知范围不正确");
        }
    }

    private void syncAttachments(Long noticeId, Long uploaderId, List<Long> requestedIds) {
        Set<Long> targetIds = requestedIds == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(requestedIds);
        List<FileAttachment> existing = attachmentMapper.selectList(
                Wrappers.<FileAttachment>lambdaQuery()
                        .eq(FileAttachment::getBizType, "NOTICE")
                        .eq(FileAttachment::getBizId, noticeId)
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
            boolean tempOwned = "TEMP".equals(attachment.getStatus())
                    && attachment.getUploaderUserId().equals(uploaderId);
            boolean alreadyBound = "BOUND".equals(attachment.getStatus())
                    && "NOTICE".equals(attachment.getBizType())
                    && noticeId.equals(attachment.getBizId());
            if (!tempOwned && !alreadyBound) {
                throw new BusinessException(403, "附件无权绑定到当前通知");
            }
            attachment.setBizType("NOTICE");
            attachment.setBizId(noticeId);
            attachment.setStatus("BOUND");
            attachmentMapper.updateById(attachment);
        }
    }

    private void attachFiles(NoticeVO notice) {
        if (notice != null) {
            notice.setAttachments(attachmentService.listForNotice(notice.getId()));
        }
    }

    private Notice requireNotice(Long noticeId) {
        Notice notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new BusinessException(404, "通知不存在");
        }
        return notice;
    }

    private void requireManage(Notice notice, UserPrincipal principal) {
        if (!accessService.canManage(
                notice,
                principal.getUserId(),
                principal.getRoles())) {
            throw new BusinessException(403, "只能管理本人发布的通知");
        }
    }

    private void validatePublisher(UserPrincipal principal) {
        if (!principal.getRoles().contains(RoleCodes.ADMIN)
                && !principal.getRoles().contains(RoleCodes.TEACHER)) {
            throw new BusinessException(403, "当前角色不能发布通知");
        }
    }

    private void validateManageQuery(NoticeQuery query) {
        if (StringUtils.hasText(query.getStatus())
                && !Set.of("DRAFT", "PUBLISHED", "WITHDRAWN").contains(query.getStatus())) {
            throw new BusinessException("通知状态筛选值不正确");
        }
        if (StringUtils.hasText(query.getScopeType())
                && !Set.of("ALL", "GRADE", "CLASS").contains(query.getScopeType())) {
            throw new BusinessException("通知范围筛选值不正确");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

