USE home_school_platform;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS leave_request (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    student_id BIGINT UNSIGNED NOT NULL COMMENT '学生',
    class_id BIGINT UNSIGNED NOT NULL COMMENT '申请时班级快照',
    leave_type VARCHAR(30) NOT NULL COMMENT 'SICK/PERSONAL/OTHER',
    start_at DATETIME(3) NOT NULL COMMENT '开始时间',
    end_at DATETIME(3) NOT NULL COMMENT '结束时间',
    reason TEXT NOT NULL COMMENT '请假原因',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED',
    approver_teacher_id BIGINT UNSIGNED NULL COMMENT '审批教师',
    approved_at DATETIME(3) NULL COMMENT '审批时间',
    approval_opinion VARCHAR(500) NULL COMMENT '审批意见',
    cancelled_at DATETIME(3) NULL COMMENT '撤销时间',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '并发版本',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_leave_student_start (student_id, start_at),
    KEY idx_leave_class_status_start (class_id, status, start_at),
    KEY idx_leave_approver_approved (approver_teacher_id, approved_at),
    CONSTRAINT fk_leave_student FOREIGN KEY (student_id)
        REFERENCES student (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_leave_class FOREIGN KEY (class_id)
        REFERENCES school_class (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_leave_teacher FOREIGN KEY (approver_teacher_id)
        REFERENCES teacher (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_leave_type
        CHECK (leave_type IN ('SICK', 'PERSONAL', 'OTHER')),
    CONSTRAINT chk_leave_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_leave_time CHECK (end_at > start_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='请假申请';

