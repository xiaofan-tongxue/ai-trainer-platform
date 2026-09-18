package com.aitrainer.service;

import com.aitrainer.dao.MistakeDao;
import com.aitrainer.dao.QuestionDao;
import com.aitrainer.db.DB;
import com.aitrainer.util.Json;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class QuestionService {
    private final QuestionDao qdao = new QuestionDao();
    private final MistakeDao mdao = new MistakeDao();

    /** 判题：返回是否正确 */
    public static boolean judge(String type, String correct, String userAnswer) {
        if (correct == null || userAnswer == null) return false;
        if ("JUDGE".equals(type)) {
            return normalizeJudge(correct).equals(normalizeJudge(userAnswer));
        }
        if ("MULTIPLE".equals(type)) {
            return sortChars(correct).equals(sortChars(userAnswer));
        }
        return correct.trim().equalsIgnoreCase(userAnswer.trim());
    }

    private static String normalizeJudge(String s) {
        String t = s == null ? "" : s.trim();
        String u = t.toUpperCase();
        if ("√".equals(t) || "对".equals(t) || "T".equals(u) || "TRUE".equals(u) || "1".equals(t) || "是".equals(t) || "Y".equals(u) || "YES".equals(u)) return "√";
        if ("×".equals(t) || "✕".equals(t) || "✗".equals(t) || "错".equals(t) || "F".equals(u) || "FALSE".equals(u) || "0".equals(t) || "否".equals(t) || "N".equals(u) || "NO".equals(u)) return "×";
        return t; // 未知输入原样返回，不会与正确值匹配
    }

    private static String sortChars(String s) {
        char[] a = s.trim().toUpperCase().toCharArray();
        Arrays.sort(a);
        return new String(a);
    }

    /** 练习作答：判题 + 记录练习 + 记录错题 */
    public Map<String, Object> answer(long userId, long questionId, String userAnswer) {
        if(userAnswer==null||userAnswer.trim().isEmpty()||userAnswer.length()>32)throw new IllegalArgumentException("请选择有效答案");
        Map<String, Object> q = qdao.findById(questionId);
        if (q == null) return err("题目不存在");
        String type = String.valueOf(q.get("type"));
        String correct = String.valueOf(q.get("answer"));
        boolean ok = judge(type, correct, userAnswer);

        DB.update(
                "INSERT INTO practice_records(user_id,question_id,user_answer,is_correct,created_at) VALUES(?,?,?,?,NOW())",
                userId, questionId, com.aitrainer.security.ProtectedFields.seal("practice_records","user_answer",userAnswer), com.aitrainer.security.ProtectedFields.seal("practice_records","is_correct",ok ? 1 : 0));
        if (ok) {
            mdao.markMastered(userId, questionId);
        } else {
            mdao.record(userId, questionId);
        }

        Map<String, Object> r = new LinkedHashMap<String, Object>();
        r.put("correct", ok);
        r.put("correctAnswer", correct);
        r.put("analysis", q.get("analysis"));
        r.put("type", type);
        return r;
    }

    private Map<String, Object> err(String msg) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("error", msg);
        return m;
    }
}
