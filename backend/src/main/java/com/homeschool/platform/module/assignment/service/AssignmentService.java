package com.homeschool.platform.module.assignment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homeschool.platform.common.api.PageResponse;
import com.homeschool.platform.common.constant.RoleCodes;
import com.homeschool.platform.common.exception.BusinessException;
import com.homeschool.platform.module.assignment.dto.AssignmentGradeRequest;
import com.homeschool.platform.module.assignment.dto.AssignmentQuery;
import com.homeschool.platform.module.assignment.dto.AssignmentReturnRequest;
import com.homeschool.platform.module.assignment.dto.AssignmentSaveRequest;
import com.homeschool.platform.module.assignment.dto.AssignmentSubmissionRequest;
import com.homeschool.platform.module.assignment.dto.SubmissionQuery;
import com.homeschool.platform.module.assignment.entity.Assignment;
import com.homeschool.platform.module.assignment.entity.AssignmentComment;
import com.homeschool.platform.module.assignment.entity.AssignmentSubmission;
import com.homeschool.platform.module.assignment.mapper.AssignmentCommentMapper;
import com.homeschool.platform.module.assignment.mapper.AssignmentMapper;
import com.homeschool.platform.module.assignment.mapper.AssignmentQueryMapper;
import com.homeschool.platform.module.assignment.mapper.AssignmentSubmissionMapper;
import com.homeschool.platform.module.assignment.vo.AssignmentOptionsVO;
import com.homeschool.platform.module.assignment.vo.AssignmentVO;
import com.homeschool.platform.module.assignment.vo.SubmissionRowVO;
import com.homeschool.platform.module.file.FileAttachmentService;
import com.homeschool.platform.module.file.entity.FileAttachment;
import com.homeschool.platform.module.file.mapper.FileAttachmentMapper;
import com.homeschool.platform.module.organization.entity.Student;
import com.homeschool.platform.module.organization.entity.Teacher;
import com.homeschool.platform.module.organization.mapper.StudentMapper;
import com.homeschool.platform.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AssignmentService {

    private final AssignmentMapper assignmentMapper;
    private final AssignmentSubmissionMapper submissionMapper;
    private final AssignmentCommentMapper commentMapper;
    private final AssignmentQueryMapper queryMapper;
    private final AssignmentAccessService accessService;
    private final AssignmentOptionService optionService;
    private final FileAttachmentMapper attachmentMapper;
    private final FileAttachmentService attachmentService;
    private final StudentMapper studentMapper;

    public AssignmentService(
            AssignmentMapper assignmentMapper,
            AssignmentSubmissionMapper submissionMapper,
            AssignmentCommentMapper commentMapper,
            AssignmentQueryMapper queryMapper,
            AssignmentAccessService accessService,
            AssignmentOptionService optionService,
            FileAttachmentMapper attachmentMapper,
            FileAttachmentService attachmentService,
            StudentMapper studentMapper) {
        this.assignmentMapper = assignmentMapper;
        this.submissionMapper = submissionMapper;
        this.commentMapper = commentMapper;
        this.queryMapper = queryMapper;
        this.accessService = accessService;
        this.optionService = optionService;
        this.attachmentMapper = attachmentMapper;
        this.attachmentService = attachmentService;
        this.studentMapper = studentMapper;
    }

    public PageResponse<AssignmentVO> managePage(
            AssignmentQuery query,
            UserPrincipal principal) {
        validateAssignmentQuery(query);
        Teacher teacher = accessService.requireTeacher(principal);
        query.setKeyword(trimToNull(query.getKeyword()));
        var result = queryMapper.selectManagePage(
                new Page<>(query.normalizedPage(), query.normalizedSize()),
                query,
                teacher.getId(),
                false);
        result.getRecords().forEach(this::attachAssignmentFiles);
        return PageResponse.from(result);
    }

    public PageResponse<AssignmentVO> readablePage(
            AssignmentQuery query,
            UserPrincipal principal) {
        Long studentId = accessService.resolveReadStudentId(
                principal,
                query.getStudentId());
        boolean admin = principal.getRoles().contains(RoleCodes.ADMIN);
        List<Long> classIds = accessService.readableClassIds(principal, studentId);
        query.setKeyword(trimToNull(query.getKeyword()));
        var result = queryMapper.selectReadablePage(
                new Page<>(query.normalizedPage(), query.normalizedSize()),
                query,
                classIds,
                admin,
                studentId);
        result.getRecords().forEach(item -> {
            attachAssignmentFiles(item);
            attachSubmissionFiles(item);
        });
        return PageResponse.from(result);
    }

    public AssignmentVO detail(
            Long assignmentId,
            Long studentId,
            UserPrincipal principal) {
        Assignment assignment = requireAssignment(assignmentId);
        Long targetStudentId = principal.getRoles().contains(RoleCodes.TEACHER)
                || principal.getRoles().contains(RoleCodes.ADMIN)
                ? studentId
                : accessService.resolveReadStudentId(principal, studentId);
        if (!accessService.canReadAssignment(assignment, principal, targetStudentId)) {
            throw new BusinessException(403, "没有权限查看该作业");
        }
        AssignmentVO result = queryMapper.selectDetail(assignmentId, targetStudentId);
        if (result == null) {
            throw new BusinessException(404, "作业不存在");
        }
        attachAssignmentFiles(result);
        attachSubmissionFiles(result);
        return result;
    }

    public AssignmentOptionsVO options(UserPrincipal principal) {
        return optionService.options(principal);
    }

    @Transactional
    public Long create(AssignmentSaveRequest request, UserPrincipal principal) {
        Teacher teacher = accessService.requireTeacher(principal);
        accessService.requireTeaching(
                teacher.getId(),
                request.classId(),
                request.subjectId());
        validateDeadlineForNew(request.deadline());
        Assignment assignment = new Assignment();
        assignment.setTeacherId(teacher.getId());
        assignment.setStatus("DRAFT");
        apply(request, assignment);
        assignmentMapper.insert(assignment);
        syncAttachments(
                "ASSIGNMENT",
                assignment.getId(),
                principal.getUserId(),
                request.attachmentIds());
        return assignment.getId();
    }

    @Transactional
    public void update(
            Long assignmentId,
            AssignmentSaveRequest request,
            UserPrincipal principal) {
        Assignment assignment = requireAssignment(assignmentId);
        accessService.requireManage(assignment, principal);
        if ("CLOSED".equals(assignment.getStatus())) {
            throw new BusinessException(409, "已关闭作业不能编辑");
        }
        long submissionCount = submissionMapper.selectCount(
                Wrappers.<AssignmentSubmission>lambdaQuery()
                        .eq(AssignmentSubmission::getAssignmentId, assignmentId));
        if (submissionCount > 0
                && (!assignment.getClassId().equals(request.classId())
                || !assignment.getSubjectId().equals(request.subjectId()))) {
            throw new BusinessException(409, "已有人提交后不能修改作业班级或学科");
        }
        Teacher teacher = accessService.requireTeacher(principal);
        accessService.requireTeaching(
                teacher.getId(),
                request.classId(),
                request.subjectId());
        if ("DRAFT".equals(assignment.getStatus())) {
            validateDeadlineForNew(request.deadline());
        }
        apply(request, assignment);
        assignmentMapper.updateById(assignment);
        syncAttachments(
                "ASSIGNMENT",
                assignmentId,
                principal.getUserId(),
                request.attachmentIds());
    }

    @Transactional
    public void publish(Long assignmentId, UserPrincipal principal) {
        Assignment assignment = requireAssignment(assignmentId);
        accessService.requireManage(assignment, principal);
        if (!"DRAFT".equals(assignment.getStatus())) {
            throw new BusinessException(409, "只有草稿作业可以发布");
        }
        if (assignment.getDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException("作业截止时间已过，不能发布");
        }
        assignment.setStatus("PUBLISHED");
        assignment.setPublishedAt(LocalDateTime.now());
        assignmentMapper.updateById(assignment);
    }

    @Transactional
    public void close(Long assignmentId, UserPrincipal principal) {
        Assignment assignment = requireAssignment(assignmentId);
        accessService.requireManage(assignment, principal);
        if (!"PUBLISHED".equals(assignment.getStatus())) {
            throw new BusinessException(409, "只有已发布作业可以关闭");
        }
        assignment.setStatus("CLOSED");
        assignmentMapper.updateById(assignment);
    }

    @Transactional
    public void deleteDraft(Long assignmentId, UserPrincipal principal) {
        Assignment assignment = requireAssignment(assignmentId);
        accessService.requireManage(assignment, principal);
        if (!"DRAFT".equals(assignment.getStatus())) {
            throw new BusinessException(409, "只有草稿作业可以删除");
        }
        syncAttachments("ASSIGNMENT", assignmentId, principal.getUserId(), List.of());
        assignmentMapper.deleteById(assignmentId);
    }

    @Transactional
    public Long submit(
            Long assignmentId,
            AssignmentSubmissionRequest request,
            UserPrincipal principal) {
        if (!principal.getRoles().contains(RoleCodes.STUDENT)) {
            throw new BusinessException(403, "只有学生可以提交作业");
        }
        Student student = studentMapper.selectOne(
                Wrappers.<Student>lambdaQuery()
                        .eq(Student::getUserId, principal.getUserId()));
        if (student == null) {
            throw new BusinessException(403, "学生档案不存在");
        }
        Assignment assignment = requireAssignment(assignmentId);
        if (!"PUBLISHED".equals(assignment.getStatus())
                || !accessService.canReadAssignment(assignment, principal, student.getId())) {
            throw new BusinessException(403, "作业不可提交或不属于当前班级");
        }
        AssignmentSubmission existing = submissionMapper.selectForUpdate(
                assignmentId,
                student.getId());
        if (existing != null && "GRADED".equals(existing.getStatus())) {
            throw new BusinessException(409, "已批改作业不能直接重交，请先联系教师退回");
        }
        boolean returned = existing != null && "RETURNED".equals(existing.getStatus());
        if (LocalDateTime.now().isAfter(assignment.getDeadline()) && !returned) {
            throw new BusinessException(409, "作业已截止，不能提交");
        }
        boolean hasContent = StringUtils.hasText(request.content());
        boolean hasFiles = request.attachmentIds() != null
                && !request.attachmentIds().isEmpty();
        if (!hasContent && !hasFiles) {
            throw new BusinessException("请填写提交内容或上传附件");
        }

        AssignmentSubmission submission = existing == null
                ? new AssignmentSubmission()
                : existing;
        if (existing == null) {
            submission.setAssignmentId(assignmentId);
            submission.setStudentId(student.getId());
            submission.setAttemptCount(1);
            submission.setVersion(0);
            submission.setSubmittedAt(LocalDateTime.now());
            submission.setStatus("SUBMITTED");
            submission.setContent(trimToNull(request.content()));
            submissionMapper.insert(submission);
        } else {
            submission.setAttemptCount(submission.getAttemptCount() + 1);
            submission.setVersion(submission.getVersion() + 1);
            submission.setSubmittedAt(LocalDateTime.now());
            submission.setStatus("SUBMITTED");
            submission.setReturnedAt(null);
            submission.setReturnReason(null);
            submission.setContent(trimToNull(request.content()));
            submissionMapper.updateById(submission);
        }
        syncAttachments(
                "SUBMISSION",
                submission.getId(),
                principal.getUserId(),
                request.attachmentIds());
        return submission.getId();
    }

    public PageResponse<SubmissionRowVO> submissions(
            Long assignmentId,
            SubmissionQuery query,
            UserPrincipal principal) {
        Assignment assignment = requireAssignment(assignmentId);
        accessService.requireManage(assignment, principal);
        validateSubmissionStatus(query.getStatus());
        query.setKeyword(trimToNull(query.getKeyword()));
        var result = queryMapper.selectSubmissionPage(
                new Page<>(query.normalizedPage(), query.normalizedSize()),
                assignmentId,
                assignment.getClassId(),
                query);
        for (SubmissionRowVO row : result.getRecords()) {
            if (row.getSubmissionId() != null) {
                row.setAttachments(attachmentService.listForBusiness(
                        "SUBMISSION",
                        row.getSubmissionId()));
            } else {
                row.setAttachments(List.of());
            }
        }
        return PageResponse.from(result);
    }

    @Transactional
    public void grade(
            Long submissionId,
            AssignmentGradeRequest request,
            UserPrincipal principal) {
        AssignmentSubmission submission = requireSubmission(submissionId);
        Assignment assignment = requireAssignment(submission.getAssignmentId());
        accessService.requireManage(assignment, principal);
        if ("RETURNED".equals(submission.getStatus())) {
            throw new BusinessException(409, "退回的作业需要学生重新提交后才能批改");
        }
        if (request.score() != null
                && assignment.getMaxScore() != null
                && request.score().compareTo(assignment.getMaxScore()) > 0) {
            throw new BusinessException("作业成绩不能超过满分");
        }
        Teacher teacher = accessService.requireTeacher(principal);
        AssignmentComment comment = commentMapper.selectOne(
                Wrappers.<AssignmentComment>lambdaQuery()
                        .eq(AssignmentComment::getSubmissionId, submissionId));
        if (comment == null) {
            comment = new AssignmentComment();
            comment.setSubmissionId(submissionId);
            comment.setTeacherId(teacher.getId());
            comment.setScore(request.score());
            comment.setComment(trimToNull(request.comment()));
            comment.setGradedAt(LocalDateTime.now());
            commentMapper.insert(comment);
        } else {
            comment.setTeacherId(teacher.getId());
            comment.setScore(request.score());
            comment.setComment(trimToNull(request.comment()));
            comment.setGradedAt(LocalDateTime.now());
            commentMapper.updateById(comment);
        }
        submission.setStatus("GRADED");
        submission.setVersion(submission.getVersion() + 1);
        submissionMapper.updateById(submission);
    }

    @Transactional
    public void returnSubmission(
            Long submissionId,
            AssignmentReturnRequest request,
            UserPrincipal principal) {
        AssignmentSubmission submission = requireSubmission(submissionId);
        Assignment assignment = requireAssignment(submission.getAssignmentId());
        accessService.requireManage(assignment, principal);
        if ("RETURNED".equals(submission.getStatus())) {
            throw new BusinessException(409, "作业已经退回");
        }
        submission.setStatus("RETURNED");
        submission.setReturnedAt(LocalDateTime.now());
        submission.setReturnReason(request.reason().trim());
        submission.setVersion(submission.getVersion() + 1);
        submissionMapper.updateById(submission);
    }

    private void apply(AssignmentSaveRequest request, Assignment assignment) {
        assignment.setClassId(request.classId());
        assignment.setSubjectId(request.subjectId());
        assignment.setTitle(request.title().trim());
        assignment.setContent(request.content().trim());
        assignment.setDeadline(request.deadline());
        assignment.setMaxScore(request.maxScore());
    }

    private void syncAttachments(
            String bizType,
            Long bizId,
            Long uploaderId,
            List<Long> requestedIds) {
        Set<Long> targetIds = requestedIds == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(requestedIds);
        List<FileAttachment> existing = attachmentMapper.selectList(
                Wrappers.<FileAttachment>lambdaQuery()
                        .eq(FileAttachment::getBizType, bizType)
                        .eq(FileAttachment::getBizId, bizId)
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
                    && bizType.equals(attachment.getBizType())
                    && bizId.equals(attachment.getBizId());
            if (!tempOwned && !alreadyBound) {
                throw new BusinessException(403, "附件无权绑定到当前作业");
            }
            attachment.setBizType(bizType);
            attachment.setBizId(bizId);
            attachment.setStatus("BOUND");
            attachmentMapper.updateById(attachment);
        }
    }

    private void attachAssignmentFiles(AssignmentVO assignment) {
        assignment.setAttachments(attachmentService.listForBusiness(
                "ASSIGNMENT",
                assignment.getId()));
    }

    private void attachSubmissionFiles(AssignmentVO assignment) {
        if (assignment.getSubmissionId() != null) {
            assignment.setSubmissionAttachments(attachmentService.listForBusiness(
                    "SUBMISSION",
                    assignment.getSubmissionId()));
        } else {
            assignment.setSubmissionAttachments(List.of());
        }
    }

    private Assignment requireAssignment(Long assignmentId) {
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException(404, "作业不存在");
        }
        return assignment;
    }

    private AssignmentSubmission requireSubmission(Long submissionId) {
        AssignmentSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(404, "作业提交不存在");
        }
        return submission;
    }

    private void validateDeadlineForNew(LocalDateTime deadline) {
        if (!deadline.isAfter(LocalDateTime.now())) {
            throw new BusinessException("作业截止时间必须晚于当前时间");
        }
    }

    private void validateAssignmentQuery(AssignmentQuery query) {
        if (StringUtils.hasText(query.getStatus())
                && !Set.of("DRAFT", "PUBLISHED", "CLOSED").contains(query.getStatus())) {
            throw new BusinessException("作业状态筛选值不正确");
        }
    }

    private void validateSubmissionStatus(String status) {
        if (StringUtils.hasText(status)
                && !Set.of("UNSUBMITTED", "SUBMITTED", "GRADED", "RETURNED")
                .contains(status)) {
            throw new BusinessException("提交状态筛选值不正确");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

