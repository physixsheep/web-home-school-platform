package com.homeschool.platform.module.leave.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homeschool.platform.common.api.PageResponse;
import com.homeschool.platform.common.constant.RoleCodes;
import com.homeschool.platform.common.exception.BusinessException;
import com.homeschool.platform.module.file.FileAttachmentService;
import com.homeschool.platform.module.file.entity.FileAttachment;
import com.homeschool.platform.module.file.mapper.FileAttachmentMapper;
import com.homeschool.platform.module.leave.dto.LeaveCreateRequest;
import com.homeschool.platform.module.leave.dto.LeaveDecisionRequest;
import com.homeschool.platform.module.leave.dto.LeaveQuery;
import com.homeschool.platform.module.leave.entity.LeaveRequest;
import com.homeschool.platform.module.leave.mapper.LeaveQueryMapper;
import com.homeschool.platform.module.leave.mapper.LeaveRequestMapper;
import com.homeschool.platform.module.leave.vo.LeaveVO;
import com.homeschool.platform.module.organization.entity.Student;
import com.homeschool.platform.module.organization.entity.StudentClass;
import com.homeschool.platform.module.organization.entity.Teacher;
import com.homeschool.platform.module.organization.mapper.StudentClassMapper;
import com.homeschool.platform.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class LeaveService {

    private final LeaveRequestMapper leaveMapper;
    private final LeaveQueryMapper queryMapper;
    private final LeaveAccessService accessService;
    private final StudentClassMapper studentClassMapper;
    private final FileAttachmentMapper attachmentMapper;
    private final FileAttachmentService attachmentService;

    public LeaveService(
            LeaveRequestMapper leaveMapper,
            LeaveQueryMapper queryMapper,
            LeaveAccessService accessService,
            StudentClassMapper studentClassMapper,
            FileAttachmentMapper attachmentMapper,
            FileAttachmentService attachmentService) {
        this.leaveMapper = leaveMapper;
        this.queryMapper = queryMapper;
        this.accessService = accessService;
        this.studentClassMapper = studentClassMapper;
        this.attachmentMapper = attachmentMapper;
        this.attachmentService = attachmentService;
    }

    public PageResponse<LeaveVO> page(LeaveQuery query, UserPrincipal principal) {
        validateStatus(query.getStatus());
        query.setKeyword(trimToNull(query.getKeyword()));
        boolean admin = principal.getRoles().contains(RoleCodes.ADMIN);
        Long studentId = null;
        if (!admin) {
            if (principal.getRoles().contains(RoleCodes.STUDENT)) {
                studentId = accessService.requireStudent(principal).getId();
            } else if (principal.getRoles().contains(RoleCodes.PARENT)) {
                studentId = accessService.resolveParentStudentId(
                        principal,
                        query.getStudentId());
            } else {
                throw new BusinessException(403, "当前角色不能使用个人请假记录列表");
            }
        }
        var result = queryMapper.selectLeavePage(
                new Page<>(query.normalizedPage(), query.normalizedSize()),
                query,
                studentId,
                admin);
        result.getRecords().forEach(this::attachFiles);
        return PageResponse.from(result);
    }

    public PageResponse<LeaveVO> approvalPage(
            LeaveQuery query,
            UserPrincipal principal) {
        validateStatus(query.getStatus());
        query.setKeyword(trimToNull(query.getKeyword()));
        List<Long> classIds = accessService.homeroomClassIds(principal);
        if (classIds.isEmpty()) {
            return new PageResponse<>(List.of(), 0, query.normalizedPage(), query.normalizedSize());
        }
        var result = queryMapper.selectApprovalPage(
                new Page<>(query.normalizedPage(), query.normalizedSize()),
                query,
                classIds);
        result.getRecords().forEach(this::attachFiles);
        return PageResponse.from(result);
    }

    public LeaveVO detail(Long leaveId, UserPrincipal principal) {
        LeaveRequest leave = requireLeave(leaveId);
        accessService.requireAccess(leave, principal);
        LeaveVO result = queryMapper.selectDetail(leaveId);
        if (result == null) {
            throw new BusinessException(404, "请假申请不存在");
        }
        attachFiles(result);
        return result;
    }

    @Transactional
    public Long create(LeaveCreateRequest request, UserPrincipal principal) {
        Student student = accessService.requireStudent(principal);
        if (!request.endAt().isAfter(request.startAt())) {
            throw new BusinessException("请假结束时间必须晚于开始时间");
        }
        if (request.endAt().isBefore(request.startAt().plusMinutes(30))) {
            throw new BusinessException("请假时长不能少于 30 分钟");
        }
        StudentClass current = studentClassMapper.selectOne(
                Wrappers.<StudentClass>lambdaQuery()
                        .eq(StudentClass::getStudentId, student.getId())
                        .eq(StudentClass::getStatus, "CURRENT")
                        .last("LIMIT 1"));
        if (current == null) {
            throw new BusinessException(409, "学生尚未分班，不能提交请假");
        }
        long overlap = leaveMapper.selectCount(
                Wrappers.<LeaveRequest>lambdaQuery()
                        .eq(LeaveRequest::getStudentId, student.getId())
                        .in(LeaveRequest::getStatus, "PENDING", "APPROVED")
                        .lt(LeaveRequest::getStartAt, request.endAt())
                        .gt(LeaveRequest::getEndAt, request.startAt()));
        if (overlap > 0) {
            throw new BusinessException(409, "该时间段已有待审批或已批准的请假");
        }

        LeaveRequest leave = new LeaveRequest();
        leave.setStudentId(student.getId());
        leave.setClassId(current.getClassId());
        leave.setLeaveType(request.leaveType());
        leave.setStartAt(request.startAt());
        leave.setEndAt(request.endAt());
        leave.setReason(request.reason().trim());
        leave.setStatus("PENDING");
        leave.setVersion(0);
        leaveMapper.insert(leave);
        syncAttachments(leave.getId(), principal.getUserId(), request.attachmentIds());
        return leave.getId();
    }

    @Transactional
    public void cancel(Long leaveId, UserPrincipal principal) {
        Student student = accessService.requireStudent(principal);
        LeaveRequest leave = requireLeave(leaveId);
        if (!student.getId().equals(leave.getStudentId())) {
            throw new BusinessException(403, "只能撤销本人的请假申请");
        }
        if (!"PENDING".equals(leave.getStatus())) {
            throw new BusinessException(409, "只有待审批请假可以撤销");
        }
        int updated = leaveMapper.cancel(leaveId, student.getId(), leave.getVersion());
        if (updated == 0) {
            throw new BusinessException(409, "请假状态已变化，请刷新后重试");
        }
    }

    @Transactional
    public void decide(
            Long leaveId,
            LeaveDecisionRequest request,
            UserPrincipal principal) {
        Teacher teacher = accessService.requireTeacher(principal);
        LeaveRequest leave = requireLeave(leaveId);
        if (!"PENDING".equals(leave.getStatus())) {
            throw new BusinessException(409, "该请假申请已经处理");
        }
        if (!accessService.isHomeroom(principal.getUserId(), leave.getClassId())) {
            throw new BusinessException(403, "只有当前班主任可以审批该请假");
        }
        int updated = leaveMapper.decide(
                leaveId,
                request.decision(),
                teacher.getId(),
                request.opinion().trim(),
                leave.getVersion());
        if (updated == 0) {
            throw new BusinessException(409, "请假状态已变化，请刷新后重试");
        }
    }

    private void syncAttachments(Long leaveId, Long uploaderId, List<Long> requestedIds) {
        Set<Long> targetIds = requestedIds == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(requestedIds);
        List<FileAttachment> existing = attachmentMapper.selectList(
                Wrappers.<FileAttachment>lambdaQuery()
                        .eq(FileAttachment::getBizType, "LEAVE")
                        .eq(FileAttachment::getBizId, leaveId)
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
            boolean ownedTemp = "TEMP".equals(attachment.getStatus())
                    && attachment.getUploaderUserId().equals(uploaderId);
            if (!ownedTemp) {
                throw new BusinessException(403, "只能绑定本人上传的临时附件");
            }
            attachment.setBizType("LEAVE");
            attachment.setBizId(leaveId);
            attachment.setStatus("BOUND");
            attachmentMapper.updateById(attachment);
        }
    }

    private void attachFiles(LeaveVO leave) {
        leave.setAttachments(attachmentService.listForBusiness("LEAVE", leave.getId()));
    }

    private LeaveRequest requireLeave(Long leaveId) {
        LeaveRequest leave = leaveMapper.selectById(leaveId);
        if (leave == null) {
            throw new BusinessException(404, "请假申请不存在");
        }
        return leave;
    }

    private void validateStatus(String status) {
        if (StringUtils.hasText(status)
                && !Set.of("PENDING", "APPROVED", "REJECTED", "CANCELLED")
                .contains(status)) {
            throw new BusinessException("请假状态筛选值不正确");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

