package com.aitrainer.service;
import com.aitrainer.security.*;
import com.aitrainer.config.Config;
import com.aitrainer.db.*;
import com.aitrainer.dao.*;
import com.aitrainer.entity.User;
import com.aitrainer.util.*;
import java.sql.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Real isolated MySQL migration/restore rehearsal. Never changes the application database. */
public final class SecurityTest {
    static int count;
    static void ok(boolean value,String text){if(!value)throw new AssertionError(text);count++;System.out.println("PASS "+text);}
    static void fails(Runnable r,String text){boolean fail=false;try{r.run();}catch(RuntimeException e){fail=true;}ok(fail,text);}
    public static void main(String[] args)throws Exception{
        byte[] key=new byte[32];new java.security.SecureRandom().nextBytes(key);String a=Crypto.encrypt("test","private",key),b=Crypto.encrypt("test","private",key);
        ok(!a.equals(b),"GCM nonce is unique");ok(Crypto.decrypt("test",a,key).equals("private"),"GCM roundtrip");
        fails(()->Crypto.decrypt("wrong",a,key),"column substitution rejected");byte[] damaged=Base64.getDecoder().decode(a.substring(Crypto.PREFIX.length()));damaged[15]^=1;
        fails(()->Crypto.decrypt("test",Crypto.PREFIX+Base64.getEncoder().encodeToString(damaged),key),"ciphertext tampering rejected");
        String pw="Security-Fixture-8634",hash=Passwords.hash(pw);ok(Passwords.verify(pw,hash)&&!Passwords.verify("wrong",hash),"PBKDF2 verifies correct password only");ok(!hash.equals(Passwords.hash(pw)),"password salts differ");
        fails(()->Passwords.policy("admin123"),"weak password rejected");
        fails(()->Json.parse("{\"a\":1,\"a\":2}"),"duplicate JSON keys rejected");fails(()->Json.parse("[invalid]"),"invalid JSON numbers rejected");fails(()->Json.parse("1e99999"),"non-finite numbers rejected");String nested=String.join("",Collections.nCopies(70,"["))+"0"+String.join("",Collections.nCopies(70,"]"));fails(()->Json.parse(nested),"deep JSON rejected");
        String session=SessionManager.create(999,"USER");ok(SessionManager.get(session)!=null,"opaque session valid");SessionManager.removeUser(999);ok(SessionManager.get(session)==null,"user session revocation");
        for(int i=0;i<3;i++)ok(RateLimiter.allow("fixture",3,60000),"rate allowance "+i);ok(!RateLimiter.allow("fixture",3,60000),"rate limit enforced");
        String originalUrl=Config.dbUrl(),user=Config.dbUser(),password=Config.dbPassword();
        String name="ai_security_test_"+System.currentTimeMillis(),restore=name+"_restore";
        Class.forName("com.mysql.jdbc.Driver");
        try(Connection root=DriverManager.getConnection(originalUrl,user,password)){
            try(Statement st=root.createStatement()){st.executeUpdate("CREATE DATABASE "+name+" CHARACTER SET utf8mb4");st.executeUpdate("CREATE DATABASE "+restore+" CHARACTER SET utf8mb4");}
            try{
                String url=originalUrl.replaceFirst("/[^/?]+\\?","/"+name+"?");System.setProperty("db.url",url);
                try(Connection c=DriverManager.getConnection(url,user,password)){schema(c);}
                DB.init();Migrations.run();
                DB.update("INSERT INTO users(username,password,nickname,email,role,status) VALUES(?,?,?,?,?,1)","security_fixture",Str.sha256("aitrainer:"+pw),"测试昵称","fixture@example.invalid","USER");
                DB.update("INSERT INTO settings(k,v) VALUES('ai_api_key','fixture-key-only')");
                DB.update("INSERT INTO practice_records(user_id,question_id,user_answer,is_correct,created_at) VALUES(1,1,'A',1,NOW())");
                DB.update("INSERT INTO practical_submissions(user_id,task_id,content,score,analysis) VALUES(1,1,'private code',75,'private review')");
                DB.update("INSERT INTO learning_profiles(user_id,exam_date,daily_minutes,foundation,institution,updated_at) VALUES(1,'2026-12-01',60,'beginner','科教兴川',NOW())");
                long before=System.currentTimeMillis();SecurityMigration.run();
                User learner=new UserDao().findByUsername("security_fixture");ok(learner!=null&&learner.email.equals("fixture@example.invalid"),"encrypted account lookup and PII roundtrip");ok(learner.passwordEquals(pw)&&learner.mustChangePassword,"legacy password wrapped and change required");
                ok("fixture-key-only".equals(new SettingsDao().get("ai_api_key","")),"existing API key preserved after encryption");
                ok(Json.integer(DB.queryOne("SELECT p.is_correct FROM practice_records p").get("is_correct"),0)==1,"joined encrypted numeric field restored");
                ok(Json.integer(new LearningService().profile(1).get("daily_minutes"),0)==60,"encrypted profile retains numeric type");
                for(Map.Entry<String,List<String>> e:ProtectedFields.TABLES.entrySet())for(String field:e.getValue())try(Connection c=DriverManager.getConnection(url,user,password);Statement st=c.createStatement();ResultSet rs=st.executeQuery("SELECT COUNT(*) FROM "+e.getKey()+" WHERE "+field+" IS NOT NULL AND "+field+" NOT LIKE 'ENC:v1:%'")){rs.next();ok(rs.getInt(1)==0,e.getKey()+"."+field+" at rest encrypted");}
                new SettingsDao().set("test_setting","new-value");ok("new-value".equals(new SettingsDao().get("test_setting","")),"new settings encrypted write/read");
                new AuthService().changePassword(learner,pw,"Changed-Fixture-98462");ok(!new UserDao().findById(1).mustChangePassword,"password change removes restricted state");SecurityMigration.run();ok(new UserDao().findById(1).passwordEquals("Changed-Fixture-98462"),"migration restart idempotent");
                Path vault=Paths.get(System.getProperty("security.dir","runtime/security"));Path backup;
                try(java.util.stream.Stream<Path> files=Files.list(vault)){backup=files.filter(p->p.getFileName().toString().startsWith("database-before-security-")).filter(p->{try{return Files.getLastModifiedTime(p).toMillis()>=before;}catch(Exception e){return false;}}).findFirst().orElseThrow(()->new AssertionError("missing backup"));}
                Map<String,Object> data=Json.obj(Crypto.open("database-backup",new String(Files.readAllBytes(backup),StandardCharsets.UTF_8)));
                String restoreUrl=originalUrl.replaceFirst("/[^/?]+\\?","/"+restore+"?");
                try(Connection c=DriverManager.getConnection(restoreUrl,user,password)){schema(c);try(Statement st=c.createStatement()){for(String table:Arrays.asList("learning_profiles","lesson_progress","exam_sessions","learning_reports","practical_reviews"))st.executeUpdate("CREATE TABLE "+table+" LIKE "+name+"."+table);}
                    c.setAutoCommit(false);for(Map.Entry<String,Object> e:data.entrySet())for(Object obj:Json.arr(e.getValue())){Map<String,Object> row=Json.obj(obj);String columns=String.join(",",row.keySet());String placeholders=String.join(",",Collections.nCopies(row.size(),"?"));try(PreparedStatement ps=c.prepareStatement("INSERT INTO "+e.getKey()+"("+columns+") VALUES("+placeholders+")")){int i=0;for(Object v:row.values())ps.setObject(++i,v);ps.executeUpdate();}}c.commit();
                    try(Statement st=c.createStatement();ResultSet rs=st.executeQuery("SELECT username,email FROM users WHERE id=1")){rs.next();ok(rs.getString(1).equals("security_fixture")&&rs.getString(2).equals("fixture@example.invalid"),"encrypted backup restored into isolated recovery database");}
                }
            }finally{for(String db:Arrays.asList(name,restore)){if(!db.matches("ai_security_test_[0-9]+(_restore)?"))throw new IllegalStateException("unsafe cleanup");try(Statement st=root.createStatement()){st.executeUpdate("DROP DATABASE "+db);}}}
        }
        System.out.println("ALL "+count+" SECURITY CHECKS PASSED; isolated databases removed");
    }
    static void schema(Connection c)throws Exception{String s=new String(Files.readAllBytes(Paths.get("db/schema.sql")),StandardCharsets.UTF_8).replaceAll("(?m)^--.*$","");for(String sql:s.split(";")){String q=sql.trim();if(q.isEmpty()||q.startsWith("CREATE DATABASE")||q.startsWith("USE "))continue;try(Statement st=c.createStatement()){st.execute(q);}}}
}
