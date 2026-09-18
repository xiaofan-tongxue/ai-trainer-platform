package com.aitrainer.service;

import com.aitrainer.dao.MistakeDao;

import java.util.List;
import java.util.Map;

public class MistakeService {
    private final MistakeDao dao = new MistakeDao();

    public List<Map<String, Object>> list(long userId, boolean onlyUnmastered) {
        return dao.listByUser(userId, onlyUnmastered);
    }

    public void remove(long userId, long questionId) {
        dao.remove(userId, questionId);
    }

    public Map<String, Object> stats(long userId) {
        return dao.stats(userId);
    }

    public List<Map<String, Object>> all() {
        return dao.listAll();
    }
}
