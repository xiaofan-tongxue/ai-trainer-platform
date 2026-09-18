package com.aitrainer.db;

import com.aitrainer.config.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * 极简连接池 + 查询工具。
 */
public final class DB {
    private static final int POOL_SIZE = 8;
    private static final BlockingQueue<Connection> POOL = new ArrayBlockingQueue<Connection>(POOL_SIZE);
    private static volatile boolean inited = false;

    private DB() {}

    public static synchronized void init() {
        if (inited) return;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            for (int i = 0; i < POOL_SIZE; i++) POOL.offer(open());
            inited = true;
        } catch (Exception e) {
            throw new RuntimeException("数据库初始化失败: " + e.getMessage(), e);
        }
    }

    private static Connection open() throws SQLException {
        return DriverManager.getConnection(Config.dbUrl(), Config.dbUser(), Config.dbPassword());
    }

    public static Connection conn() {
        try {
            Connection c = POOL.take();
            if (c == null || c.isClosed()) c = open();
            return c;
        } catch (Exception e) {
            try { return open(); } catch (SQLException ex) {
                throw new RuntimeException("获取数据库连接失败: " + ex.getMessage(), ex);
            }
        }
    }

    public static void release(Connection c) {
        if (c == null) return;
        try {
            if (!c.getAutoCommit()) c.setAutoCommit(true);
            POOL.offer(c);
        } catch (SQLException e) {
            try { c.close(); } catch (SQLException ignore) {}
        }
    }

    public static void close(Connection c, PreparedStatement ps, ResultSet rs) {
        if (rs != null) { try { rs.close(); } catch (SQLException ignore) {} }
        if (ps != null) { try { ps.close(); } catch (SQLException ignore) {} }
        release(c);
    }

    /* ---------- 查询工具 ---------- */

    public interface RowMapper<T> { T map(ResultSet rs) throws SQLException; }

    /** 把 ResultSet 当前行转成 Map（key 用列名） */
    public static Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        int n = rs.getMetaData().getColumnCount();
        for (int i = 1; i <= n; i++) {
            String name = rs.getMetaData().getColumnLabel(i);
            Object v = rs.getObject(i);
            // Connector/J 8 returns LocalDateTime for DATETIME; keep the existing SQL timestamp wire format.
            if (v instanceof java.time.LocalDateTime) v = java.sql.Timestamp.valueOf((java.time.LocalDateTime)v).toString();
            if (v instanceof java.time.LocalDate) v = v.toString();
            if (v instanceof java.sql.Timestamp) v = v.toString();
            if (v instanceof java.sql.Date) v = v.toString();
            if (v instanceof byte[]) v = new String((byte[]) v);
            if(v instanceof String&&((String)v).startsWith(com.aitrainer.security.Crypto.PREFIX))v=com.aitrainer.security.Crypto.open(rs.getMetaData().getTableName(i)+"."+rs.getMetaData().getColumnName(i),(String)v);
            m.put(name, v);
        }
        return m;
    }

    public static List<Map<String, Object>> query(String sql, Object... params) {
        Connection c = conn();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = c.prepareStatement(sql);
            bind(ps, params);
            rs = ps.executeQuery();
            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            while (rs.next()) list.add(rowToMap(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("查询失败: " + e.getMessage(), e);
        } finally {
            close(c, ps, rs);
        }
    }

    public static Map<String, Object> queryOne(String sql, Object... params) {
        List<Map<String, Object>> list = query(sql, params);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 执行更新，返回自增主键（若无自增返回受影响行数） */
    public static long update(String sql, Object... params) {
        Connection c = conn();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            bind(ps, params);
            int n = ps.executeUpdate();
            try {
                rs = ps.getGeneratedKeys();
                if (rs.next()) return rs.getLong(1);
            } catch (SQLException ignore) {}
            return n;
        } catch (SQLException e) {
            throw new RuntimeException("更新失败: " + e.getMessage(), e);
        } finally {
            close(c, ps, rs);
        }
    }

    public static long count(String sql, Object... params) {
        Map<String, Object> m = queryOne(sql, params);
        if (m == null || m.values().isEmpty()) return 0;
        Object v = m.values().iterator().next();
        if (v == null) return 0;
        return ((Number) v).longValue();
    }

    private static void bind(PreparedStatement ps, Object[] params) throws SQLException {
        if (params == null) return;
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p == null) ps.setObject(i + 1, null);
            else if (p instanceof Integer) ps.setInt(i + 1, (Integer) p);
            else if (p instanceof Long) ps.setLong(i + 1, (Long) p);
            else if (p instanceof Double) ps.setDouble(i + 1, (Double) p);
            else if (p instanceof Boolean) ps.setBoolean(i + 1, (Boolean) p);
            else ps.setString(i + 1, p.toString());
        }
    }

    /** 事务包装 */
    public interface Tx<T> { T run(Connection c) throws Exception; }

    public static <T> T tx(Tx<T> fn) {
        Connection c = conn();
        try {
            c.setAutoCommit(false);
            T r = fn.run(c);
            c.commit();
            return r;
        } catch (Exception e) {
            try { c.rollback(); } catch (SQLException ignore) {}
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            try { c.setAutoCommit(true); } catch (SQLException ignore) {}
            release(c);
        }
    }
}
