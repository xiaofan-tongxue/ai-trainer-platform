package com.aitrainer.service;

import com.aitrainer.db.DB;
import com.aitrainer.util.Json;
import java.util.*;
import static com.aitrainer.service.Curriculum.map;
import static com.aitrainer.security.ProtectedFields.seal;

/** Account-owned, monotonic learning milestones; never exam grading evidence.
 * Reuses lesson_progress with a separate namespace/status, requiring no DDL upgrade.
 */
public final class PythonProgressService {
    private static final Set<String> PARTS = new HashSet<String>(Arrays.asList(
        "read", "quiz", "guided", "independent", "guidedReference", "independentReference"));
    public static boolean lesson(String id) { return id != null && id.matches("py-(0[1-9]|[1-3][0-9]|4[0-8])"); }
    public static Map<String,Object> validate(Map<String,Object> body) {
        for(String key:body.keySet())if(!Arrays.asList("progress","lastLesson").contains(key))throw new IllegalArgumentException("未知进度字段");
        Object raw=body.get("progress");
        if(!(raw instanceof Map)||((Map<?,?>)raw).size()>48)throw new IllegalArgumentException("课程记录格式错误");
        Map<String,Object> clean=new LinkedHashMap<String,Object>();
        for(Map.Entry<String,Object> entry:Json.obj(raw).entrySet()){
            if(!lesson(entry.getKey())||!(entry.getValue() instanceof Map))throw new IllegalArgumentException("课程编号错误");
            Map<String,Object> flags=new LinkedHashMap<String,Object>();
            for(Map.Entry<String,Object> flag:Json.obj(entry.getValue()).entrySet()){
                if(!PARTS.contains(flag.getKey())||!(flag.getValue() instanceof Boolean))throw new IllegalArgumentException("学习标记错误");
                if(Boolean.TRUE.equals(flag.getValue()))flags.put(flag.getKey(),true);
            }
            if(!flags.isEmpty())clean.put(entry.getKey(),flags);
        }
        Object last=body.get("lastLesson");
        if(last!=null&&(!(last instanceof String)||!lesson((String)last)))throw new IllegalArgumentException("最近课程错误");
        return map("progress",clean,"lastLesson",last);
    }
    public Map<String,Object> load(long uid) {
        Map<String,Object> progress=new LinkedHashMap<String,Object>();String last=null;
        for(Map<String,Object> row:DB.query("SELECT lesson_id,status,score FROM lesson_progress WHERE user_id=? AND lesson_id LIKE 'python:%'",uid)){
            String id=Json.str(row.get("lesson_id"));
            if(id.equals("python:last")){
                int n=Json.integer(row.get("score"),0);
                if(n>=1&&n<=48)last=String.format(java.util.Locale.ROOT,"py-%02d",n);
                continue;
            }
            String[] segments=id.split(":");
            if(segments.length!=3||!lesson(segments[1])||!PARTS.contains(segments[2])||!"python_completed".equals(row.get("status")))continue;
            if(!progress.containsKey(segments[1]))progress.put(segments[1],new LinkedHashMap<String,Object>());
            Json.obj(progress.get(segments[1])).put(segments[2],true);
        }
        return map("progress",progress,"lastLesson",last);
    }
    public Map<String,Object> save(long uid,Map<String,Object> body) {
        Map<String,Object> clean=validate(body);
        // Per-milestone upserts merge concurrent tabs; a stale client cannot erase achievements.
        for(Map.Entry<String,Object> entry:Json.obj(clean.get("progress")).entrySet())
            for(String part:Json.obj(entry.getValue()).keySet())
                DB.update("INSERT INTO lesson_progress(user_id,lesson_id,status,score,attempts,updated_at) VALUES(?,?,'python_completed',?,1,NOW()) ON DUPLICATE KEY UPDATE status='python_completed'",uid,"python:"+entry.getKey()+":"+part,seal("lesson_progress","score",1));
        String last=Json.str(clean.get("lastLesson"),"");
        if(!last.isEmpty())DB.update("INSERT INTO lesson_progress(user_id,lesson_id,status,score,attempts,updated_at) VALUES(?,'python:last','python_bookmark',?,1,NOW()) ON DUPLICATE KEY UPDATE score=VALUES(score),updated_at=NOW()",uid,seal("lesson_progress","score",Integer.parseInt(last.substring(3))));
        return load(uid);
    }
}
