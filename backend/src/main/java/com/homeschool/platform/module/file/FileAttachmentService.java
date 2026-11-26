package com.homeschool.platform.module.file;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.homeschool.platform.common.exception.BusinessException;
import com.homeschool.platform.module.file.entity.FileAttachment;
import com.homeschool.platform.module.file.mapper.FileAttachmentMapper;
import com.homeschool.platform.module.file.vo.FileAttachmentVO;
import com.homeschool.platform.module.assignment.entity.Assignment;
import com.homeschool.platform.module.assignment.entity.AssignmentSubmission;
import com.homeschool.platform.module.assignment.mapper.AssignmentMapper;
import com.homeschool.platform.module.assignment.mapper.AssignmentSubmissionMapper;
import com.homeschool.platform.module.assignment.service.AssignmentAccessService;
import com.homeschool.platform.module.notice.entity.Notice;
import com.homeschool.platform.module.notice.mapper.NoticeMapper;
import com.homeschool.platform.module.notice.service.NoticeAccessService;
import com.homeschool.platform.module.leave.entity.LeaveRequest;
import com.homeschool.platform.module.leave.mapper.LeaveRequestMapper;
import com.homeschool.platform.module.leave.service.LeaveAccessService;
import com.homeschool.platform.module.growth.entity.GrowthRecord;
import com.homeschool.platform.module.growth.mapper.GrowthRecordMapper;
import com.homeschool.platform.module.growth.service.GrowthAccessService;
import com.homeschool.platform.security.UserPrincipal;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class FileAttachmentService {

    private final FileAttachmentMapper attachmentMapper;
    private final FileStorageService storageService;
    private final StorageProperties storageProperties;
    private final NoticeMapper noticeMapper;
    private final NoticeAccessService noticeAccessService;
    private final AssignmentMapper assignmentMapper;
    private final AssignmentSubmissionMapper submissionMapper;
    private final AssignmentAccessService assignmentAccessService;
    private final LeaveRequestMapper leaveMapper;
    private final LeaveAccessService leaveAccessService;
    private final GrowthRecordMapper growthMapper;
    private final GrowthAccessService growthAccessService;

    public FileAttachmentService(
            FileAttachmentMapper attachmentMapper,
            FileStorageService storageService,
            StorageProperties storageProperties,
            NoticeMapper noticeMapper,
            NoticeAccessService noticeAccessService,
            AssignmentMapper assignmentMapper,
            AssignmentSubmissionMapper submissionMapper,
            AssignmentAccessService assignmentAccessService,
            LeaveRequestMapper leaveMapper,
            LeaveAccessService leaveAccessService,
            GrowthRecordMapper growthMapper,
            GrowthAccessService growthAccessService) {
        this.attachmentMapper = attachmentMapper;
        this.storageService = storageService;
        this.storageProperties = storageProperties;
        this.noticeMapper = noticeMapper;
        this.noticeAccessService = noticeAccessService;
        this.assignmentMapper = assignmentMapper;
        this.submissionMapper = submissionMapper;
        this.assignmentAccessService = assignmentAccessService;
        this.leaveMapper = leaveMapper;
        this.leaveAccessService = leaveAccessService;
        this.growthMapper = growthMapper;
        this.growthAccessService = growthAccessService;
    }

    @Transactional
    public FileAttachmentVO upload(MultipartFile file, UserPrincipal principal) {
        FileStorageService.StoredFile stored =
                storageService.store(file, storageProperties);
        FileAttachment attachment = new FileAttachment();
        attachment.setUploaderUserId(principal.getUserId());
        attachment.setStorageKey(stored.storageKey());
        attachment.setOriginalName(stored.originalName());
        attachment.setExtension(stored.extension());
        attachment.setMimeType(stored.mimeType().length() > 100
                ? stored.mimeType().substring(0, 100)
                : stored.mimeType());
        attachment.setFileSize(stored.fileSize());
        attachment.setSha256(stored.sha256());
        attachment.setStatus("TEMP");
        attachmentMapper.insert(attachment);
        return toVO(attachment);
    }

    public List<FileAttachmentVO> listForNotice(Long noticeId) {
        return listForBusiness("NOTICE", noticeId);
    }

    public List<FileAttachmentVO> listForBusiness(String bizType, Long bizId) {
        return attachmentMapper.selectList(
                        Wrappers.<FileAttachment>lambdaQuery()
                                .eq(FileAttachment::getBizType, bizType)
                                .eq(FileAttachment::getBizId, bizId)
                                .eq(FileAttachment::getStatus, "BOUND")
                                .orderByAsc(FileAttachment::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    public DownloadFile download(Long attachmentId, UserPrincipal principal) {
        FileAttachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null || "DELETED".equals(attachment.getStatus())) {
            throw new BusinessException(404, "附件不存在");
        }
        if ("TEMP".equals(attachment.getStatus())) {
            if (!attachment.getUploaderUserId().equals(principal.getUserId())) {
                throw new BusinessException(403, "没有权限访问该附件");
            }
        } else if ("BOUND".equals(attachment.getStatus())
                && "NOTICE".equals(attachment.getBizType())) {
            Notice notice = noticeMapper.selectById(attachment.getBizId());
            if (notice == null) {
                throw new BusinessException(404, "附件关联的通知不存在");
            }
            var scope = noticeAccessService.resolveScope(
                    principal.getUserId(),
                    principal.getRoles());
            noticeAccessService.requireAccess(
                    notice,
                    principal.getUserId(),
                    principal.getRoles(),
                    scope);
        } else if ("BOUND".equals(attachment.getStatus())
                && "ASSIGNMENT".equals(attachment.getBizType())) {
            Assignment assignment = assignmentMapper.selectById(attachment.getBizId());
            if (assignment == null
                    || !assignmentAccessService.canAccessAssignmentAsset(
                    assignment,
                    principal)) {
                throw new BusinessException(403, "没有权限访问该附件");
            }
        } else if ("BOUND".equals(attachment.getStatus())
                && "SUBMISSION".equals(attachment.getBizType())) {
            AssignmentSubmission submission =
                    submissionMapper.selectById(attachment.getBizId());
            if (submission == null
                    || !assignmentAccessService.canReadSubmission(
                    submission,
                    principal)) {
                throw new BusinessException(403, "没有权限访问该附件");
            }
        } else if ("BOUND".equals(attachment.getStatus())
                && "LEAVE".equals(attachment.getBizType())) {
            LeaveRequest leave = leaveMapper.selectById(attachment.getBizId());
            if (leave == null || !leaveAccessService.canAccess(leave, principal)) {
                throw new BusinessException(403, "没有权限访问该附件");
            }
        } else if ("BOUND".equals(attachment.getStatus())
                && "GROWTH".equals(attachment.getBizType())) {
            GrowthRecord record = growthMapper.selectById(attachment.getBizId());
            if (record == null || !growthAccessService.canAccess(record, principal)) {
                throw new BusinessException(403, "没有权限访问该附件");
            }
        } else {
            throw new BusinessException(403, "没有权限访问该附件");
        }

        Path path = storageService.resolveSafe(attachment.getStorageKey());
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(404, "附件文件不存在");
        }
        return new DownloadFile(
                new FileSystemResource(path),
                attachment.getOriginalName(),
                attachment.getMimeType(),
                attachment.getFileSize());
    }

    @Transactional
    public void deleteTemp(Long attachmentId, UserPrincipal principal) {
        FileAttachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException(404, "附件不存在");
        }
        if (!"TEMP".equals(attachment.getStatus())
                || !attachment.getUploaderUserId().equals(principal.getUserId())) {
            throw new BusinessException(403, "只能删除本人未绑定的临时附件");
        }
        attachment.setStatus("DELETED");
        attachmentMapper.updateById(attachment);
    }

    public FileAttachmentVO toVO(FileAttachment attachment) {
        return new FileAttachmentVO(
                attachment.getId(),
                attachment.getOriginalName(),
                attachment.getExtension(),
                attachment.getMimeType(),
                attachment.getFileSize());
    }

    public record DownloadFile(
            Resource resource,
            String originalName,
            String mimeType,
            long fileSize) {
    }
}

