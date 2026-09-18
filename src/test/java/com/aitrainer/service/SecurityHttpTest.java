package com.aitrainer.service;
import com.aitrainer.db.DB;
import com.aitrainer.dao.UserDao;
import com.aitrainer.security.*;
import com.aitrainer.util.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;
import static com.aitrainer.service.Curriculum.map;

/** Bounded local DAST, three disposable identities; no paid AI calls. */
public final class SecurityHttpTest {
    static final String BASE=System.getProperty("test.baseUrl","http://127.0.0.1:19001");
    static List<Object> results=new ArrayList<Object>();
    static Map<String,Object> call(String method,String path,String token,String cookie,String csrf,String raw,Map<String,String> headers)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(BASE+path).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(false);c.setRequestMethod(method);
        if(token!=null)c.setRequestProperty("Authorization","Bearer "+token);if(cookie!=null)c.setRequestProperty("Cookie",cookie);if(csrf!=null)c.setRequestProperty("X-CSRF-Token",csrf);
        if(raw!=null)c.setRequestProperty("Content-Type","application/json");for(Map.Entry<String,String> h:headers.entrySet())c.setRequestProperty(h.getKey(),h.getValue());
        if(raw!=null){c.setDoOutput(true);c.getOutputStream().write(raw.getBytes("UTF-8"));}
        int status=c.getResponseCode();InputStream input=status>=400?c.getErrorStream():c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream();if(input!=null){byte[] b=new byte[4096];int n;while((n=input.read(b))!=-1)out.write(b,0,n);input.close();}
        Map<String,Object> r=map("status",status,"body",new String(out.toByteArray(),"UTF-8"),"cookie",c.getHeaderField("Set-Cookie"),"nosniff",c.getHeaderField("X-Content-Type-Options"),"csp",c.getHeaderField("Content-Security-Policy"));c.disconnect();return r;
    }
    static Map<String,Object> request(String method,String path,String token,Object body)throws Exception{return call(method,path,token,null,null,body==null?null:Json.stringify(body),Collections.emptyMap());}
    static Map<String,Object> data(Map<String,Object> r){return Json.obj(Json.obj(Json.parse(Json.str(r.get("body")))).get("data"));}
    static boolean status(Map<String,Object> r,int n){return Json.integer(r.get("status"),0)==n;}
    static void check(boolean ok,String name){results.add(map("check",name,"passed",ok));System.out.println((ok?"PASS ":"FAIL ")+name);if(!ok)throw new AssertionError(name);}
    public static void main(String[] args)throws Exception{
        System.setProperty("sun.net.http.allowRestrictedHeaders","true");
        DB.init();List<Long> ids=new ArrayList<Long>();List<String> accounts=new ArrayList<String>();String suffix=String.valueOf(System.currentTimeMillis());UserDao dao=new UserDao();String pw="Security-Http-86349";
        try{
            for(String role:Arrays.asList("USER","USER","ADMIN")){String account="sec_"+ids.size()+"_"+suffix;accounts.add(account);ids.add(dao.insert(account,Passwords.hash(pw),"安全复测", "fixture@example.invalid",role));}
            Map<String,Object> home=request("GET","/index.html",null,null);check(status(home,200)&&"nosniff".equals(home.get("nosniff"))&&Json.str(home.get("csp")).contains("frame-ancestors 'none'"),"security response headers");
            for(String path:Arrays.asList("/admin.html.bak","/js/common.js.bak","/.env","/%2e%2e/db/schema.sql"))check(!status(request("GET",path,null,null),200),"backup and traversal blocked: "+path);
            check(status(request("POST","/index.html",null,map()),405),"static method allowlist");
            check(status(request("GET","/api/admin/users",null,null),401),"anonymous admin API denied");
            check(status(request("GET","/api/learning/report",null,null),401),"anonymous report denied");
            check(status(request("GET","/api/questions?page=1&page=2",null,null),400),"HTTP parameter pollution rejected");
            check(status(call("POST","/api/auth/login",null,null,null,"{\"username\":\"x\",\"username\":\"y\"}",Collections.emptyMap()),400),"duplicate auth JSON rejected");
            check(status(call("POST","/api/auth/login",null,null,null,"{}",Collections.singletonMap("Content-Type","text/plain")),415),"content type confusion rejected");
            check(status(call("POST","/api/auth/login",null,null,null,"{}",Collections.singletonMap("Origin","https://untrusted.invalid")),403),"cross origin rejected");
            Map<String,Object> logged=call("POST","/api/auth/login",null,null,null,Json.stringify(map("username",accounts.get(0),"password",pw)),Collections.singletonMap("X-Session-Mode","cookie"));
            Map<String,Object> first=data(logged);String cookie=Json.str(logged.get("cookie")),csrf=Json.str(first.get("csrf"));check(status(logged,200)&&cookie.contains("HttpOnly")&&cookie.contains("SameSite=Strict")&&"cookie".equals(first.get("token")),"HttpOnly browser cookie with no bearer exposure");cookie=cookie.split(";")[0];
            check(status(call("POST","/api/learning/profile",null,cookie,null,"{}",Collections.emptyMap()),403),"cookie CSRF without token rejected");
            check(status(call("POST","/api/learning/profile",null,cookie,csrf,"{\"daily_minutes\":60}",Collections.emptyMap()),200),"same origin CSRF protected write succeeds");
            check(status(call("GET","/api/admin/users",null,cookie,null,null,Collections.emptyMap()),403),"ordinary user function level authorization");
            String second=Json.str(data(request("POST","/api/auth/login",null,map("username",accounts.get(1),"password",pw))).get("token"));
            Map<String,Object> paper=data(request("POST","/api/exam/generate?mode=diagnostic",second,map()));String sessionId=Json.str(paper.get("sessionId"));
            check(!status(call("GET","/api/exam/session/"+sessionId,null,cookie,null,null,Collections.emptyMap()),200),"cross account exam object access denied");
            check(status(request("GET","/api/exam/generate?mode=diagnostic",second,null),405),"state changing GET removed");
            check(status(request("GET","/api/questions/ignored/suffix",second,null),405),"unconsumed route segments rejected");
            check(status(request("POST","/api/chapters",second,map()),405),"read only endpoint method enforced");
            check(!Json.str(request("GET","/api/questions?type=ALL&size=1000000",second,null).get("body")).contains("\"answer\":"),"question API avoids answer exposure");
            Map<String,Object> limited=data(request("GET","/api/questions?size=1000000",second,null));check(Json.arr(limited.get("list")).size()<=100,"pagination bound enforced");
            DB.update("UPDATE users SET must_change_password=1 WHERE id=?",ids.get(1));
            check(status(request("GET","/api/questions",second,null),403),"legacy or reset credentials restricted to password update");
            check(status(request("POST","/api/auth/password",second,map("oldPassword",pw,"newPassword","Changed-Secure-94751")),200),"old credentials can update password");
            check(status(request("GET","/api/auth/me",second,null),401),"password change revokes bearer session");
            String admin=Json.str(data(request("POST","/api/auth/login",null,map("username",accounts.get(2),"password",pw))).get("token"));
            check(status(request("GET","/api/admin/users",admin,null),200),"administrator authorized access");
            check(!Json.str(request("GET","/api/admin/ai",admin,null).get("body")).contains("apiKey"),"admin config response never returns API key");
            check(status(request("PUT","/api/admin/user/"+ids.get(0),admin,map("status",0)),200),"admin account disable succeeds");
            check(status(call("GET","/api/auth/me",null,cookie,null,null,Collections.emptyMap()),401),"disabled account cookie revoked");
            check(status(request("PUT","/api/admin/user/"+ids.get(2),admin,map("role","UNKNOWN")),400),"role enum validation");
            for(Map.Entry<String,List<String>> e:ProtectedFields.TABLES.entrySet())for(String field:e.getValue())check(DB.count("SELECT COUNT(*) FROM "+e.getKey()+" WHERE "+field+" IS NOT NULL AND "+field+" NOT LIKE 'ENC:v1:%'")==0,"active database encrypted: "+e.getKey()+"."+field);
            // Bounded lockout check, no brute-force dictionary and no real account involved.
            for(int i=0;i<5;i++)request("POST","/api/auth/login",null,map("username",accounts.get(1),"password","wrong-credential"));
            check(DB.count("SELECT COUNT(*) FROM auth_failures WHERE account_key=? AND blocked_until>NOW()",Crypto.lookup(accounts.get(1)))==1,"persistent five failure account lockout");
        }finally{
            for(long id:ids){for(String table:Arrays.asList("learning_reports","lesson_progress","learning_profiles","exam_sessions","exam_records","practice_records","mistakes","login_logs","operation_logs"))DB.update("DELETE FROM "+table+" WHERE user_id=?",id);DB.update("DELETE FROM users WHERE id=?",id);SessionManager.removeUser(id);}
            for(String account:accounts)DB.update("DELETE FROM auth_failures WHERE account_key=?",Crypto.lookup(account));
            Path out=Paths.get("docs/security/evidence");Files.createDirectories(out);Files.write(out.resolve("http-security-results.json"),Json.stringify(map("testedAt",java.time.Instant.now().toString(),"target",BASE,"results",results)).getBytes("UTF-8"));
        }
        System.out.println("ALL "+results.size()+" HTTP SECURITY CHECKS PASSED; fixtures removed");
    }
}
