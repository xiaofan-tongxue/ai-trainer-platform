package com.aitrainer.security;
import com.aitrainer.config.Config;
import com.aitrainer.util.*;
import java.sql.*;
import static com.aitrainer.service.Curriculum.map;
/** Called by provision-database.ps1; JSON credentials must be captured, never logged. */
public final class ProvisionDatabase {
    public static void main(String[] args)throws Exception{
        String url=Config.dbUrl();String base=url.split("\\?",2)[0];String database=base.substring(base.lastIndexOf('/')+1);if(!database.matches("[A-Za-z0-9_]+"))throw new IllegalArgumentException("database name invalid");
        String account="aitrainer_"+System.currentTimeMillis(),password=SessionManager.random()+"9a";
        Class.forName("com.mysql.jdbc.Driver");
        try(Connection c=DriverManager.getConnection(url,Config.dbUser(),Config.dbPassword())){
            try{
            for(String host:new String[]{"localhost","127.0.0.1"}){
                try(PreparedStatement p=c.prepareStatement("CREATE USER '"+account+"'@'"+host+"' IDENTIFIED BY ?")){p.setString(1,password);p.executeUpdate();}
                try(Statement s=c.createStatement()){s.executeUpdate("GRANT SELECT,INSERT,UPDATE,DELETE ON `"+database+"`.* TO '"+account+"'@'"+host+"'");}
            }
        try(Connection check=DriverManager.getConnection(url,account,password);Statement s=check.createStatement()){
            s.executeQuery("SELECT COUNT(*) FROM users").close();boolean denied=false;
            try{s.executeUpdate("CREATE TABLE security_should_be_denied(id INT)");}catch(SQLException e){denied=true;}
            if(!denied){s.executeUpdate("DROP TABLE security_should_be_denied");throw new IllegalStateException("runtime account unexpectedly permits DDL");}
        }
            }catch(Exception failure){for(String host:new String[]{"localhost","127.0.0.1"})try(Statement s=c.createStatement()){s.executeUpdate("DROP USER IF EXISTS '"+account+"'@'"+host+"'");}catch(SQLException cleanup){failure.addSuppressed(cleanup);}throw failure;}
        }
        System.out.print(Json.stringify(map("username",account,"password",password)));
    }
}
