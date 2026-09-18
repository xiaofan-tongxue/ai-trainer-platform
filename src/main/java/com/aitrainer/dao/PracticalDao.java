package com.aitrainer.dao;

import com.aitrainer.db.DB;
import com.aitrainer.util.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static com.aitrainer.security.ProtectedFields.seal;

public class PracticalDao {
    public static Map<String, Object> decorate(Map<String, Object> t) {
        if (t == null) return null;
        for (String k : new String[]{"uploads", "materials"}) {
            Object o = t.get(k);
            if (o instanceof String && !((String) o).isEmpty()) {
                try { t.put(k, Json.parse((String) o)); }
                catch (Exception ignore) { t.put(k, new ArrayList<Object>()); }
            } else if (o == null) {
                t.put(k, new ArrayList<Object>());
            }
        }
        return t;
    }

    public List<Map<String, Object>> list() {
        List<Map<String, Object>> list = DB.query(
                "SELECT id,code,title,category,duration,level,sort_order FROM practical_tasks ORDER BY sort_order,id");
        return list;
    }

    public Map<String, Object> findById(long id) {
        return decorate(DB.queryOne("SELECT * FROM practical_tasks WHERE id=?", id));
    }

    public long insertSubmission(long userId, long taskId, String content, double score, String analysis) {
        return DB.update(
                "INSERT INTO practical_submissions(user_id,task_id,content,score,analysis,status,created_at) VALUES(?,?,?,?,?,'GRADED',NOW())",
                userId, taskId, seal("practical_submissions","content",content), seal("practical_submissions","score",score), seal("practical_submissions","analysis",analysis));
    }

    public List<Map<String, Object>> submissionsByUser(long userId) {
        return DB.query(
                "SELECT ps.*, pt.code, pt.title, COALESCE(pr.grading_mode,'legacy') grading_mode FROM practical_submissions ps " +
                "LEFT JOIN practical_reviews pr ON pr.submission_id=ps.id " +
                "LEFT JOIN practical_tasks pt ON pt.id=ps.task_id " +
                "WHERE ps.user_id=? ORDER BY ps.id DESC", userId);
    }

    public List<Map<String, Object>> allSubmissions() {
        return DB.query(
                "SELECT ps.*, u.username, pt.code, pt.title FROM practical_submissions ps " +
                "LEFT JOIN users u ON u.id=ps.user_id LEFT JOIN practical_tasks pt ON pt.id=ps.task_id " +
                "ORDER BY ps.id DESC LIMIT 500");
    }

    public long count() {
        return DB.count("SELECT COUNT(*) FROM practical_tasks");
    }

    public long submissionCount() {
        return DB.count("SELECT COUNT(*) FROM practical_submissions");
    }
}
