package com.aitrainer.util;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Hashed opaque tokens; 30-minute idle timeout and 8-hour absolute lifetime. */
public final class SessionManager {
    private static final Map<String,Session> STORE=new ConcurrentHashMap<String,Session>();
    public static final class Session {public long userId,expire,absolute;public String role,csrf;}
    public static String random(){byte[] b=new byte[32];new SecureRandom().nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    public static synchronized String create(long uid,String role){
        long now=System.currentTimeMillis();STORE.entrySet().removeIf(e->e.getValue().expire<=now||e.getValue().absolute<=now);
        removeUser(uid);if(STORE.size()>=10000)throw new IllegalStateException("会话容量已满");
        String token=random();Session s=new Session();s.userId=uid;s.role=role;s.expire=now+1800000;s.absolute=now+28800000;s.csrf=random();STORE.put(Str.sha256(token),s);return token;
    }
    public static Session get(String token){if(token==null||!token.matches("[A-Za-z0-9_-]{43}"))return null;String key=Str.sha256(token);Session s=STORE.get(key);long now=System.currentTimeMillis();if(s==null)return null;if(now>=s.expire||now>=s.absolute){STORE.remove(key);return null;}s.expire=Math.min(now+1800000,s.absolute);return s;}
    public static void remove(String token){if(token!=null)STORE.remove(Str.sha256(token));}
    public static void removeUser(long uid){STORE.entrySet().removeIf(e->e.getValue().userId==uid);}
}
