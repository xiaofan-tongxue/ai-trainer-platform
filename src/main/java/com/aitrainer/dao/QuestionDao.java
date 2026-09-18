package com.aitrainer.dao;

import com.aitrainer.db.DB;
import com.aitrainer.util.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuestionDao {
    /** 返回带解析后 options 的题目 */
    public static Map<String, Object> decorate(Map<String, Object> q) {
        if (q == null) return null;
        Object o = q.get("options");
        if (o instanceof String && !((String) o).isEmpty()) {
            try { q.put("options", Json.parse((String) o)); }
            catch (Exception ignore) { q.put("options", new ArrayList<Object>()); }
        } else if (o == null) {
            q.put("options", new ArrayList<Object>());
        }
        return q;
    }

    public List<Map<String,Object>> allTagged() {
        List<Map<String,Object>> list=DB.query("SELECT * FROM questions ORDER BY id");
        for(Map<String,Object> q:list){decorate(q);q.put("domain",com.aitrainer.service.Knowledge.of(q));}
        return list;
    }

    public static Map<String,Object> publicView(Map<String,Object> q) {
        Map<String,Object> r=new java.util.LinkedHashMap<String,Object>(q);
        r.remove("answer");r.remove("analysis");return r;
    }

    public Map<String, Object> findById(long id) {
        return decorate(DB.queryOne("SELECT * FROM questions WHERE id=?", id));
    }

    public List<Map<String, Object>> findByType(String type, int offset, int size) {
        List<Map<String, Object>> list = DB.query(
                "SELECT * FROM questions WHERE type=? ORDER BY id LIMIT ? OFFSET ?", type, size, offset);
        for (Map<String, Object> m : list) decorate(m);
        return list;
    }

    public long countByType(String type) {
        return DB.count("SELECT COUNT(*) FROM questions WHERE type=?", type);
    }

    /** 随机抽取指定数量指定类型的题目 */
    public List<Map<String, Object>> randomByType(String type, int n) {
        List<Map<String, Object>> list = DB.query(
                "SELECT * FROM questions WHERE type=? ORDER BY RAND() LIMIT ?", type, n);
        for (Map<String, Object> m : list) decorate(m);
        return list;
    }

    public List<Map<String, Object>> byIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<Map<String, Object>>();
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) in.append(i == 0 ? "?" : ",?");
        List<Map<String, Object>> list = DB.query(
                "SELECT * FROM questions WHERE id IN (" + in + ") ORDER BY id",
                ids.toArray(new Object[0]));
        for (Map<String, Object> m : list) decorate(m);
        return list;
    }

    public long totalCount() {
        return DB.count("SELECT COUNT(*) FROM questions");
    }
}
