package com.aitrainer;

import com.aitrainer.config.Config;
import com.aitrainer.db.DB;
import com.aitrainer.web.ApiHandler;
import com.aitrainer.web.Request;
import com.aitrainer.web.Response;
import com.aitrainer.web.StaticHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * 人工智能训练师（三级）学习平台 — 启动入口
 * 嵌入式 HTTP 服务器，端口 19001，Java 8。
 */
public class Main {
    public static void main(String[] args) throws IOException {
        System.out.println("==============================================");
        System.out.println("  人工智能训练师（三级）学习平台 启动中...");
        System.out.println("==============================================");

        DB.init();
        if(DB.count("SELECT COUNT(*) FROM security_migrations WHERE version='field-v1'")!=1)throw new IllegalStateException("请先运行安全迁移工具");
        if(com.aitrainer.security.SecurityGate.production()&&(!com.aitrainer.security.SecurityGate.origin().startsWith("https://")||"root".equalsIgnoreCase(Config.dbUser())))throw new IllegalStateException("生产模式要求HTTPS入口与独立最小权限数据库账号");
        System.out.println("[OK] 数据库及安全迁移状态已检查");

        final ApiHandler api = new ApiHandler();
        System.setProperty("sun.net.httpserver.maxReqTime","20");
        System.setProperty("sun.net.httpserver.maxRspTime","180");
        System.setProperty("sun.net.httpserver.maxReqHeaders","60");
        HttpServer server = HttpServer.create(new InetSocketAddress(System.getProperty("bind.address","127.0.0.1"),Config.port()), 64);
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                Response res = new Response(exchange);
                Request req=null;
                try{req=new Request(exchange);if(com.aitrainer.security.SecurityGate.check(req,res)){
                    if(req.path.startsWith("/api/"))api.handle(req,res);else StaticHandler.serve(req,res);
                }}catch(IllegalArgumentException e){res.fail(400,"请求格式错误");}catch(Exception e){res.fail(500,"服务暂时不可用");}
                finally{if(req!=null&&req.path.startsWith("/api/"))com.aitrainer.security.SecurityAudit.record(req,res.statusCode());exchange.close();}
            }
        });
        server.setExecutor(new java.util.concurrent.ThreadPoolExecutor(16,16,0,java.util.concurrent.TimeUnit.SECONDS,new java.util.concurrent.ArrayBlockingQueue<Runnable>(64),new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()));
        server.start();

        System.out.println("[OK] 服务已启动: http://localhost:" + Config.port());
        System.out.println("[OK] 登录门户:   http://localhost:" + Config.port() + "/login");
        System.out.println("[OK] 旧账号首次登录后必须修改密码");
        System.out.println("[OK] 静态目录:   " + Config.staticDir());
        System.out.println("[OK] 素材目录:   " + Config.materialsBase());
        System.out.println("==============================================");
    }
}
