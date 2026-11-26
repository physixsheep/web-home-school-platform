USE home_school_platform;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT UNSIGNED NULL COMMENT '操作用户',
    username_snapshot VARCHAR(50) NULL COMMENT '用户名快照',
    role_snapshot VARCHAR(100) NULL COMMENT '角色快照',
    module VARCHAR(50) NOT NULL COMMENT '业务模块',
    operation VARCHAR(100) NOT NULL COMMENT '操作名称',
    http_method VARCHAR(10) NOT NULL COMMENT 'HTTP方法',
    request_uri VARCHAR(255) NOT NULL COMMENT '请求路径',
    request_params TEXT NULL COMMENT '脱敏查询参数',
    result_code INT NOT NULL COMMENT 'HTTP状态码',
    client_ip VARCHAR(64) NULL COMMENT '客户端IP',
    user_agent VARCHAR(500) NULL COMMENT '客户端标识',
    duration_ms BIGINT NOT NULL COMMENT '耗时毫秒',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_operation_log_user_time (user_id, created_at),
    KEY idx_operation_log_module_time (module, created_at),
    KEY idx_operation_log_result_time (result_code, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='操作日志';

