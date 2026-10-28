package com.aitrainer.service;

import com.aitrainer.db.DB;
import com.aitrainer.util.Json;
import java.util.*;
import static com.aitrainer.service.Curriculum.map;
import static com.aitrainer.security.ProtectedFields.seal;

/** Learner-declared deliverable checks, deliberately separate from exam scores. */
public final class LearningEvidenceService {
    public static Map<String,Object> validate(Map<String,Object> body) {
        if(!body.keySet().equals(new HashSet<String>(Arrays.asList("lessonId","index","checked"))))throw new IllegalArgumentException("成果自查字段不正确");
        String id=Json.str(body.get("lessonId"),"");List<Object> checks=Json.arr(Curriculum.lesson(id).get("evidenceChecks"));
        Object raw=body.get("index");
        if(!(raw instanceof Number)||((Number)raw).doubleValue()!=((Number)raw).intValue())throw new IllegalArgumentException("自查项编号不正确");
        int index=((Number)raw).intValue();
        if(index<0||index>=checks.size()||!(body.get("checked") instanceof Boolean))throw new IllegalArgumentException("自查项或状态不正确");
        return map("lessonId",id,"index",index,"checked",body.get("checked"));
    }
    public Map<String,Object> load(long uid) {
        Map<String,Object> out=new LinkedHashMap<String,Object>();
        for(Map<String,Object> row:DB.query("SELECT lesson_id FROM lesson_progress WHERE user_id=? AND status='evidence_checked' AND lesson_id LIKE 'evidence:%'",uid)){
            String[] parts=Json.str(row.get("lesson_id")).split(":");
            if(parts.length!=3)continue;
            if(!out.containsKey(parts[1]))out.put(parts[1],new ArrayList<Object>());
            Json.arr(out.get(parts[1])).add(Integer.valueOf(parts[2]));
        }
        return out;
    }
    public Map<String,Object> save(long uid,Map<String,Object> body) {
        Map<String,Object> clean=validate(body);boolean checked=Boolean.TRUE.equals(clean.get("checked"));
        DB.update("INSERT INTO lesson_progress(user_id,lesson_id,status,score,attempts,updated_at) VALUES(?,?,?,?,1,NOW()) ON DUPLICATE KEY UPDATE status=VALUES(status),score=VALUES(score),updated_at=NOW()",uid,"evidence:"+clean.get("lessonId")+":"+clean.get("index"),checked?"evidence_checked":"evidence_pending",seal("lesson_progress","score",checked?1:0));
        return map("evidence",load(uid));
    }
}
