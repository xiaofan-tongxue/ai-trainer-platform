package com.aitrainer.dao;

import com.aitrainer.db.DB;
import com.aitrainer.entity.User;

import java.util.List;
import java.util.Map;
import static com.aitrainer.security.ProtectedFields.seal;

public class UserDao {
    public User findByUsername(String username) {
        Map<String, Object> m = DB.queryOne(
                "SELECT * FROM users WHERE username_lookup=? LIMIT 1", com.aitrainer.security.Crypto.lookup(username));
        return User.from(m);
    }

    public User findById(long id) {
        Map<String, Object> m = DB.queryOne("SELECT * FROM users WHERE id=?", id);
        return User.from(m);
    }

    public long insert(String username, String passwordHash, String nickname, String email, String role) {
        return DB.update(
                "INSERT INTO users(username,password,nickname,email,role,username_lookup,must_change_password,status,created_at) VALUES(?,?,?,?,?,?,0,1,NOW())",
                seal("users","username",username), passwordHash, seal("users","nickname",nickname), seal("users","email",email), role, com.aitrainer.security.Crypto.lookup(username));
    }

    public void updateLastLogin(long id) {
        DB.update("UPDATE users SET last_login_at=NOW() WHERE id=?", id);
    }

    public List<Map<String, Object>> list(String keyword) {
        String term=keyword==null?"":keyword.toLowerCase(java.util.Locale.ROOT);
        if(term.length()>128)throw new IllegalArgumentException("查询条件过长");
        List<Map<String,Object>> rows=DB.query("SELECT id,username,nickname,email,role,status,created_at,last_login_at FROM users ORDER BY id LIMIT 500");
        rows.removeIf(r->!(String.valueOf(r.get("username"))+" "+String.valueOf(r.get("nickname"))+" "+String.valueOf(r.get("email"))).toLowerCase(java.util.Locale.ROOT).contains(term));
        return rows;
    }

    public void updateStatus(long id, int status) {
        DB.update("UPDATE users SET status=? WHERE id=?", status, id);
    }

    public void updateRole(long id, String role) {
        DB.update("UPDATE users SET role=? WHERE id=?", role, id);
    }

    public void updatePassword(long id, String passwordHash) {
        DB.update("UPDATE users SET password=?,must_change_password=0 WHERE id=?", passwordHash, id);
        com.aitrainer.util.SessionManager.removeUser(id);
    }

    public void updateProfile(long id, String nickname, String email) {
        DB.update("UPDATE users SET nickname=?, email=? WHERE id=?", seal("users","nickname",nickname), seal("users","email",email), id);
    }

    public void delete(long id) {
        DB.update("DELETE FROM users WHERE id=?", id);
    }

    public long count() {
        return DB.count("SELECT COUNT(*) FROM users");
    }
}
