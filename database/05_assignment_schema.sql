USE home_school_platform;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS assignment (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    class_id BIGINT UNSIGNED NOT NULL COMMENT '班级',
    teacher_id BIGINT UNSIGNED NOT NULL COMMENT '发布教师',
    subject_id BIGINT UNSIGNED NOT NULL COMMENT '学科',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content LONGTEXT NOT NULL COMMENT '作业要求',
    deadline DATETIME(3) NOT NULL COMMENT '截止时间',
    max_score DECIMAL(6,2) NULL COMMENT '满分',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/CLOSED',
    published_at DATETIME(3) NULL COMMENT '发布时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_assignment_class_status_deadline (class_id, status, deadline),
    KEY idx_assignment_teacher_status (teacher_id, status),
    KEY idx_assignment_subject (subject_id),
    CONSTRAINT fk_assignment_class FOREIGN KEY (class_id)
        REFERENCES school_class (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_assignment_teacher FOREIGN KEY (teacher_id)
        REFERENCES teacher (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_assignment_subject FOREIGN KEY (subject_id)
        REFERENCES subject (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_assignment_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED')),
    CONSTRAINT chk_assignment_max_score
        CHECK (max_score IS NULL OR max_score > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='作业';

CREATE TABLE IF NOT EXISTS assignment_submission (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    assignment_id BIGINT UNSIGNED NOT NULL COMMENT '作业',
    student_id BIGINT UNSIGNED NOT NULL COMMENT '学生',
    content LONGTEXT NULL COMMENT '提交内容',
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED'
        COMMENT 'SUBMITTED/GRADED/RETURNED',
    attempt_count INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '提交次数',
    submitted_at DATETIME(3) NOT NULL COMMENT '当前提交时间',
    returned_at DATETIME(3) NULL COMMENT '退回时间',
    return_reason VARCHAR(500) NULL COMMENT '退回原因',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '并发版本',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_submission_assignment_student (assignment_id, student_id),
    KEY idx_submission_student_status (student_id, status),
    CONSTRAINT fk_submission_assignment FOREIGN KEY (assignment_id)
        REFERENCES assignment (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_submission_student FOREIGN KEY (student_id)
        REFERENCES student (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_submission_status
        CHECK (status IN ('SUBMITTED', 'GRADED', 'RETURNED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='作业提交';

CREATE TABLE IF NOT EXISTS assignment_comment (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    submission_id BIGINT UNSIGNED NOT NULL COMMENT '作业提交',
    teacher_id BIGINT UNSIGNED NOT NULL COMMENT '批改教师',
    score DECIMAL(6,2) NULL COMMENT '成绩',
    comment TEXT NULL COMMENT '评语',
    graded_at DATETIME(3) NOT NULL COMMENT '批改时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_assignment_comment_submission (submission_id),
    KEY idx_assignment_comment_teacher (teacher_id),
    CONSTRAINT fk_comment_submission FOREIGN KEY (submission_id)
        REFERENCES assignment_submission (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_comment_teacher FOREIGN KEY (teacher_id)
        REFERENCES teacher (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_comment_score CHECK (score IS NULL OR score >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='作业批改';

