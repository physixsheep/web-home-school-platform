USE home_school_platform;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS growth_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    student_id BIGINT UNSIGNED NOT NULL COMMENT '学生',
    class_id BIGINT UNSIGNED NOT NULL COMMENT '记录时班级快照',
    category VARCHAR(30) NOT NULL
        COMMENT 'STUDY/MORAL/ACTIVITY/HONOR/COMPREHENSIVE',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content LONGTEXT NOT NULL COMMENT '记录内容',
    occurred_at DATE NOT NULL COMMENT '发生日期',
    recorder_teacher_id BIGINT UNSIGNED NOT NULL COMMENT '记录教师',
    visible_to_parent TINYINT NOT NULL DEFAULT 1 COMMENT '家长可见',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_growth_student_category_date (student_id, category, occurred_at),
    KEY idx_growth_recorder_date (recorder_teacher_id, occurred_at),
    KEY idx_growth_class_date (class_id, occurred_at),
    CONSTRAINT fk_growth_student FOREIGN KEY (student_id)
        REFERENCES student (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_growth_class FOREIGN KEY (class_id)
        REFERENCES school_class (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_growth_teacher FOREIGN KEY (recorder_teacher_id)
        REFERENCES teacher (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_growth_category
        CHECK (category IN ('STUDY', 'MORAL', 'ACTIVITY', 'HONOR', 'COMPREHENSIVE')),
    CONSTRAINT chk_growth_visible CHECK (visible_to_parent IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='学生成长档案';

