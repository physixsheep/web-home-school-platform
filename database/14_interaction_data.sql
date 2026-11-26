USE home_school_platform;

SET NAMES utf8mb4;

INSERT INTO interaction_message
    (id, sender_user_id, sender_role, student_id, class_id, receiver_teacher_id,
     subject, content, status, last_reply_at)
VALUES
    (1, 4, 'PARENT', 1, 1, 1, '近期学习情况',
     '老师您好，想了解一下孩子近期在课堂上的学习状态。',
     'REPLIED', CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE
    subject = VALUES(subject),
    content = VALUES(content),
    status = VALUES(status),
    last_reply_at = VALUES(last_reply_at);

INSERT INTO message_reply
    (id, message_id, replier_user_id, replier_role, content)
VALUES
    (1, 1, 2, 'TEACHER',
     '家长您好，孩子近期课堂参与积极，作业完成情况良好。')
ON DUPLICATE KEY UPDATE
    content = VALUES(content),
    replier_role = VALUES(replier_role);

