package com.aitrainer.dao;

import com.aitrainer.db.DB;

import java.util.List;
import java.util.Map;

public class ChapterDao {
    public List<Map<String, Object>> list() {
        return DB.query(
                "SELECT id,code,title,category,sort_order,level FROM chapters ORDER BY sort_order,id");
    }

    public Map<String, Object> findById(long id) {
        return DB.queryOne("SELECT * FROM chapters WHERE id=?", id);
    }
}
