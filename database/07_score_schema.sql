USE home_school_platform;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS examination (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(150) NOT NULL COMMENT '考试名称',
    exam_type VARCHAR(30) NOT NULL COMMENT '月考/期中/期末等',
    grade_id BIGINT UNSIGNED NOT NULL COMMENT '年级',
    class_id BIGINT UNSIGNED NULL COMMENT '班级；空表示年级范围',
    academic_year VARCHAR(20) NOT NULL COMMENT '学年',
    term VARCHAR(20) NOT NULL COMMENT '学期',
    exam_date DATE NOT NULL COMMENT '考试日期',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/ARCHIVED',
    created_by BIGINT UNSIGNED NOT NULL COMMENT '创建用户',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_exam_grade_date (grade_id, exam_date),
    KEY idx_exam_class_date (class_id, exam_date),
    KEY idx_exam_term (academic_year, term),
    KEY idx_exam_status (status),
    CONSTRAINT fk_exam_grade FOREIGN KEY (grade_id)
        REFERENCES grade (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_exam_class FOREIGN KEY (class_id)
        REFERENCES school_class (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_exam_creator FOREIGN KEY (created_by)
        REFERENCES sys_user (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_exam_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='考试信息';

CREATE TABLE IF NOT EXISTS score (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    exam_id BIGINT UNSIGNED NOT NULL COMMENT '考试',
    student_id BIGINT UNSIGNED NOT NULL COMMENT '学生',
    subject_id BIGINT UNSIGNED NOT NULL COMMENT '学科',
    class_id BIGINT UNSIGNED NOT NULL COMMENT '考试时班级快照',
    score DECIMAL(6,2) NULL COMMENT '分数；缺考可为空',
    is_absent TINYINT NOT NULL DEFAULT 0 COMMENT '是否缺考',
    remark VARCHAR(255) NULL COMMENT '备注',
    entered_by BIGINT UNSIGNED NOT NULL COMMENT '首次录入用户',
    updated_by BIGINT UNSIGNED NULL COMMENT '最近修改用户',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_score_exam_student_subject (exam_id, student_id, subject_id),
    KEY idx_score_exam_class_subject (exam_id, class_id, subject_id),
    KEY idx_score_student_exam (student_id, exam_id),
    CONSTRAINT fk_score_exam FOREIGN KEY (exam_id)
        REFERENCES examination (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_score_student FOREIGN KEY (student_id)
        REFERENCES student (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_score_subject FOREIGN KEY (subject_id)
        REFERENCES subject (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_score_class FOREIGN KEY (class_id)
        REFERENCES school_class (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_score_entered_by FOREIGN KEY (entered_by)
        REFERENCES sys_user (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_score_updated_by FOREIGN KEY (updated_by)
        REFERENCES sys_user (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_score_value CHECK (score IS NULL OR score >= 0),
    CONSTRAINT chk_score_absent CHECK (
        (is_absent = 1 AND score IS NULL)
        OR (is_absent = 0)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='考试成绩';

