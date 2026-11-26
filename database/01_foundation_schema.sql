USE home_school_platform;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    username VARCHAR(50) NOT NULL COMMENT '登录账号',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
    real_name VARCHAR(50) NOT NULL COMMENT '姓名',
    phone VARCHAR(20) NULL COMMENT '手机号',
    email VARCHAR(100) NULL COMMENT '邮箱',
    avatar_file_id BIGINT UNSIGNED NULL COMMENT '头像附件',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    token_version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'JWT失效版本',
    last_login_at DATETIME(3) NULL COMMENT '最近登录时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    UNIQUE KEY uk_sys_user_phone (phone),
    KEY idx_sys_user_status_deleted (status, deleted),
    CONSTRAINT chk_sys_user_status CHECK (status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='用户账号';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_code VARCHAR(30) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    description VARCHAR(255) NULL COMMENT '说明',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '启用状态',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_code (role_code),
    CONSTRAINT chk_sys_role_status CHECK (status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='角色';

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户',
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NULL COMMENT '分配人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_role (user_id, role_id),
    KEY idx_sys_user_role_role (role_id),
    CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id)
        REFERENCES sys_user (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id)
        REFERENCES sys_role (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='用户角色关联';

CREATE TABLE IF NOT EXISTS grade (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    grade_name VARCHAR(30) NOT NULL COMMENT '年级名称',
    grade_level TINYINT NOT NULL COMMENT '年级层级 1-3',
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_grade_name (grade_name),
    CONSTRAINT chk_grade_level CHECK (grade_level BETWEEN 1 AND 3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='年级';

CREATE TABLE IF NOT EXISTS school_class (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    grade_id BIGINT UNSIGNED NOT NULL COMMENT '年级',
    class_name VARCHAR(50) NOT NULL COMMENT '班级名称',
    academic_year VARCHAR(20) NOT NULL COMMENT '学年',
    graduation_year SMALLINT NULL COMMENT '毕业年份',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_school_class_grade_name_year (grade_id, class_name, academic_year),
    KEY idx_school_class_year (academic_year),
    CONSTRAINT fk_school_class_grade FOREIGN KEY (grade_id)
        REFERENCES grade (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='班级';

CREATE TABLE IF NOT EXISTS subject (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    subject_code VARCHAR(30) NOT NULL COMMENT '学科编码',
    subject_name VARCHAR(50) NOT NULL COMMENT '学科名称',
    default_full_score DECIMAL(6,2) NOT NULL DEFAULT 150.00 COMMENT '默认满分',
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_subject_code (subject_code),
    UNIQUE KEY uk_subject_name (subject_name),
    CONSTRAINT chk_subject_full_score CHECK (default_full_score > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='学科';

CREATE TABLE IF NOT EXISTS teacher (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '教师业务ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户账号',
    employee_no VARCHAR(30) NOT NULL COMMENT '工号',
    gender VARCHAR(10) NULL,
    title VARCHAR(50) NULL COMMENT '职称',
    specialty VARCHAR(100) NULL COMMENT '专业或主要学科',
    hire_date DATE NULL,
    office_phone VARCHAR(20) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_teacher_user (user_id),
    UNIQUE KEY uk_teacher_employee_no (employee_no),
    CONSTRAINT fk_teacher_user FOREIGN KEY (user_id)
        REFERENCES sys_user (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='教师档案';

CREATE TABLE IF NOT EXISTS student (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '学生业务ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户账号',
    student_no VARCHAR(30) NOT NULL COMMENT '学号',
    gender VARCHAR(10) NULL,
    birth_date DATE NULL,
    enrollment_date DATE NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_user (user_id),
    UNIQUE KEY uk_student_no (student_no),
    CONSTRAINT fk_student_user FOREIGN KEY (user_id)
        REFERENCES sys_user (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='学生档案';

CREATE TABLE IF NOT EXISTS parent (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '家长业务ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户账号',
    gender VARCHAR(10) NULL,
    occupation VARCHAR(100) NULL,
    emergency_contact VARCHAR(20) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_parent_user (user_id),
    CONSTRAINT fk_parent_user FOREIGN KEY (user_id)
        REFERENCES sys_user (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='家长档案';

CREATE TABLE IF NOT EXISTS parent_student (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id BIGINT UNSIGNED NOT NULL,
    student_id BIGINT UNSIGNED NOT NULL,
    relationship VARCHAR(20) NOT NULL COMMENT '父亲/母亲/监护人',
    is_primary TINYINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    verified_by BIGINT UNSIGNED NULL,
    verified_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_parent_student (parent_id, student_id),
    KEY idx_parent_student_student_status (student_id, status),
    KEY idx_parent_student_parent_status (parent_id, status),
    CONSTRAINT fk_parent_student_parent FOREIGN KEY (parent_id)
        REFERENCES parent (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_parent_student_student FOREIGN KEY (student_id)
        REFERENCES student (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_parent_student_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'REVOKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='家长学生绑定';

CREATE TABLE IF NOT EXISTS teacher_class (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    teacher_id BIGINT UNSIGNED NOT NULL,
    class_id BIGINT UNSIGNED NOT NULL,
    subject_id BIGINT UNSIGNED NULL,
    relation_type VARCHAR(20) NOT NULL COMMENT 'HOMEROOM/SUBJECT',
    start_date DATE NOT NULL,
    end_date DATE NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_teacher_class_teacher_status (teacher_id, status),
    KEY idx_teacher_class_class_status (class_id, status),
    KEY idx_teacher_class_subject (subject_id),
    CONSTRAINT fk_teacher_class_teacher FOREIGN KEY (teacher_id)
        REFERENCES teacher (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_teacher_class_class FOREIGN KEY (class_id)
        REFERENCES school_class (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_teacher_class_subject FOREIGN KEY (subject_id)
        REFERENCES subject (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_teacher_class_relation
        CHECK (relation_type IN ('HOMEROOM', 'SUBJECT')),
    CONSTRAINT chk_teacher_class_subject
        CHECK (
            (relation_type = 'HOMEROOM' AND subject_id IS NULL)
            OR (relation_type = 'SUBJECT' AND subject_id IS NOT NULL)
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='教师班级关联';

CREATE TABLE IF NOT EXISTS student_class (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    student_id BIGINT UNSIGNED NOT NULL,
    class_id BIGINT UNSIGNED NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CURRENT',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_class_date (student_id, class_id, start_date),
    KEY idx_student_class_student_status (student_id, status),
    KEY idx_student_class_class_status (class_id, status),
    CONSTRAINT fk_student_class_student FOREIGN KEY (student_id)
        REFERENCES student (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_student_class_class FOREIGN KEY (class_id)
        REFERENCES school_class (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_student_class_status
        CHECK (status IN ('CURRENT', 'ENDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='学生班级履历';

