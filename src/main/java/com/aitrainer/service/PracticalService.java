package com.aitrainer.service;

import com.aitrainer.dao.PracticalDao;
import com.aitrainer.util.Json;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PracticalService {
    private final PracticalDao dao = new PracticalDao();
    private final AiAgentService ai = new AiAgentService();

    public List<Map<String, Object>> list() {
        return dao.list();
    }

    /** 题目详情：学习模式含参考答案；答题模式不含（由前端控制展示） */
    public Map<String, Object> detail(long id) {
        Map<String, Object> t = dao.findById(id);
        if (t == null) return null;
        return t;
    }

    /** 提交作答，AI Agent 评分 */
    public Map<String, Object> submit(long userId, long taskId, String content) {
        if(content==null || content.trim().isEmpty() || content.length()>30000) throw new IllegalArgumentException("作答长度须为1至30000字符");
        Map<String, Object> t = dao.findById(taskId);
        if (t == null) {
            Map<String, Object> e = new LinkedHashMap<String, Object>();
            e.put("error", "题目不存在");
            return e;
        }
        AiAgentService.GradeResult g = ai.grade(
                String.valueOf(t.get("task_desc")),
                String.valueOf(t.get("reference_answer")),
                content);

        long sid = dao.insertSubmission(userId, taskId, content, g.score, g.analysis);
        com.aitrainer.db.DB.update("INSERT INTO practical_reviews(submission_id,grading_mode) VALUES(?,?)",sid,g.mode);
        Map<String, Object> r = new LinkedHashMap<String, Object>();
        r.put("submissionId", sid);
        r.put("taskCode", t.get("code"));
        r.put("taskTitle", t.get("title"));
        r.put("score", g.score);
        r.put("mode", g.mode);
        r.put("comment", g.comment);
        r.put("covered", g.covered);
        r.put("missing", g.missing);
        r.put("analysis", g.analysis);
        return r;
    }

    public List<Map<String, Object>> submissions(long userId) {
        return dao.submissionsByUser(userId);
    }
}
