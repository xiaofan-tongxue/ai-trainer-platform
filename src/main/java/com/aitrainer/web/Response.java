package com.aitrainer.web;

import com.aitrainer.util.Json;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class Response {
    private final HttpExchange exchange;
    private boolean sent = false;
    private int statusCode=500;
    public int statusCode(){return statusCode;}

    public Response(HttpExchange exchange) {
        this.exchange = exchange;
        header("X-Content-Type-Options","nosniff");
        header("X-Frame-Options","DENY");
        header("Referrer-Policy","no-referrer");
        header("Permissions-Policy","camera=(), microphone=(), geolocation=()");
        header("Content-Security-Policy","default-src 'self'; script-src 'self' 'unsafe-inline' 'wasm-unsafe-eval' https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; connect-src 'self' https://cdn.jsdelivr.net; worker-src 'self' blob:; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'");
        if(com.aitrainer.security.SecurityGate.production())header("Strict-Transport-Security","max-age=31536000");
    }

    public void header(String name,String value){exchange.getResponseHeaders().set(name,value);}

    public void json(int status, Object obj) {
        byte[] b = Json.stringify(obj).getBytes(StandardCharsets.UTF_8);
        send(status, "application/json; charset=utf-8", b);
    }

    public void json(Object obj) {
        json(200, obj);
    }

    public void ok(Object data) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("code", 0);
        m.put("data", data);
        json(200, m);
    }

    public void fail(int status, String message) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("code", 1);
        m.put("message", message);
        json(status, m);
    }

    public void error(String message) {
        fail(400, message);
    }

    public void unauthorized() {
        fail(401, "未登录或登录已过期");
    }

    public void forbidden() {
        fail(403, "无权限访问");
    }

    public void text(int status, String contentType, String body) {
        send(status, contentType, body.getBytes(StandardCharsets.UTF_8));
    }

    public void bytes(int status, String contentType, byte[] body) {
        send(status, contentType, body);
    }

    private void send(int status, String contentType, byte[] body) {
        if (sent) return;
        sent = true;
        statusCode=status;
        try {
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        } catch (IOException e) {
            // 客户端可能已断开
        }
    }

    public boolean isSent() { return sent; }
}
