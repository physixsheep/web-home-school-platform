package com.homeschool.platform.module.leave.vo;

import com.homeschool.platform.module.file.vo.FileAttachmentVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LeaveVO {

    private Long id;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private Long classId;
    private String className;
    private String gradeName;
    private String leaveType;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String reason;
    private String status;
    private Long approverTeacherId;
    private String approverName;
    private LocalDateTime approvedAt;
    private String approvalOpinion;
    private LocalDateTime cancelledAt;
    private Integer version;
    private LocalDateTime createdAt;
    private List<FileAttachmentVO> attachments;
}

