USE home_school_platform;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS notice (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    publisher_id BIGINT UNSIGNED NOT NULL COMMENT '发布用户',
    scope_type VARCHAR(20) NOT NULL COMMENT 'ALL/GRADE/CLASS',
    grade_id BIGINT UNSIGNED NULL COMMENT '年级范围',
    class_id BIGINT UNSIGNED NULL COMMENT '班级范围',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content LONGTEXT NOT NULL COMMENT '正文',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/WITHDRAWN',
    published_at DATETIME(3) NULL COMMENT '发布时间',
    withdrawn_at DATETIME(3) NULL COMMENT '撤回时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_notice_status_published (status, published_at),
    KEY idx_notice_class_status (class_id, status),
    KEY idx_notice_grade_status (grade_id, status),
    KEY idx_notice_publisher_status (publisher_id, status),
    CONSTRAINT fk_notice_publisher FOREIGN KEY (publisher_id)
        REFERENCES sys_user (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_notice_grade FOREIGN KEY (grade_id)
        REFERENCES grade (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_notice_class FOREIGN KEY (class_id)
        REFERENCES school_class (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_notice_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'WITHDRAWN')),
    CONSTRAINT chk_notice_scope
        CHECK (
            (scope_type = 'ALL' AND grade_id IS NULL AND class_id IS NULL)
            OR (scope_type = 'GRADE' AND grade_id IS NOT NULL AND class_id IS NULL)
            OR (scope_type = 'CLASS' AND class_id IS NOT NULL)
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='校园通知';

CREATE TABLE IF NOT EXISTS notice_read (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    notice_id BIGINT UNSIGNED NOT NULL COMMENT '通知',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '阅读用户',
    read_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_notice_read_notice_user (notice_id, user_id),
    KEY idx_notice_read_user_time (user_id, read_at),
    CONSTRAINT fk_notice_read_notice FOREIGN KEY (notice_id)
        REFERENCES notice (id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_notice_read_user FOREIGN KEY (user_id)
        REFERENCES sys_user (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='通知阅读记录';

CREATE TABLE IF NOT EXISTS file_attachment (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    uploader_user_id BIGINT UNSIGNED NOT NULL COMMENT '上传用户',
    biz_type VARCHAR(30) NULL COMMENT '业务类型',
    biz_id BIGINT UNSIGNED NULL COMMENT '业务记录ID',
    storage_key VARCHAR(255) NOT NULL COMMENT '私有存储相对路径',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    extension VARCHAR(20) NOT NULL COMMENT '规范化扩展名',
    mime_type VARCHAR(100) NOT NULL COMMENT '服务端记录的MIME',
    file_size BIGINT UNSIGNED NOT NULL COMMENT '文件字节数',
    sha256 CHAR(64) NULL COMMENT 'SHA-256摘要',
    status VARCHAR(20) NOT NULL DEFAULT 'TEMP' COMMENT 'TEMP/BOUND/DELETED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_file_storage_key (storage_key),
    KEY idx_file_biz (biz_type, biz_id),
    KEY idx_file_uploader_status (uploader_user_id, status),
    CONSTRAINT fk_file_uploader FOREIGN KEY (uploader_user_id)
        REFERENCES sys_user (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_file_status
        CHECK (status IN ('TEMP', 'BOUND', 'DELETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='附件信息';

