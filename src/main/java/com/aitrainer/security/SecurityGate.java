package com.aitrainer.security;
import com.aitrainer.web.*;
import com.aitrainer.util.SessionManager;
import java.net.URI;

public final class SecurityGate {
    public static boolean production(){return "production".equals(System.getenv("APP_ENV"));}
    public static String origin(){String s=System.getenv("PUBLIC_ORIGIN");return s==null?"":s;}
    public static boolean check(Request req,Response res){
        String host=req.header("host");
        String expected=origin();
        boolean hostOk=!expected.isEmpty()?URI.create(expected).getRawAuthority().equalsIgnoreCase(host):host!=null&&host.matches("(?i)(localhost|127\\.0\\.0\\.1|\\[::1\\])(:[0-9]{1,5})?");
        if(!hostOk){res.fail(421,"请求主机不匹配");return false;}
        if(req.rawPath.length()>2048||req.path.contains("\\")||req.path.contains("\u0000")||req.path.contains("//")){res.fail(400,"请求路径无效");return false;}
        if(!req.path.startsWith("/api/"))return true;
        if(!RateLimiter.allow("ip:"+req.ip(),360,60000)){res.header("Retry-After","60");res.fail(429,"请求过于频繁，请稍后再试");return false;}
        if(req.path.startsWith("/api/auth/")&&!req.method.equals("GET")&&!RateLimiter.allow("auth:"+req.ip(),15,60000)){res.header("Retry-After","60");res.fail(429,"认证请求过于频繁");return false;}
        String incoming=req.header("origin");String allowed=expected.isEmpty()?"http://"+host:expected;
        if(incoming!=null&&!allowed.equals(incoming)){res.forbidden();return false;}
        if("cross-site".equals(req.header("sec-fetch-site"))){res.forbidden();return false;}
        boolean write=!req.method.equals("GET")&&!req.method.equals("HEAD");
        if(write&& !req.path.equals("/api/auth/logout")){
            String ct=req.header("content-type");if(ct==null||!ct.toLowerCase().startsWith("application/json")){res.fail(415,"仅支持application/json");return false;}
        }
        if(write&&req.cookie("ait_session")!=null){SessionManager.Session s=SessionManager.get(req.bearerToken());if(s!=null&&!java.security.MessageDigest.isEqual(s.csrf.getBytes(java.nio.charset.StandardCharsets.UTF_8),String.valueOf(req.header("x-csrf-token")).getBytes(java.nio.charset.StandardCharsets.UTF_8))){res.fail(403,"CSRF校验失败，请重新登录");return false;}}
        return true;
    }
}
