package com.aitrainer.web;

import com.aitrainer.config.Config;
import com.aitrainer.dao.PracticalDao;
import com.aitrainer.entity.User;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class MaterialHandler {
    private static final PracticalDao dao = new PracticalDao();

    public static void serve(Request req, Response res, User user, long taskId) {
        try {
            Map<String, Object> task = dao.findById(taskId);
            if (task == null) { res.fail(404, "题目不存在"); return; }
            String code = String.valueOf(task.get("code"));
            String file = req.query.get("file");
            if (file == null || file.isEmpty()) { res.fail(400, "缺少 file 参数"); return; }

            File base = new File(new File(Config.materialsBase()), code);
            Path basePath = base.getCanonicalFile().toPath();
            Path target = basePath.resolve(file).normalize().toAbsolutePath();
            if (!target.startsWith(basePath)) { res.fail(403, "非法路径"); return; }
            if(Files.exists(target)&&!target.toRealPath().startsWith(basePath)){res.fail(403,"非法路径");return;}
            File f = target.toFile();
            if (!f.exists() || !f.isFile()) { res.fail(404, "素材文件不存在: " + file); return; }

            String name = f.getName().toLowerCase();
            String contentType = contentType(name);
            boolean inline = isPreviewable(name);

            if (isText(name)) {
                // 文本文件：UTF-8 预览（失败则按二进制下载）
                try {
                    byte[] data = Files.readAllBytes(f.toPath());
                    String text = new String(data, StandardCharsets.UTF_8);
                    res.text(200, "text/plain; charset=utf-8", text);
                    return;
                } catch (Exception e) {
                    // fall through to binary
                }
            }

            byte[] data = Files.readAllBytes(f.toPath());
            String cd = inline ? "inline" : "attachment; filename*=UTF-8''" + urlEncode(f.getName());
            res.bytes(200, contentType, data);
            // 无法直接设置 Content-Disposition，通过字节响应发送即可
        } catch (Exception e) {
            res.fail(500, "素材读取失败: " + e.getMessage());
        }
    }

    private static boolean isText(String name) {
        return name.endsWith(".csv") || name.endsWith(".txt") || name.endsWith(".json")
                || name.endsWith(".md") || name.endsWith(".py") || name.endsWith(".ipynb")
                || name.endsWith(".html") || name.endsWith(".xml") || name.endsWith(".labels")
                || name.endsWith(".names") || name.endsWith(".js") || name.endsWith(".sql")
                || name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".log");
    }

    private static boolean isPreviewable(String name) {
        return isText(name) || name.endsWith(".png") || name.endsWith(".jpg")
                || name.endsWith(".jpeg") || name.endsWith(".gif") || name.endsWith(".bmp")
                || name.endsWith(".webp") || name.endsWith(".svg");
    }

    private static String contentType(String name) {
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".svg")) return "image/svg+xml";
        if (name.endsWith(".csv")) return "text/csv; charset=utf-8";
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".json")) return "application/json; charset=utf-8";
        if (name.endsWith(".ipynb")) return "application/json; charset=utf-8";
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".zip")) return "application/zip";
        if (name.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (name.endsWith(".doc")) return "application/msword";
        if (name.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (name.endsWith(".onnx")) return "application/octet-stream";
        if (name.endsWith(".pth") || name.endsWith(".pt")) return "application/octet-stream";
        if (name.endsWith(".pkl")) return "application/octet-stream";
        return "application/octet-stream";
    }

    private static String urlEncode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20"); }
        catch (Exception e) { return s; }
    }
}
