package com.homeschool.platform.module.file.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homeschool.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_attachment")
public class FileAttachment extends BaseEntity {

    private Long uploaderUserId;
    private String bizType;
    private Long bizId;
    private String storageKey;
    private String originalName;
    private String extension;
    private String mimeType;
    private Long fileSize;
    private String sha256;
    private String status;
}

