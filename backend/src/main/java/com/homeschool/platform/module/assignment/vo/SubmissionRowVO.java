package com.homeschool.platform.module.assignment.vo;

import com.homeschool.platform.module.file.vo.FileAttachmentVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SubmissionRowVO {

    private Long studentId;
    private String studentName;
    private String studentNo;
    private Long submissionId;
    private String status;
    private Integer attemptCount;
    private String content;
    private LocalDateTime submittedAt;
    private BigDecimal score;
    private String comment;
    private LocalDateTime gradedAt;
    private String returnReason;
    private List<FileAttachmentVO> attachments;
}

