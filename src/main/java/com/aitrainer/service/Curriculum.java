package com.aitrainer.service;

import com.aitrainer.util.Json;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class Curriculum {
    private static Map<String,Object> data;
    public static synchronized Map<String,Object> data() {
        if(data==null) {
            try {
                Map<String,Object> loaded=Json.obj(Json.parse(new String(Files.readAllBytes(Paths.get(System.getProperty("curriculum.file","data/curriculum.json"))),StandardCharsets.UTF_8)));
                Map<String,Object> bridge=Json.obj(Json.parse(new String(Files.readAllBytes(Paths.get(System.getProperty("bridge.file","data/bridge-course.json"))),StandardCharsets.UTF_8)));
                List<Object> original=Json.arr(loaded.get("lessons")), extra=Json.arr(bridge.get("lessons")), merged=new ArrayList<Object>();
                Set<String> ids=new HashSet<String>();
                for(Object o:original)if(!ids.add(Json.str(Json.obj(o).get("id"))))throw new IllegalArgumentException("重复课程编号");
                Set<String> baseIds=new HashSet<String>(ids);
                for(Object o:extra){Map<String,Object> l=Json.obj(o);if(!baseIds.contains(Json.str(l.get("after")))||!ids.add(Json.str(l.get("id"))))throw new IllegalArgumentException("衔接课程映射错误");}
                for(Object o:original){merged.add(o);for(Object e:extra)if(Json.obj(o).get("id").equals(Json.obj(e).get("after")))merged.add(e);}
                loaded.put("lessons",merged);loaded.put("overviewCount",original.size());loaded.put("bridgeCount",extra.size());
                loaded.put("totalHours",Json.integer(loaded.get("hours"),136)+Json.integer(bridge.get("additionalHours"),0));
                loaded.put("reviewedAt",bridge.get("version"));data=loaded;
            }
            catch(Exception e) { throw new IllegalStateException("课程文件读取失败，请检查 data/curriculum.json 和 data/bridge-course.json",e); }
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
