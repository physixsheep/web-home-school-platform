package com.homeschool.platform.module.interaction.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homeschool.platform.common.api.PageResponse;
import com.homeschool.platform.common.constant.RoleCodes;
import com.homeschool.platform.common.exception.BusinessException;
import com.homeschool.platform.module.interaction.dto.MessageCreateRequest;
import com.homeschool.platform.module.interaction.dto.MessageQuery;
import com.homeschool.platform.module.interaction.dto.MessageReplyRequest;
import com.homeschool.platform.module.interaction.entity.InteractionMessage;
import com.homeschool.platform.module.interaction.entity.MessageReply;
import com.homeschool.platform.module.interaction.mapper.InteractionMessageMapper;
import com.homeschool.platform.module.interaction.mapper.MessageQueryMapper;
import com.homeschool.platform.module.interaction.mapper.MessageReplyMapper;
import com.homeschool.platform.module.interaction.vo.MessageDetailVO;
import com.homeschool.platform.module.interaction.vo.MessageVO;
import com.homeschool.platform.module.organization.entity.Student;
import com.homeschool.platform.module.organization.entity.StudentClass;
import com.homeschool.platform.module.organization.entity.Teacher;
import com.homeschool.platform.module.organization.vo.EntityOptionVO;
import com.homeschool.platform.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class MessageService {

    private final InteractionMessageMapper messageMapper;
    private final MessageReplyMapper replyMapper;
    private final MessageQueryMapper queryMapper;
    private final MessageAccessService accessService;

    public MessageService(
            InteractionMessageMapper messageMapper,
            MessageReplyMapper replyMapper,
            MessageQueryMapper queryMapper,
            MessageAccessService accessService) {
        this.messageMapper = messageMapper;
        this.replyMapper = replyMapper;
        this.queryMapper = queryMapper;
        this.accessService = accessService;
    }

    public PageResponse<MessageVO> minePage(MessageQuery query, UserPrincipal principal) {
        validateQuery(query);
        query.setKeyword(trimToNull(query.getKeyword()));
        var result = queryMapper.selectMinePage(
                new Page<>(query.normalizedPage(), query.normalizedSize()),
                query,
                principal.getUserId());
        return PageResponse.from(result);
    }

    public PageResponse<MessageVO> receivedPage(MessageQuery query, UserPrincipal principal) {
        validateQuery(query);
        query.setKeyword(trimToNull(query.getKeyword()));
        boolean admin = principal.getRoles().contains(RoleCodes.ADMIN);
        Long teacherId = null;
        if (!admin) {
            teacherId = accessService.requireTeacher(principal).getId();
        }
        var result = queryMapper.selectReceivedPage(
                new Page<>(query.normalizedPage(), query.normalizedSize()),
                query,
                teacherId,
                admin);
        return PageResponse.from(result);
    }

    public List<EntityOptionVO> teacherOptions(
            Long studentId,
            UserPrincipal principal) {
        Student student = accessService.resolveCreateStudent(principal, studentId);
        accessService.currentClass(student.getId());
        return queryMapper.selectTeacherOptions(student.getId());
    }

    public MessageDetailVO detail(Long messageId, UserPrincipal principal) {
        InteractionMessage message = requireMessage(messageId);
        accessService.requireParticipant(message, principal);
        MessageVO view = queryMapper.selectDetail(messageId);
        if (view == null) {
            throw new BusinessException(404, "留言会话不存在");
        }
        return new MessageDetailVO(view, queryMapper.selectReplies(messageId));
    }

    @Transactional
    public Long create(MessageCreateRequest request, UserPrincipal principal) {
        Student student = accessService.resolveCreateStudent(
                principal,
                request.studentId());
        StudentClass current = accessService.currentClass(student.getId());
        accessService.requireTeacherForClass(request.teacherId(), current.getClassId());
        String senderRole = principal.getRoles().contains(RoleCodes.PARENT)
                ? RoleCodes.PARENT
                : RoleCodes.STUDENT;

        InteractionMessage message = new InteractionMessage();
        message.setSenderUserId(principal.getUserId());
        message.setSenderRole(senderRole);
        message.setStudentId(student.getId());
        message.setClassId(current.getClassId());
        message.setReceiverTeacherId(request.teacherId());
        message.setSubject(request.subject().trim());
        message.setContent(request.content().trim());
        message.setStatus("UNREAD");
        messageMapper.insert(message);
        return message.getId();
    }

    @Transactional
    public void markRead(Long messageId, UserPrincipal principal) {
        InteractionMessage message = requireMessage(messageId);
        if (!principal.getRoles().contains(RoleCodes.TEACHER)) {
            throw new BusinessException(403, "只有接收教师可以标记留言已读");
        }
        Teacher teacher = accessService.requireTeacher(principal);
        if (!teacher.getId().equals(message.getReceiverTeacherId())) {
            throw new BusinessException(403, "只能标记本人接收的留言");
        }
        if ("UNREAD".equals(message.getStatus())) {
            message.setStatus("READ");
            messageMapper.updateById(message);
        }
    }

    @Transactional
    public void reply(
            Long messageId,
            MessageReplyRequest request,
            UserPrincipal principal) {
        InteractionMessage message = requireMessage(messageId);
        accessService.requireParticipant(message, principal);
        if ("CLOSED".equals(message.getStatus())) {
            throw new BusinessException(409, "留言会话已关闭");
        }
        String replierRole;
        if (principal.getRoles().contains(RoleCodes.TEACHER)) {
            Teacher teacher = accessService.requireTeacher(principal);
            if (!teacher.getId().equals(message.getReceiverTeacherId())) {
                throw new BusinessException(403, "只能回复本人接收的留言");
            }
            replierRole = RoleCodes.TEACHER;
            message.setStatus("REPLIED");
        } else if (message.getSenderUserId().equals(principal.getUserId())) {
            replierRole = message.getSenderRole();
            message.setStatus("UNREAD");
        } else {
            throw new BusinessException(403, "没有权限回复该留言");
        }

        MessageReply reply = new MessageReply();
        reply.setMessageId(messageId);
        reply.setReplierUserId(principal.getUserId());
        reply.setReplierRole(replierRole);
        reply.setContent(request.content().trim());
        replyMapper.insert(reply);
        message.setLastReplyAt(reply.getCreatedAt() == null
                ? LocalDateTime.now()
                : reply.getCreatedAt());
        messageMapper.updateById(message);
    }

    @Transactional
    public void close(Long messageId, UserPrincipal principal) {
        InteractionMessage message = requireMessage(messageId);
        if (!principal.getRoles().contains(RoleCodes.TEACHER)) {
            throw new BusinessException(403, "只有接收教师可以关闭留言");
        }
        Teacher teacher = accessService.requireTeacher(principal);
        if (!teacher.getId().equals(message.getReceiverTeacherId())) {
            throw new BusinessException(403, "只能关闭本人接收的留言");
        }
        if ("CLOSED".equals(message.getStatus())) {
            return;
        }
        message.setStatus("CLOSED");
        message.setClosedAt(LocalDateTime.now());
        messageMapper.updateById(message);
    }

    private InteractionMessage requireMessage(Long messageId) {
        InteractionMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(404, "留言会话不存在");
        }
        return message;
    }

    private void validateQuery(MessageQuery query) {
        if (StringUtils.hasText(query.getStatus())
                && !Set.of("UNREAD", "READ", "REPLIED", "CLOSED")
                .contains(query.getStatus())) {
            throw new BusinessException("留言状态筛选值不正确");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

