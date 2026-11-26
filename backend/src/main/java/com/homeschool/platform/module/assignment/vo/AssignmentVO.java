package com.homeschool.platform.module.assignment.vo;

import com.homeschool.platform.module.file.vo.FileAttachmentVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AssignmentVO {

    private Long id;
    private Long classId;
    private String className;
    private String gradeName;
    private Long teacherId;
    private String teacherName;
    private Long subjectId;
    private String subjectName;
    private String title;
    private String content;
    private LocalDateTime deadline;
    private BigDecimal maxScore;
    private String status;
    private LocalDateTime publishedAt;
    private Long studentCount;
    private Long submittedCount;
    private Long gradedCount;
    private Long unsubmittedCount;
    private Long submissionId;
    private Long targetStudentId;
    private String submissionStatus;
    private String submissionContent;
    private Integer attemptCount;
    private LocalDateTime submittedAt;
    private BigDecimal score;
    private String teacherComment;
    private LocalDateTime gradedAt;
    private String returnReason;
    private List<FileAttachmentVO> attachments;
    private List<FileAttachmentVO> submissionAttachments;
}

