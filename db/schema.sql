-- =============================================================
-- 人工智能训练师（三级）学习平台 数据库结构
-- MySQL 5.7  /  utf8mb4
-- 数据库: ai_trainer_platform
-- =============================================================
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `ai_trainer_platform`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `ai_trainer_platform`;

-- ---------------------------------------------------------
-- 用户表
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `username`      VARCHAR(64)  NOT NULL COMMENT '登录账号',
  `password`      VARCHAR(128) NOT NULL COMMENT 'SHA256(aitrainer:密码)',
  `nickname`      VARCHAR(64)  DEFAULT NULL,
  `email`         VARCHAR(128) DEFAULT NULL,
  `role`          VARCHAR(16)  NOT NULL DEFAULT 'USER' COMMENT 'ADMIN/USER',
  `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `created_at`    DATETIME     DEFAULT NULL,
  `last_login_at` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ---------------------------------------------------------
-- 理论知识章节
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `chapters` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `code`       VARCHAR(32)  DEFAULT NULL,
  `title`      VARCHAR(255) NOT NULL,
  `category`   VARCHAR(32)  NOT NULL DEFAULT 'THEORY' COMMENT 'THEORY/PRACTICAL',
  `sort_order` INT          DEFAULT 0,
  `content`    LONGTEXT,
  `level`      INT          DEFAULT 3,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='理论知识章节';

-- ---------------------------------------------------------
-- 理论题（判断/单选/多选）
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `questions` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `level`      INT          DEFAULT 3,
  `type`       VARCHAR(16)  NOT NULL COMMENT 'JUDGE/SINGLE/MULTIPLE',
  `question`   TEXT         NOT NULL,
  `options`    TEXT         COMMENT 'JSON [{label,text}]',
  `answer`     VARCHAR(32)  NOT NULL,
  `analysis`   TEXT,
  `knowledge`  VARCHAR(128) DEFAULT NULL,
  `difficulty` INT          DEFAULT 2,
  PRIMARY KEY (`id`),
  KEY `idx_q_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='理论题';

-- ---------------------------------------------------------
-- 实操题
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `practical_tasks` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT,
  `code`             VARCHAR(32)  NOT NULL,
  `title`            VARCHAR(255) NOT NULL,
  `category`         VARCHAR(64)  DEFAULT NULL,
  `level`            INT          DEFAULT 3,
  `equipment`        TEXT,
  `duration`         VARCHAR(32)  DEFAULT NULL,
  `task_desc`        LONGTEXT,
  `skills`           TEXT,
  `quality`          TEXT,
  `notes`            TEXT,
  `uploads`          TEXT         COMMENT 'JSON 上传要求',
  `reference_answer` LONGTEXT,
  `materials`        TEXT         COMMENT 'JSON 素材文件列表',
  `sort_order`       INT          DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实操题';

-- ---------------------------------------------------------
-- 练习记录
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `practice_records` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT      NOT NULL,
  `question_id` BIGINT      NOT NULL,
  `user_answer` VARCHAR(64) DEFAULT NULL,
  `is_correct`  TINYINT     DEFAULT 0,
  `created_at`  DATETIME    DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_pr_user` (`user_id`),
  KEY `idx_pr_q` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='练习记录';

-- ---------------------------------------------------------
-- 考试记录
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `exam_records` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT       NOT NULL,
  `title`        VARCHAR(128) DEFAULT NULL,
  `total`        INT          DEFAULT 0,
  `correct`      INT          DEFAULT 0,
  `score`        DOUBLE       DEFAULT 0,
  `passed`       TINYINT      DEFAULT 0,
  `duration_sec` INT          DEFAULT 0,
  `detail`       LONGTEXT     COMMENT 'JSON 答题详情',
  `created_at`   DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_er_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试记录';

-- ---------------------------------------------------------
-- 错题本
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mistakes` (
  `id`            BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT   NOT NULL,
  `question_id`   BIGINT   NOT NULL,
  `wrong_count`   INT      DEFAULT 1,
  `mastered`      TINYINT  DEFAULT 0,
  `last_wrong_at` DATETIME DEFAULT NULL,
  `created_at`    DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mistake_user_q` (`user_id`, `question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本';

-- ---------------------------------------------------------
-- 实操题提交
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `practical_submissions` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT      NOT NULL,
  `task_id`    BIGINT      NOT NULL,
  `content`    LONGTEXT    COMMENT '用户提交的答案/代码',
  `score`      DOUBLE      DEFAULT NULL,
  `analysis`   LONGTEXT    COMMENT 'AI Agent 评分分析',
  `status`     VARCHAR(16) DEFAULT 'GRADED',
  `created_at` DATETIME    DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ps_user` (`user_id`),
  KEY `idx_ps_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实操题提交';

-- ---------------------------------------------------------
-- 登录日志
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `login_logs` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT       DEFAULT NULL,
  `username`   VARCHAR(64)  DEFAULT NULL,
  `ip`         VARCHAR(64)  DEFAULT NULL,
  `user_agent` VARCHAR(512) DEFAULT NULL,
  `success`    TINYINT      DEFAULT 0,
  `created_at` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ll_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';

-- ---------------------------------------------------------
-- 操作日志
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `operation_logs` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT       DEFAULT NULL,
  `username`   VARCHAR(64)  DEFAULT NULL,
  `action`     VARCHAR(64)  DEFAULT NULL,
  `target`     VARCHAR(128) DEFAULT NULL,
  `detail`     VARCHAR(512) DEFAULT NULL,
  `ip`         VARCHAR(64)  DEFAULT NULL,
  `created_at` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ol_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- ---------------------------------------------------------
-- 系统设置
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `settings` (
  `k` VARCHAR(64) NOT NULL,
  `v` TEXT,
  PRIMARY KEY (`k`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统设置';
