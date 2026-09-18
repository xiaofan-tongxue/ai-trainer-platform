package com.aitrainer.dao;

import com.aitrainer.db.DB;

import java.util.List;
import java.util.Map;
import static com.aitrainer.security.ProtectedFields.seal;

public class LogDao {
    public void loginLog(Long userId, String username, String ip, String ua, int success) {
        DB.update(
                "INSERT INTO login_logs(user_id,username,ip,user_agent,success,created_at) VALUES(?,?,?,?,?,NOW())",
                userId, seal("login_logs","username",username), seal("login_logs","ip",ip), seal("login_logs","user_agent",ua), success);
    }

    public void operationLog(Long userId, String username, String action, String target, String detail, String ip) {
        DB.update(
                "INSERT INTO operation_logs(user_id,username,action,target,detail,ip,created_at) VALUES(?,?,?,?,?,?,NOW())",
                userId, seal("operation_logs","username",username), action, seal("operation_logs","target",target), seal("operation_logs","detail",detail), seal("operation_logs","ip",ip));
    }

    public List<Map<String, Object>> loginLogs(int limit) {
        return DB.query("SELECT * FROM login_logs ORDER BY id DESC LIMIT ?", Math.max(1,Math.min(500,limit)));
    }

    public List<Map<String, Object>> operationLogs(int limit) {
        return DB.query("SELECT * FROM operation_logs ORDER BY id DESC LIMIT ?", Math.max(1,Math.min(500,limit)));
    }

    public long loginLogCount() {
        return DB.count("SELECT COUNT(*) FROM login_logs");
    }

    public long operationLogCount() {
        return DB.count("SELECT COUNT(*) FROM operation_logs");
    }
}
