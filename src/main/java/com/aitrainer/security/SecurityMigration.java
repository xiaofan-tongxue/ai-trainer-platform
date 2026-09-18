package com.aitrainer.security;
import com.aitrainer.db.DB;
import com.aitrainer.util.Json;
import java.sql.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Offline, restartable conversion. DDL is not transactional in MySQL 5.7. */
public final class SecurityMigration {
    public static void run(){
        DB.update("CREATE TABLE IF NOT EXISTS security_migrations(version VARCHAR(40) PRIMARY KEY, completed_at DATETIME NOT NULL)");
        DB.update("CREATE TABLE IF NOT EXISTS auth_failures(account_key CHAR(64) PRIMARY KEY,failures INT NOT NULL,blocked_until DATETIME NULL,last_attempt DATETIME NOT NULL)");
        if(DB.count("SELECT COUNT(*) FROM security_migrations WHERE version='field-v1'")>0)return;
        Crypto.mac("startup","check");
        try{
            Path backup=Paths.get(System.getProperty("security.dir","runtime/security"),"database-before-security-"+System.currentTimeMillis()+".enc");
            Map<String,Object> original=new LinkedHashMap<String,Object>();
            // Back up all tables containing learner records, not just the converted columns.
            for(String table:Arrays.asList("users","settings","practice_records","exam_records","mistakes","practical_submissions","login_logs","operation_logs","learning_profiles","lesson_progress","exam_sessions","learning_reports","practical_reviews"))original.put(table,DB.query("SELECT * FROM "+table));
            String sealed=Crypto.seal("database-backup",original);
            if(!Json.stringify(original).equals(Json.stringify(Crypto.open("database-backup",sealed))))throw new IllegalStateException("备份验证失败");
            Files.write(backup,sealed.getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE_NEW);
            if(DB.count("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='users' AND column_name='username_lookup'")==0)DB.update("ALTER TABLE users ADD username_lookup CHAR(64) CHARACTER SET ascii NULL, ADD must_change_password TINYINT NOT NULL DEFAULT 1");
            if(DB.count("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='users' AND index_name='uk_username'")>0)DB.update("ALTER TABLE users DROP INDEX uk_username");
            DB.update("ALTER TABLE users MODIFY password VARCHAR(255) NOT NULL");
            for(Map.Entry<String,List<String>> entry:ProtectedFields.TABLES.entrySet())for(String field:entry.getValue())DB.update("ALTER TABLE "+entry.getKey()+" MODIFY "+field+" LONGTEXT NULL");
            DB.tx(c->{
                for(Map.Entry<String,List<String>> entry:ProtectedFields.TABLES.entrySet()){
                    String table=entry.getKey(),pk=table.equals("settings")?"k":table.equals("learning_profiles")?"user_id":"id";
                    if(table.equals("lesson_progress"))pk="user_id";
                    try(Statement st=c.createStatement();ResultSet rs=st.executeQuery("SELECT * FROM "+table)){
                        while(rs.next()){
                            Object id=rs.getObject(pk);String extra=table.equals("lesson_progress")?" AND lesson_id=?":"";
                            for(String field:entry.getValue()){
                                Object raw=rs.getObject(field);if(raw==null)continue;String value=raw.toString();
                                if(value.startsWith(Crypto.PREFIX)){Crypto.open(table+"."+field,value);continue;}
                                Object typed=value;
                                if(Arrays.asList("score").contains(field))typed=Double.valueOf(value);
                                else if(Arrays.asList("is_correct","total","correct","passed","duration_sec","daily_minutes").contains(field))typed=Long.valueOf(value);
                                try(PreparedStatement ps=c.prepareStatement("UPDATE "+table+" SET "+field+"=? WHERE "+pk+"=?"+extra)){ps.setString(1,ProtectedFields.seal(table,field,typed));ps.setObject(2,id);if(!extra.isEmpty())ps.setString(3,rs.getString("lesson_id"));ps.executeUpdate();}
                            }
                            if(table.equals("users")){
                                String account=rs.getString("username");if(account.startsWith(Crypto.PREFIX))account=String.valueOf(Crypto.open("users.username",account));
                                String pass=rs.getString("password");if(pass.matches("[0-9a-fA-F]{64}"))pass=Passwords.wrapLegacy(pass);
                                try(PreparedStatement ps=c.prepareStatement("UPDATE users SET username_lookup=?,password=? WHERE id=?")){ps.setString(1,Crypto.lookup(account));ps.setString(2,pass);ps.setObject(3,id);ps.executeUpdate();}
                            }
                        }
                    }
                }
                return null;
            });
            if(DB.count("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='users' AND index_name='uk_username_lookup'")==0)DB.update("ALTER TABLE users ADD UNIQUE KEY uk_username_lookup(username_lookup)");
            DB.update("INSERT INTO security_migrations(version,completed_at) VALUES('field-v1',NOW())");
            System.out.println("[OK] 敏感字段迁移完成，加密备份校验通过");
        }catch(Exception e){throw new IllegalStateException("安全迁移未完成，禁止启动；请查看安全迁移运行手册",e);}
    }
    public static void main(String[] args){DB.init();com.aitrainer.db.Migrations.run();run();System.out.println("Protected field inventory: "+ProtectedFields.TABLES.size()+" tables");}
}
