package com.aitrainer.web;

import com.aitrainer.util.Json;
import com.aitrainer.util.Str;
import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class Request {
    public final String method;
    public final String path;      // 不含 query，已 URL 解码
    public final String rawPath;
    public final Map<String, String> query = new LinkedHashMap<String, String>();
    public final Map<String, String> headers = new LinkedHashMap<String, String>();
    private String body;
    private Object json;
    private Map<String, String> form;
    private final HttpExchange exchange;

    public Request(HttpExchange ex) {
        this.exchange = ex;
        this.method = ex.getRequestMethod().toUpperCase();
        String uri = ex.getRequestURI().toString();
        int q = uri.indexOf('?');
        this.rawPath = q >= 0 ? uri.substring(0, q) : uri;
        this.path = Str.urlDecode(rawPath);
        if (q >= 0) parseQuery(uri.substring(q + 1));
        for (Map.Entry<String, java.util.List<String>> h : ex.getRequestHeaders().entrySet()) {
            if (!h.getValue().isEmpty()) headers.put(h.getKey().toLowerCase(), h.getValue().get(0));
        }
    }

    private void parseQuery(String qs) {
        if (qs == null || qs.isEmpty()) return;
        for (String kv : qs.split("&")) {
            int i = kv.indexOf('=');
            String key=Str.urlDecode(i<0?kv:kv.substring(0,i));
            if(query.containsKey(key)||query.size()>=30)throw new IllegalArgumentException("查询参数重复或过多");
            query.put(key,i<0?"":Str.urlDecode(kv.substring(i+1)));
        }
    }

    public String body() {
        if (body == null) {
            try (InputStream in = new java.io.BufferedInputStream(exchange.getRequestBody())) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    if(out.size()+n>1024*1024)throw new IllegalArgumentException("请求内容超过1MB上限");
                    out.write(buf, 0, n);
                }
                body = new String(out.toByteArray(), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) { throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException("请求内容读取失败");
            }
        }
        return body;
    }

    public Object json() {
        if (json == null) {
            String b = body();
            if (b == null || b.trim().isEmpty()) { json = null; }
            else {
                try { json = Json.parse(b); } catch (Exception e) { throw new IllegalArgumentException("请求JSON格式错误"); }
                if(!(json instanceof Map))throw new IllegalArgumentException("请求应为JSON对象");
            }
        }
        return json;
    }

    public Map<String, String> form() {
        if (form == null) {
            form = new LinkedHashMap<String, String>();
            String ct = headers.get("content-type");
            if (ct != null && ct.contains("application/x-www-form-urlencoded")) {
                parseQuery(body());
                form.putAll(query);
            }
        }
        return form;
    }

    public String header(String name) {
        return headers.get(name.toLowerCase());
    }

    public String bearerToken() {
        String a = header("authorization");
        if (a != null && a.startsWith("Bearer ")&&!a.equals("Bearer cookie")) return a.substring(7).trim();
        return cookie("ait_session");
    }

    public String cookie(String name) {
        String c = header("cookie");
        if (c == null) return null;
        for (String kv : c.split(";")) {
            String[] p = kv.trim().split("=", 2);
            if (p.length == 2 && p[0].equals(name)) return p[1];
        }
        return null;
    }

    public String ip() {
        String peer=exchange.getRemoteAddress().getAddress().getHostAddress();
        String trusted=System.getenv("TRUSTED_PROXY_IP");
        String xf=header("x-real-ip");
        if(trusted!=null&&trusted.equals(peer)&&xf!=null&&xf.matches("[0-9a-fA-F:.]{3,45}"))return xf;
        return peer;
    }

    public String userAgent() {
        return header("user-agent");
    }
}
