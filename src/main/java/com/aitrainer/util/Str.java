package com.aitrainer.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.UUID;

public final class Str {
    private Str() {}

    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final SecureRandom RAND = new SecureRandom();

    public static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(b.length * 2);
            for (byte x : b) sb.append(HEX[(x >> 4) & 0xf]).append(HEX[x & 0xf]);
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** New passwords use unique-salt PBKDF2-HMAC-SHA256. */
    public static String hashPassword(String password) {
        return com.aitrainer.security.Passwords.hash(password);
    }

    public static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] b = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(b.length * 2);
            for (byte x : b) sb.append(HEX[(x >> 4) & 0xf]).append(HEX[x & 0xf]);
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String token() {
        return UUID.randomUUID().toString().replace("-", "") + Long.toHexString(RAND.nextLong());
    }

    public static String urlDecode(String s) {
        if (s == null) return null;
        try { return URLDecoder.decode(s, "UTF-8"); }
        catch (UnsupportedEncodingException e) { return s; }
    }

    public static boolean empty(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static String nvl(String s, String def) {
        return s == null || s.isEmpty() ? def : s;
    }

    /** 转义 HTML，防止 XSS */
    public static String html(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
