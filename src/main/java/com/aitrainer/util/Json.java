package com.aitrainer.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 轻量级 JSON 解析/序列化（Java 8 无第三方依赖）。
 * 支持: Map(LinkedHashMap) / List(ArrayList) / String / Integer / Long / Double / Boolean / null
 */
public final class Json {
    private Json() {}

    /* ---------------- parse ---------------- */
    public static Object parse(String s) {
        Parser p = new Parser(s);
        Object v = p.parseValue();
        p.skipWs();
        if (!p.eof()) throw new IllegalArgumentException("JSON 多余字符 at " + p.pos);
        return v;
    }

    private static final class Parser {
        final String s;
        int pos;
        int depth;
        Parser(String s) { this.s = s == null ? "" : s; }
        boolean eof() { return pos >= s.length(); }
        char peek() { return s.charAt(pos); }
        void skipWs() { while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++; }
        char next() { return s.charAt(pos++); }

        Object parseValue() {
            if(++depth>64)throw new IllegalArgumentException("JSON嵌套超过64层");
            try{return parseValueInner();}finally{depth--;}
        }
        Object parseValueInner() {
            skipWs();
            if (eof()) throw new IllegalArgumentException("JSON值缺失");
            char c = peek();
            switch (c) {
                case '{': return parseObject();
                case '[': return parseArray();
                case '"': return parseString();
                case 't': expect("true"); return Boolean.TRUE;
                case 'f': expect("false"); return Boolean.FALSE;
                case 'n': expect("null"); return null;
                default: return parseNumber();
            }
        }

        void expect(String w) {
            for (int i = 0; i < w.length(); i++) {
                if (pos+i>=s.length() || s.charAt(pos + i) != w.charAt(i))
                    throw new IllegalArgumentException("JSON 语法错误 at " + pos);
            }
            pos += w.length();
        }

        Map<String, Object> parseObject() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            next(); // {
            skipWs();
            if (!eof() && peek() == '}') { next(); return m; }
            while (true) {
                skipWs();
                String key = parseString();
                skipWs();
                if (eof() || next() != ':') throw new IllegalArgumentException("JSON 缺冒号 at " + pos);
                Object v = parseValue();
                if(m.containsKey(key))throw new IllegalArgumentException("JSON不允许重复字段");
                m.put(key, v);
                skipWs();
                if (eof()) throw new IllegalArgumentException("JSON 未闭合 at " + pos);
                char c = next();
                if (c == '}') break;
                if (c != ',') throw new IllegalArgumentException("JSON 缺逗号 at " + pos);
            }
            return m;
        }

        List<Object> parseArray() {
            List<Object> list = new ArrayList<Object>();
            next(); // [
            skipWs();
            if (!eof() && peek() == ']') { next(); return list; }
            while (true) {
                Object v = parseValue();
                list.add(v);
                skipWs();
                if (eof()) throw new IllegalArgumentException("JSON 数组未闭合 at " + pos);
                char c = next();
                if (c == ']') break;
                if (c != ',') throw new IllegalArgumentException("JSON 数组缺逗号 at " + pos);
            }
            return list;
        }

        String parseString() {
            if(eof()||peek()!='"')throw new IllegalArgumentException("JSON字段必须是字符串");
            next(); // "
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (eof()) throw new IllegalArgumentException("JSON 字符串未闭合");
                char c = next();
                if (c == '"') break;
                if (c == '\\') {
                    if (eof()) throw new IllegalArgumentException("JSON 转义错误");
                    char e = next();
                    switch (e) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            if (pos + 4 > s.length()) throw new IllegalArgumentException("JSON \\u 错误");
                            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                            pos += 4;
                            break;
                        default: throw new IllegalArgumentException("JSON转义无效");
                    }
                } else {
                    if(c<0x20)throw new IllegalArgumentException("JSON控制字符无效");
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        Object parseNumber() {
            int start = pos;
            while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || "+-eE.".indexOf(s.charAt(pos)) >= 0)) pos++;
            String n = s.substring(start, pos);
            if(!n.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?"))throw new IllegalArgumentException("JSON数字无效");
            if (n.indexOf('.') >= 0 || n.indexOf('e') >= 0 || n.indexOf('E') >= 0) {
                try { double value=Double.parseDouble(n);if(!Double.isFinite(value))throw new IllegalArgumentException();return value; } catch (Exception e) { throw new IllegalArgumentException("JSON数字超出范围"); }
            }
            try { return Long.valueOf(n); } catch (Exception e) { throw new IllegalArgumentException("JSON整数超出范围"); }
        }
    }

    /* ---------------- stringify ---------------- */
    public static String stringify(Object v) {
        StringBuilder sb = new StringBuilder();
        write(sb, v);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object v) {
        if (v == null) { sb.append("null"); return; }
        if (v instanceof String) { writeString(sb, (String) v); return; }
        if (v instanceof Boolean || v instanceof Integer || v instanceof Long || v instanceof Double || v instanceof Float || v instanceof Short) {
            sb.append(v.toString()); return;
        }
        if (v instanceof Map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                writeString(sb, String.valueOf(e.getKey()));
                sb.append(':');
                write(sb, e.getValue());
            }
            sb.append('}');
            return;
        }
        if (v instanceof Iterable) {
            sb.append('[');
            boolean first = true;
            for (Object o : (Iterable<?>) v) {
                if (!first) sb.append(',');
                first = false;
                write(sb, o);
            }
            sb.append(']');
            return;
        }
        if (v instanceof Object[]) {
            sb.append('[');
            boolean first = true;
            for (Object o : (Object[]) v) {
                if (!first) sb.append(',');
                first = false;
                write(sb, o);
            }
            sb.append(']');
            return;
        }
        writeString(sb, v.toString());
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }

    /* ---------------- helpers ---------------- */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> obj(Object v) {
        return v instanceof Map ? (Map<String, Object>) v : new LinkedHashMap<String, Object>();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> arr(Object v) {
        return v instanceof List ? (List<Object>) v : new ArrayList<Object>();
    }

    public static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    public static String str(Object v, String def) {
        return v == null ? def : String.valueOf(v);
    }

    public static long lng(Object v, long def) {
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) { try { return Long.parseLong((String) v); } catch (Exception e) {} }
        return def;
    }

    public static int integer(Object v, int def) {
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) { try { return Integer.parseInt((String) v); } catch (Exception e) {} }
        return def;
    }
}
