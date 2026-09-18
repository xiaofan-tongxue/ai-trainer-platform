package com.aitrainer.dao;

import com.aitrainer.db.DB;

import java.util.List;
import java.util.Map;

public class MistakeDao {
    /** 记录一次答错（存在则累加） */
    public void record(long userId, long questionId) {
        DB.update("INSERT INTO mistakes(user_id,question_id,wrong_count,mastered,last_wrong_at,created_at) VALUES(?,?,1,0,NOW(),NOW()) ON DUPLICATE KEY UPDATE wrong_count=wrong_count+1,mastered=0,last_wrong_at=NOW()",userId,questionId);
    }

    /** 答对时标记掌握 */
    public void markMastered(long userId, long questionId) {
        DB.update("UPDATE mistakes SET mastered=1 WHERE user_id=? AND question_id=?", userId, questionId);
    }

    public void remove(long userId, long questionId) {
        DB.update("DELETE FROM mistakes WHERE user_id=? AND question_id=?", userId, questionId);
    }

    public List<Map<String, Object>> listByUser(long userId, boolean onlyUnmastered) {
        String sql = "SELECT m.*, q.type, q.question, q.options, q.answer, q.analysis FROM mistakes m " +
                "JOIN questions q ON q.id=m.question_id WHERE m.user_id=?";
        if (onlyUnmastered) sql += " AND m.mastered=0";
        sql += " ORDER BY m.last_wrong_at DESC";
        List<Map<String, Object>> list = DB.query(sql, userId);
        for (Map<String, Object> m : list) QuestionDao.decorate(m);
        return list;
    }

    /** 管理员查看全部错题 */
    public List<Map<String, Object>> listAll() {
        List<Map<String, Object>> list = DB.query(
                "SELECT m.*, u.username, u.nickname, q.type, q.question, q.answer FROM mistakes m " +
                "JOIN users u ON u.id=m.user_id JOIN questions q ON q.id=m.question_id " +
                "ORDER BY m.last_wrong_at DESC LIMIT 500");
        return list;
    }

    public Map<String, Object> stats(long userId) {
        Map<String, Object> m = DB.queryOne(
                "SELECT COALESCE(SUM(mastered=0),0) AS total, COALESCE(SUM(mastered=1),0) AS mastered, COALESCE(SUM(wrong_count),0) AS wrong_times FROM mistakes WHERE user_id=?",
                userId);
        return m;
    }

    public long countByUser(long userId) {
        return DB.count("SELECT COUNT(*) FROM mistakes WHERE user_id=? AND mastered=0", userId);
    }
}
