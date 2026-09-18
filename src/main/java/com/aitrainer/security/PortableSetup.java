package com.aitrainer.security;

import com.aitrainer.db.DB;
import com.aitrainer.dao.UserDao;

/** Fresh private installation only. Credentials come from the installer process environment. */
public final class PortableSetup {
    public static void main(String[] args) {
        if(!"YES".equals(System.getenv("AITRAINER_FRESH_INSTALL")))throw new IllegalStateException("Fresh-install marker required");
        DB.init();
        com.aitrainer.db.Migrations.run();
        SecurityMigration.run();
        UserDao users=new UserDao();
        if(users.findByUsername("admin")==null){
            String password=System.getenv("SETUP_ADMIN_PASSWORD");
            Passwords.policy(password);
            users.insert("admin",Passwords.hash(password),"平台管理员","","ADMIN");
            DB.update("UPDATE users SET must_change_password=1 WHERE username_lookup=?",Crypto.lookup("admin"));
        }
        if(DB.count("SELECT COUNT(*) FROM questions")!=900)throw new IllegalStateException("Question seed verification failed");
        System.out.println("Fresh database, encrypted fields and unique initial administrator verified");
    }
}
