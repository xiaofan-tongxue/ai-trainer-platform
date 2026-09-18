package com.aitrainer.config;
import java.io.File;
import com.aitrainer.security.Secrets;
public final class Config {
    private Config(){}
    private static String env(String n,String fallback){String v=System.getenv(n);return v==null||v.isEmpty()?fallback:v;}
    public static int port(){return Integer.getInteger("port",19001);}
    public static String dbUrl(){return System.getProperty("db.url",env("DB_URL","jdbc:mysql://127.0.0.1:3306/ai_trainer_platform?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"));}
    public static String dbUser(){String value=System.getProperty("db.user",System.getenv("DB_USER"));if(value!=null&&!value.isEmpty())return value;return java.nio.file.Files.exists(java.nio.file.Paths.get(System.getProperty("security.dir","runtime/security"),"db-user.dpapi"))?Secrets.get("db-user","DB_USER"):"root";}
    public static String dbPassword(){String p=System.getProperty("db.password");return p!=null?p:Secrets.get("db-password","DB_PASSWORD");}
    public static String staticDir(){return System.getProperty("webapp.dir",new File("webapp").getAbsolutePath());}
    public static String materialsBase(){return System.getProperty("materials.dir",new File("materials").getAbsolutePath());}
}
