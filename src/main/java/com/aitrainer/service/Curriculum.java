package com.aitrainer.service;

import com.aitrainer.util.Json;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class Curriculum {
    private static Map<String,Object> data;
    public static synchronized Map<String,Object> data() {
        if(data==null) {
            try { data=Json.obj(Json.parse(new String(Files.readAllBytes(Paths.get(System.getProperty("curriculum.file","data/curriculum.json"))),StandardCharsets.UTF_8))); }
            catch(Exception e) { throw new IllegalStateException("课程文件读取失败，请检查 data/curriculum.json",e); }
        }
        return data;
    }
    public static List<Object> lessons() { return Json.arr(data().get("lessons")); }
    public static Map<String,Object> lesson(String id) {
        for(Object o:lessons()) if(id.equals(Json.obj(o).get("id"))) return Json.obj(o);
        throw new IllegalArgumentException("课程不存在");
    }
    public static Map<String,Object> view() {
        Map<String,Object> copy=Json.obj(Json.parse(Json.stringify(data())));
        for(Object o:Json.arr(copy.get("lessons"))) for(Object q:Json.arr(Json.obj(o).get("quiz"))) {
            Json.obj(q).remove("answer"); Json.obj(q).remove("explanation");
        }
        return copy;
    }
    public static Map<String,Object> map(Object... pairs) {
        Map<String,Object> m=new LinkedHashMap<String,Object>();
        for(int i=0;i<pairs.length;i+=2)m.put(pairs[i].toString(),pairs[i+1]); return m;
    }
}
