package com.aitrainer.dao;

import com.aitrainer.db.DB;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SettingsDao {
    public Map<String, String> all() {
        Map<String, String> m = new LinkedHashMap<String, String>();
        List<Map<String, Object>> rows = DB.query("SELECT k,v FROM settings");
        for (Map<String, Object> r : rows) m.put(String.valueOf(r.get("k")), r.get("v") == null ? "" : r.get("v").toString());
        return m;
    }

    public String get(String key, String def) {
        Map<String, Object> m = DB.queryOne("SELECT v FROM settings WHERE k=?", key);
        return m == null || m.get("v") == null ? def : m.get("v").toString();
    }

    public void set(String key, String value) {
        DB.update("INSERT INTO settings(k,v) VALUES(?,?) ON DUPLICATE KEY UPDATE v=VALUES(v)", key, com.aitrainer.security.ProtectedFields.seal("settings","v",value));
    }
}
