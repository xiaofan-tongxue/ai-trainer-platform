package com.aitrainer.service;

import com.aitrainer.dao.UserDao;
import com.aitrainer.entity.User;
import com.aitrainer.util.SessionManager;
import com.aitrainer.util.Str;

import java.util.Map;
import com.aitrainer.security.Passwords;
import com.aitrainer.security.RateLimiter;
import com.aitrainer.security.Crypto;
import com.aitrainer.db.DB;

public class AuthService {
    private final UserDao dao = new UserDao();
    private final LogService logs = new LogService();

    public static class AuthResult {
        public boolean ok;
        public String message;
        public User user;
        public String token;
    }

    public AuthResult login(String username, String password, String ip, String ua) {
        AuthResult r = new AuthResult();
        if(username==null||username.length()>64||password==null||password.length()>128){r.message="用户名或密码错误";return r;}
        String key=Crypto.lookup(username);
        if(DB.count("SELECT COUNT(*) FROM auth_failures WHERE account_key=? AND blocked_until>NOW()",key)>0){r.message="认证失败或尝试次数过多，请15分钟后重试";return r;}
        if (Str.empty(username) || Str.empty(password)) {
            r.message = "用户名和密码不能为空";
            logs.login(null, username, ip, ua, 0);
            return r;
        }
        User u = dao.findByUsername(username);
        if (u == null || !u.passwordEquals(password)) {
            r.message = "用户名或密码错误";
            if(u==null)Passwords.verify(password,DUMMY_HASH);
            DB.update("INSERT INTO auth_failures(account_key,failures,blocked_until,last_attempt) VALUES(?,1,NULL,NOW()) ON DUPLICATE KEY UPDATE failures=IF(last_attempt<DATE_SUB(NOW(),INTERVAL 15 MINUTE),1,failures+1),blocked_until=IF(failures>=5,DATE_ADD(NOW(),INTERVAL 15 MINUTE),NULL),last_attempt=NOW()",key);
            logs.login(u == null ? null : u.id, username, ip, ua, 0);
            return r;
        }
        if (u.status == 0) {
            r.message = "账号已被禁用，请联系管理员";
            logs.login(u.id, username, ip, ua, 0);
            return r;
        }
        dao.updateLastLogin(u.id);
        DB.update("DELETE FROM auth_failures WHERE account_key=?",key);
        r.ok = true;
        r.user = u;
        r.token = SessionManager.create(u.id, u.role);
        logs.login(u.id, username, ip, ua, 1);
        return r;
    }

    public AuthResult register(String username, String password, String nickname, String email, String ip) {
        AuthResult r = new AuthResult();
        if (Str.empty(username) || Str.empty(password)) {
            r.message = "用户名和密码不能为空";
            return r;
        }
        validateRegistration(username,password,nickname,email);
        if (dao.findByUsername(username) != null) {
            r.message = "用户名已存在";
            return r;
        }
        long id = dao.insert(username, Str.hashPassword(password),
                Str.nvl(nickname, username), email, "USER");
        r.ok = true;
        r.user = dao.findById(id);
        r.token = SessionManager.create(id, "USER");
        logs.op(id, username, "REGISTER", "user", "注册新用户", ip);
        return r;
    }

    public User current(long userId) {
        return dao.findById(userId);
    }
    private static final String DUMMY_HASH=Passwords.hash("non-account-timing-baseline-2864");
    public static void validateRegistration(String username,String password,String nickname,String email){
        if(username==null||!username.matches("[A-Za-z0-9_]{3,64}"))throw new IllegalArgumentException("用户名为3–64位字母、数字或下划线");
        Passwords.policy(password);
        if(nickname!=null&&nickname.length()>64)throw new IllegalArgumentException("昵称最多64字");
        if(email!=null&&(!email.isEmpty()&&(!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")||email.length()>128)))throw new IllegalArgumentException("邮箱格式不正确");
    }
    public void changePassword(User u,String oldPassword,String newPassword){
        if(!u.passwordEquals(oldPassword))throw new IllegalArgumentException("当前密码错误");
        Passwords.policy(newPassword);if(newPassword.equals(oldPassword))throw new IllegalArgumentException("新密码不能与当前密码相同");
        dao.updatePassword(u.id,Passwords.hash(newPassword));
    }
}
