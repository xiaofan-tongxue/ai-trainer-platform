package com.aitrainer.service;

import com.aitrainer.dao.ExamDao;
import com.aitrainer.dao.LogDao;
import com.aitrainer.dao.MistakeDao;
import com.aitrainer.dao.PracticalDao;
import com.aitrainer.dao.QuestionDao;
import com.aitrainer.dao.UserDao;
import com.aitrainer.db.DB;
import com.aitrainer.util.Str;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdminService {
    private final UserDao udao = new UserDao();
    private final QuestionDao qdao = new QuestionDao();
    private final ExamDao edao = new ExamDao();
    private final MistakeDao mdao = new MistakeDao();
    private final PracticalDao pdao = new PracticalDao();
    private final LogDao ldao = new LogDao();

    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("users", udao.count());
        m.put("questions", qdao.totalCount());
        m.put("practical", pdao.count());
        m.put("exams", edao.count());
        m.put("submissions", pdao.submissionCount());
        m.put("loginLogs", ldao.loginLogCount());
        m.put("operationLogs", ldao.operationLogCount());
        return m;
    }

    public List<Map<String, Object>> users(String keyword) {
        return udao.list(keyword);
    }

    public Map<String, Object> createUser(String username, String password, String nickname, String email, String role) {
        Map<String, Object> r = new LinkedHashMap<String, Object>();
        if (Str.empty(username) || Str.empty(password)) { r.put("error", "用户名和密码不能为空"); return r; }
        AuthService.validateRegistration(username,password,nickname,email);
        if (udao.findByUsername(username) != null) { r.put("error", "用户名已存在"); return r; }
        String rl = "ADMIN".equals(role) ? "ADMIN" : "USER";
        long id = udao.insert(username, Str.hashPassword(password),
                Str.nvl(nickname, username), email, rl);
        r.put("id", id);
        r.put("ok", true);
        return r;
    }

    public void setStatus(long id, int status) { if(status!=0&&status!=1)throw new IllegalArgumentException("账号状态无效");protectLastAdmin(id,status==0);udao.updateStatus(id, status);com.aitrainer.util.SessionManager.removeUser(id); }
    public void setRole(long id, String role) { if(!"ADMIN".equals(role)&&!"USER".equals(role))throw new IllegalArgumentException("角色无效");protectLastAdmin(id,!"ADMIN".equals(role));udao.updateRole(id,role);com.aitrainer.util.SessionManager.removeUser(id); }
    public void resetPassword(long id, String password) {
        com.aitrainer.security.Passwords.policy(password);udao.updatePassword(id, Str.hashPassword(password));
        DB.update("UPDATE users SET must_change_password=1 WHERE id=?",id);
    }
    public void deleteUser(long id) { protectLastAdmin(id,true);udao.delete(id);com.aitrainer.util.SessionManager.removeUser(id); }
    private void protectLastAdmin(long id,boolean remove){if(remove&&DB.count("SELECT COUNT(*) FROM users WHERE id=? AND role='ADMIN' AND status=1",id)>0&&DB.count("SELECT COUNT(*) FROM users WHERE role='ADMIN' AND status=1")<=1)throw new IllegalArgumentException("必须保留至少一个启用的管理员");}

    public List<Map<String, Object>> loginLogs(int limit) { return ldao.loginLogs(limit); }
    public List<Map<String, Object>> operationLogs(int limit) { return ldao.operationLogs(limit); }
    public List<Map<String, Object>> allMistakes() { return mdao.listAll(); }
    public List<Map<String, Object>> allSubmissions() { return pdao.allSubmissions(); }
}
