-- 测试平台建表脚本（幂等：可重复执行）
CREATE DATABASE IF NOT EXISTS test_platform DEFAULT CHARSET utf8mb4;
USE test_platform;

CREATE TABLE IF NOT EXISTS sys_user (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(32)  NOT NULL,
    password   VARCHAR(128) NOT NULL COMMENT 'sha256(salt + 明文)',
    salt       VARCHAR(32)  NOT NULL,
    nickname   VARCHAR(32),
    role       VARCHAR(16)  NOT NULL DEFAULT 'USER' COMMENT 'USER / ADMIN',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '用户';

CREATE TABLE IF NOT EXISTS project (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL,
    description VARCHAR(255),
    owner_id    BIGINT       NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_owner (owner_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '项目';

CREATE TABLE IF NOT EXISTS env (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id   BIGINT       NOT NULL,
    name         VARCHAR(32)  NOT NULL COMMENT 'dev / test / prod',
    base_url     VARCHAR(255) NOT NULL,
    headers_json TEXT,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_project (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '环境配置';

CREATE TABLE IF NOT EXISTS api (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id   BIGINT       NOT NULL,
    name         VARCHAR(64)  NOT NULL,
    method       VARCHAR(10)  NOT NULL COMMENT 'GET/POST/PUT/DELETE',
    path         VARCHAR(255) NOT NULL,
    headers_json TEXT,
    body         TEXT,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_project (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '接口定义';

CREATE TABLE IF NOT EXISTS test_case (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT       NOT NULL,
    name        VARCHAR(64)  NOT NULL,
    description VARCHAR(255),
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_project (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '测试用例';

CREATE TABLE IF NOT EXISTS test_step (
    id               BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    case_id          BIGINT   NOT NULL,
    sort             INT      NOT NULL DEFAULT 1,
    api_id           BIGINT   NOT NULL,
    override_headers TEXT     COMMENT '覆盖接口 Header（JSON）',
    override_body    TEXT     COMMENT '覆盖接口 Body',
    extractors_json  TEXT     COMMENT '变量提取规则：[{"name":"token","path":"$.data.token"}]',
    assertions_json  TEXT     COMMENT '断言规则：[{"type":"status","expect":200}]',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_case (case_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '用例步骤';

CREATE TABLE IF NOT EXISTS run (
    id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT      NOT NULL,
    case_id     BIGINT      NOT NULL,
    env_id      BIGINT      NOT NULL,
    status      VARCHAR(16) NOT NULL COMMENT 'RUNNING/PASSED/FAILED/ERROR',
    total       INT         NOT NULL DEFAULT 0,
    passed      INT         NOT NULL DEFAULT 0,
    failed      INT         NOT NULL DEFAULT 0,
    duration_ms INT         NOT NULL DEFAULT 0,
    started_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at DATETIME    NULL,
    error_msg   VARCHAR(500),
    KEY idx_case_time (case_id, started_at),
    KEY idx_project (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '执行记录';

CREATE TABLE IF NOT EXISTS run_detail (
    id                 BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    run_id             BIGINT      NOT NULL,
    step_id            BIGINT      NOT NULL,
    sort               INT         NOT NULL,
    status             VARCHAR(16) NOT NULL COMMENT 'PASSED/FAILED/ERROR',
    request_snapshot   TEXT        COMMENT '实际发出的请求',
    response_snapshot  TEXT        COMMENT '响应（截断 4KB）',
    assertions_result  TEXT        COMMENT '断言明细',
    duration_ms        INT         NOT NULL DEFAULT 0,
    error_msg          VARCHAR(500),
    KEY idx_run (run_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '执行明细';
