USE home_school_platform;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS interaction_message (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    sender_user_id BIGINT UNSIGNED NOT NULL COMMENT '发起用户',
    sender_role VARCHAR(20) NOT NULL COMMENT 'STUDENT/PARENT',
    student_id BIGINT UNSIGNED NOT NULL COMMENT '涉及学生',
    class_id BIGINT UNSIGNED NOT NULL COMMENT '留言时班级快照',
    receiver_teacher_id BIGINT UNSIGNED NOT NULL COMMENT '接收教师',
    subject VARCHAR(200) NOT NULL COMMENT '主题',
    content TEXT NOT NULL COMMENT '正文',
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD'
        COMMENT 'UNREAD/READ/REPLIED/CLOSED',
    last_reply_at DATETIME(3) NULL COMMENT '最近回复时间',
    closed_at DATETIME(3) NULL COMMENT '关闭时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_message_receiver_status (receiver_teacher_id, status),
    KEY idx_message_sender_time (sender_user_id, created_at),
    KEY idx_message_student (student_id),
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_user_id)
        REFERENCES sys_user (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_message_student FOREIGN KEY (student_id)
        REFERENCES student (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_message_class FOREIGN KEY (class_id)
        REFERENCES school_class (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_message_teacher FOREIGN KEY (receiver_teacher_id)
        REFERENCES teacher (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_message_sender_role
        CHECK (sender_role IN ('STUDENT', 'PARENT')),
    CONSTRAINT chk_message_status
        CHECK (status IN ('UNREAD', 'READ', 'REPLIED', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='家校留言会话';

CREATE TABLE IF NOT EXISTS message_reply (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    message_id BIGINT UNSIGNED NOT NULL COMMENT '会话',
    replier_user_id BIGINT UNSIGNED NOT NULL COMMENT '回复用户',
    replier_role VARCHAR(20) NOT NULL COMMENT 'TEACHER/STUDENT/PARENT',
    content TEXT NOT NULL COMMENT '回复正文',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_reply_message_time (message_id, created_at),
    CONSTRAINT fk_reply_message FOREIGN KEY (message_id)
        REFERENCES interaction_message (id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_reply_user FOREIGN KEY (replier_user_id)
        REFERENCES sys_user (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_reply_role
        CHECK (replier_role IN ('TEACHER', 'STUDENT', 'PARENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='留言回复';

