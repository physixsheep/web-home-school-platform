USE home_school_platform;

SET NAMES utf8mb4;

INSERT INTO examination
    (id, name, exam_type, grade_id, class_id, academic_year, term,
     exam_date, status, created_by)
VALUES
    (1, '2026学年第一学期期中考试', 'MIDTERM', 1, NULL, '2026-2027',
     '第一学期', '2026-11-10', 'PUBLISHED', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    exam_type = VALUES(exam_type),
    grade_id = VALUES(grade_id),
    class_id = VALUES(class_id),
    academic_year = VALUES(academic_year),
    term = VALUES(term),
    exam_date = VALUES(exam_date),
    status = VALUES(status);

INSERT INTO score
    (id, exam_id, student_id, subject_id, class_id, score, is_absent,
     remark, entered_by)
VALUES
    (1, 1, 1, 1, 1, 118.00, 0, NULL, 1),
    (2, 1, 1, 2, 1, 128.00, 0, NULL, 1),
    (3, 1, 1, 3, 1, 126.00, 0, NULL, 1)
ON DUPLICATE KEY UPDATE
    class_id = VALUES(class_id),
    score = VALUES(score),
    is_absent = VALUES(is_absent),
    remark = VALUES(remark),
    updated_by = VALUES(entered_by);

