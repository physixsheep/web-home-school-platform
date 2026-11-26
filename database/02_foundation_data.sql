USE home_school_platform;

SET NAMES utf8mb4;

INSERT INTO sys_role (id, role_code, role_name, description, status)
VALUES
    (1, 'ADMIN', '学校管理员', '全校管理权限', 'ENABLED'),
    (2, 'TEACHER', '教师', '任教班级业务权限', 'ENABLED'),
    (3, 'STUDENT', '学生', '本人学习数据权限', 'ENABLED'),
    (4, 'PARENT', '家长', '绑定孩子数据权限', 'ENABLED')
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name),
    description = VALUES(description),
    status = VALUES(status);

-- 测试密码：
-- admin / Admin@123
-- teacher01 / Teacher@123
-- student01 / Student@123
-- parent01 / Parent@123
INSERT INTO sys_user
    (id, username, password_hash, real_name, phone, email, status, token_version)
VALUES
    (1, 'admin', '$2b$10$7YA/0dzYUr1rf74ZhIWbU./0oTKKKJ4GMz1YEGaV0gKnIPV5dHewy',
     '系统管理员', '13800000001', 'admin@example.test', 'ENABLED', 0),
    (2, 'teacher01', '$2b$10$lZiHnWMxjfLiju9g9npTiuWvEwMxzmJkfup9BY5b96vk9Rvdeh88C',
     '张老师', '13800000002', 'teacher01@example.test', 'ENABLED', 0),
    (3, 'student01', '$2b$10$Rhip7F8q/faBUdLHDdUE0uxTxJNkoSSVdqersVqEJdx/cL88fatpm',
     '李同学', '13800000003', 'student01@example.test', 'ENABLED', 0),
    (4, 'parent01', '$2b$10$EuVaTEOXdzjy7DhbtMy/YebsAV1uD15AIBjFR94.N6l7K2rIJQSSq',
     '李家长', '13800000004', 'parent01@example.test', 'ENABLED', 0)
ON DUPLICATE KEY UPDATE
    real_name = VALUES(real_name),
    phone = VALUES(phone),
    email = VALUES(email),
    status = VALUES(status);

INSERT INTO sys_user_role (id, user_id, role_id, created_by)
VALUES
    (1, 1, 1, 1),
    (2, 2, 2, 1),
    (3, 3, 3, 1),
    (4, 4, 4, 1)
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id);

INSERT INTO grade (id, grade_name, grade_level, sort_order, status)
VALUES (1, '高一', 1, 1, 'ENABLED')
ON DUPLICATE KEY UPDATE
    grade_level = VALUES(grade_level),
    sort_order = VALUES(sort_order),
    status = VALUES(status);

INSERT INTO school_class
    (id, grade_id, class_name, academic_year, graduation_year, status)
VALUES (1, 1, '1班', '2026-2027', 2029, 'ENABLED')
ON DUPLICATE KEY UPDATE
    class_name = VALUES(class_name),
    academic_year = VALUES(academic_year),
    graduation_year = VALUES(graduation_year),
    status = VALUES(status);

INSERT INTO subject
    (id, subject_code, subject_name, default_full_score, sort_order, status)
VALUES
    (1, 'CHINESE', '语文', 150.00, 1, 'ENABLED'),
    (2, 'MATH', '数学', 150.00, 2, 'ENABLED'),
    (3, 'ENGLISH', '英语', 150.00, 3, 'ENABLED')
ON DUPLICATE KEY UPDATE
    subject_name = VALUES(subject_name),
    default_full_score = VALUES(default_full_score),
    sort_order = VALUES(sort_order),
    status = VALUES(status);

INSERT INTO teacher
    (id, user_id, employee_no, gender, title, specialty, hire_date, status)
VALUES (1, 2, 'T2026001', 'MALE', '一级教师', '数学', '2020-09-01', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    employee_no = VALUES(employee_no),
    title = VALUES(title),
    specialty = VALUES(specialty),
    status = VALUES(status);

INSERT INTO student
    (id, user_id, student_no, gender, birth_date, enrollment_date, status)
VALUES (1, 3, 'S2026001', 'MALE', '2010-05-12', '2026-09-01', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    student_no = VALUES(student_no),
    status = VALUES(status);

INSERT INTO parent
    (id, user_id, gender, occupation, emergency_contact, status)
VALUES (1, 4, 'FEMALE', '工程师', '13800000004', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    occupation = VALUES(occupation),
    emergency_contact = VALUES(emergency_contact),
    status = VALUES(status);

INSERT INTO parent_student
    (id, parent_id, student_id, relationship, is_primary, status, verified_by, verified_at)
VALUES (1, 1, 1, 'MOTHER', 1, 'ACTIVE', 1, CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE
    relationship = VALUES(relationship),
    is_primary = VALUES(is_primary),
    status = VALUES(status),
    verified_by = VALUES(verified_by),
    verified_at = VALUES(verified_at);

INSERT INTO teacher_class
    (id, teacher_id, class_id, subject_id, relation_type, start_date, end_date, status)
VALUES
    (1, 1, 1, NULL, 'HOMEROOM', '2026-09-01', NULL, 'ACTIVE'),
    (2, 1, 1, 2, 'SUBJECT', '2026-09-01', NULL, 'ACTIVE')
ON DUPLICATE KEY UPDATE
    subject_id = VALUES(subject_id),
    relation_type = VALUES(relation_type),
    end_date = VALUES(end_date),
    status = VALUES(status);

INSERT INTO student_class
    (id, student_id, class_id, start_date, end_date, status)
VALUES (1, 1, 1, '2026-09-01', NULL, 'CURRENT')
ON DUPLICATE KEY UPDATE
    end_date = VALUES(end_date),
    status = VALUES(status);

