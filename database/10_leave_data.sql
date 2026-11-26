USE home_school_platform;

SET NAMES utf8mb4;

INSERT INTO leave_request
    (id, student_id, class_id, leave_type, start_at, end_at, reason,
     status, approver_teacher_id, approved_at, approval_opinion, version)
VALUES
    (1, 1, 1, 'SICK', '2026-11-28 08:00:00.000',
     '2026-11-28 17:30:00.000', '身体不适，需要就医。',
     'PENDING', NULL, NULL, NULL, 0)
ON DUPLICATE KEY UPDATE
    leave_type = VALUES(leave_type),
    start_at = VALUES(start_at),
    end_at = VALUES(end_at),
    reason = VALUES(reason),
    status = VALUES(status),
    approver_teacher_id = VALUES(approver_teacher_id),
    approved_at = VALUES(approved_at),
    approval_opinion = VALUES(approval_opinion);

