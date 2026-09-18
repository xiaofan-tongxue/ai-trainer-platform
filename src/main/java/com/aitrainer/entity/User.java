package com.aitrainer.entity;

import com.aitrainer.util.Str;

import java.util.Map;

public class User {
    public long id;
    public String username;
    public String passwordHash; // 不对外暴露
    public String nickname;
    public String email;
    public String role;
    public int status;
    public boolean mustChangePassword;
    public String createdAt;
    public String lastLoginAt;

    public static User from(Map<String, Object> m) {
        if (m == null) return null;
        User u = new User();
        u.id = num(m.get("id"));
        u.username = str(m.get("username"));
        u.passwordHash = str(m.get("password"));
        u.nickname = str(m.get("nickname"));
        u.email = str(m.get("email"));
        u.role = str(m.get("role"));
        u.status = (int) num(m.get("status"));
        u.mustChangePassword=num(m.get("must_change_password"))!=0;
        u.createdAt = str(m.get("created_at"));
        u.lastLoginAt = str(m.get("last_login_at"));
        return u;
    }

    public boolean passwordEquals(String plain) {
        return com.aitrainer.security.Passwords.verify(plain,passwordHash);
    }

    public Map<String, Object> toSafe() {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<String, Object>();
        m.put("id", id);
        m.put("username", username);
        m.put("nickname", nickname);
        m.put("email", email);
        m.put("role", role);
        m.put("status", status);
        m.put("mustChangePassword",mustChangePassword);
        m.put("createdAt", createdAt);
        m.put("lastLoginAt", lastLoginAt);
        return m;
    }

    private static long num(Object o) { return o instanceof Number ? ((Number) o).longValue() : 0L; }
    private static String str(Object o) { return o == null ? null : o.toString(); }
}
