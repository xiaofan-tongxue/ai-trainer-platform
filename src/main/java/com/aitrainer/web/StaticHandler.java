package com.aitrainer.web;

import com.aitrainer.config.Config;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StaticHandler {
    public static void serve(Request req, Response res) {
        try {
            if(!req.method.equals("GET")&&!req.method.equals("HEAD")){res.fail(405,"方法不支持");return;}
            String path = req.path;
            File webapp = new File(Config.staticDir());
            Path base = webapp.getCanonicalFile().toPath();

            String rel;
            if (path.equals("/") || path.isEmpty()) rel = "index.html";
            else if (path.equals("/login")) rel = "login.html";
            else rel = path.startsWith("/") ? path.substring(1) : path;
            if(rel.startsWith(".")||rel.contains("/.")||rel.matches("(?i).*(\\.bak|\\.map|\\.sql|\\.env|\\.java|\\.class|\\.zip|\\.dpapi|~)$")){res.fail(404,"资源不存在");return;}

            // 无扩展名的页面路由 -> 尝试 .html
            Path target = base.resolve(rel).normalize();
            if (!target.startsWith(base)) { res.fail(403, "非法路径"); return; }
            File f = target.toFile();
            if (!f.exists() && !rel.contains(".")) {
                File alt = base.resolve(rel + ".html").normalize().toFile();
                if (alt.exists()) f = alt;
            }
            if (!f.exists() || !f.isFile()) {
                res.text(404, "text/html; charset=utf-8", "<h1>404 Not Found</h1>");
                return;
            }

            String name = f.getName().toLowerCase();
            if(!f.toPath().toRealPath().startsWith(base.toRealPath())){res.fail(403,"非法路径");return;}
            if(!name.matches(".*\\.(html|css|js|png|jpg|jpeg|ico|woff|woff2|ttf)$")){res.fail(404,"资源不存在");return;}
            if (isText(name)) {
                String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                res.text(200, contentType(name), text);
            } else {
                byte[] data = Files.readAllBytes(f.toPath());
                res.bytes(200, contentType(name), data);
            }
        } catch (Exception e) {
            res.fail(500, "静态资源读取失败");
        }
    }

    private static boolean isText(String n) {
        return n.endsWith(".html") || n.endsWith(".css") || n.endsWith(".js")
                || n.endsWith(".json") || n.endsWith(".svg") || n.endsWith(".txt")
                || n.endsWith(".md") || n.endsWith(".map");
    }

    private static String contentType(String n) {
        if (n.endsWith(".html")) return "text/html; charset=utf-8";
        if (n.endsWith(".css")) return "text/css; charset=utf-8";
        if (n.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (n.endsWith(".json")) return "application/json; charset=utf-8";
        if (n.endsWith(".svg")) return "image/svg+xml";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".ico")) return "image/x-icon";
        if (n.endsWith(".woff")) return "font/woff";
        if (n.endsWith(".woff2")) return "font/woff2";
        if (n.endsWith(".ttf")) return "font/ttf";
        return "application/octet-stream";
    }
}
