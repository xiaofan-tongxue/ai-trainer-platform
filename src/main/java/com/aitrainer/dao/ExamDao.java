package com.aitrainer.dao;

import com.aitrainer.db.DB;

import java.util.List;
import java.util.Map;
import static com.aitrainer.security.ProtectedFields.seal;

public class ExamDao {
    public long insert(long userId, String title, int total, int correct, double score, int passed,
                       int durationSec, String detail) {
        return DB.update(
                "INSERT INTO exam_records(user_id,title,total,correct,score,passed,duration_sec,detail,created_at) " +
                "VALUES(?,?,?,?,?,?,?,?,NOW())",
                userId, title, seal("exam_records","total",total), seal("exam_records","correct",correct), seal("exam_records","score",score), seal("exam_records","passed",passed), seal("exam_records","duration_sec",durationSec), seal("exam_records","detail",detail));
    }

    public List<Map<String, Object>> listByUser(long userId) {
        return DB.query(
                "SELECT id,title,total,correct,score,passed,duration_sec,created_at FROM exam_records " +
                "WHERE user_id=? ORDER BY id DESC LIMIT 200", userId);
    }

    public Map<String, Object> findById(long id) {
        return DB.queryOne("SELECT * FROM exam_records WHERE id=?", id);
    }

    public long count() {
        return DB.count("SELECT COUNT(*) FROM exam_records");
    }
}
