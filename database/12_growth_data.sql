USE home_school_platform;

SET NAMES utf8mb4;

INSERT INTO growth_record
    (id, student_id, class_id, category, title, content, occurred_at,
     recorder_teacher_id, visible_to_parent)
VALUES
    (1, 1, 1, 'STUDY', '数学阶段进步明显',
     '近期课堂参与积极，阶段练习成绩稳步提升。',
     '2026-11-15', 1, 1),
    (2, 1, 1, 'MORAL', '主动参与班级服务',
     '主动协助整理班级学习资料，展现了良好的责任意识。',
     '2026-11-20', 1, 1)
ON DUPLICATE KEY UPDATE
    category = VALUES(category),
    title = VALUES(title),
    content = VALUES(content),
    occurred_at = VALUES(occurred_at),
    recorder_teacher_id = VALUES(recorder_teacher_id),
    visible_to_parent = VALUES(visible_to_parent);

