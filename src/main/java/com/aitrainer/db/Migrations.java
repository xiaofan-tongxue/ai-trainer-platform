package com.aitrainer.db;

/** Additive, idempotent upgrade. Original records and configuration are preserved. */
public final class Migrations {
    private Migrations() {}
    public static void run() {
        DB.update("CREATE TABLE IF NOT EXISTS learning_profiles (user_id BIGINT PRIMARY KEY, exam_date DATE NULL, daily_minutes INT NOT NULL DEFAULT 60, foundation VARCHAR(20) NOT NULL DEFAULT 'beginner', institution VARCHAR(160) NOT NULL DEFAULT '', updated_at DATETIME NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        DB.update("CREATE TABLE IF NOT EXISTS lesson_progress (user_id BIGINT NOT NULL, lesson_id VARCHAR(64) NOT NULL, status VARCHAR(20) NOT NULL, score DOUBLE NOT NULL DEFAULT 0, attempts INT NOT NULL DEFAULT 1, updated_at DATETIME NOT NULL, PRIMARY KEY(user_id,lesson_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        DB.update("CREATE TABLE IF NOT EXISTS exam_sessions (id VARCHAR(64) PRIMARY KEY, user_id BIGINT NOT NULL, mode VARCHAR(20) NOT NULL, title VARCHAR(128) NOT NULL, paper LONGTEXT NOT NULL, answers LONGTEXT NULL, result LONGTEXT NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', started_at BIGINT NOT NULL, deadline_at BIGINT NOT NULL, submitted_at DATETIME NULL, INDEX idx_session_user(user_id,submitted_at)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        DB.update("CREATE TABLE IF NOT EXISTS learning_reports (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, evidence_hash VARCHAR(64) NOT NULL, mode VARCHAR(24) NOT NULL, report LONGTEXT NOT NULL, created_at DATETIME NOT NULL, INDEX idx_report_user(user_id,created_at)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        DB.update("CREATE TABLE IF NOT EXISTS practical_reviews (submission_id BIGINT PRIMARY KEY, grading_mode VARCHAR(24) NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }
}
