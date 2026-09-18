package com.aitrainer.service;

import com.aitrainer.dao.LogDao;

public class LogService {
    private final LogDao dao = new LogDao();

    public void login(Long userId, String username, String ip, String ua, int success) {
        dao.loginLog(userId, username, ip, ua, success);
    }

    public void op(Long userId, String username, String action, String target, String detail, String ip) {
        dao.operationLog(userId, username, action, target, detail, ip);
    }
}
