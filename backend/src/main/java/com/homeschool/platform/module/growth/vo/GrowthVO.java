package com.homeschool.platform.module.growth.vo;

import com.homeschool.platform.module.file.vo.FileAttachmentVO;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GrowthVO {

    private Long id;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private Long classId;
    private String className;
    private String gradeName;
    private String category;
    private String title;
    private String content;
    private LocalDate occurredAt;
    private Long recorderTeacherId;
    private String recorderName;
    private Integer visibleToParent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<FileAttachmentVO> attachments;
}

