package com.aitrainer.service;

import com.aitrainer.util.Json;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Versioned public teaching overlays. Never writes to the learner database. */
public final class ContentQuality {
    private static final Map<String,Object> QUESTIONS=read("data/question-explanations.json","questions");
    private static final Map<String,Object> PRACTICAL=read("data/practical-guidance.json","tasks");
    private static Map<String,Object> read(String file,String key){
        try{return Json.obj(Json.obj(Json.parse(new String(Files.readAllBytes(Paths.get(file)),StandardCharsets.UTF_8))).get(key));}
        catch(Exception e){throw new IllegalStateException("学习资料质量文件读取失败："+file,e);}
    }
    public static String sha256(String value){
        try{StringBuilder b=new StringBuilder();for(byte n:MessageDigest.getInstance("SHA-256").digest(value.replace("\r\n","\n").getBytes(StandardCharsets.UTF_8)))b.append(String.format("%02x",n&255));return b.toString();}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    public static String fingerprint(Map<String,Object> q){
        Object raw=q.get("options");if(raw instanceof String)raw=Json.parse((String)raw);
        List<Object> options=new ArrayList<Object>();
        for(Object o:Json.arr(raw)){Map<String,Object> v=Json.obj(o);options.add(Arrays.asList(Json.str(v.get("label"),""),Json.str(v.get("text"),"")));}
        return sha256(Json.stringify(Arrays.asList(Json.str(q.get("type"),""),Json.str(q.get("question"),""),options,Json.str(q.get("answer"),""))));
    }
    public static boolean questionEligible(String fingerprint){return !Boolean.FALSE.equals(Json.obj(QUESTIONS.get(fingerprint)).get("examEligible"));}
    public static void question(Map<String,Object> q){
        String hash=fingerprint(q);Map<String,Object> extra=Json.obj(QUESTIONS.get(hash));
        q.put("questionFingerprint",hash);q.put("examEligible",questionEligible(hash));
        if(!extra.isEmpty()){
            q.put("reviewStatus",extra.get("reviewStatus"));
            // Preserve an administrator's explanation; append provenance and unresolved issues.
            String original=Json.str(q.get("analysis"),"").trim();
            q.put("analysis",original.isEmpty()?extra.get("analysis"):original.endsWith(Json.str(extra.get("analysis")))?original:original+"\n\n"+extra.get("analysis"));
        }
    }
    public static boolean practicalEligible(String code){return !Boolean.FALSE.equals(Json.obj(PRACTICAL.get(code)).get("examEligible"));}
    public static void practical(Map<String,Object> t){
        Map<String,Object> extra=Json.obj(PRACTICAL.get(Json.str(t.get("code"))));
        if(extra.isEmpty()){t.put("examEligible",false);t.put("qualityNotice","自定义题目尚未完成素材和参考答案核查，暂不纳入模拟。");return;}
        String hash=sha256(Json.str(t.get("reference_answer"),""));
        boolean matches=Json.str(extra.get("sourceReferenceSha256")).equals(hash)||sha256(Json.str(extra.get("reference"))).equals(hash);
        t.put("examEligible",matches&&practicalEligible(Json.str(t.get("code"))));
        t.put("qualityNotice",matches?extra.get("notice"):"本题参考答案与已核查版本不同，保留现有内容，暂不纳入模拟；请管理员复核。");
        if(matches){
            t.put("reference_answer",extra.get("reference"));
            Map<String,Object> guide=new LinkedHashMap<String,Object>(extra);guide.remove("reference");guide.remove("sourceReferenceSha256");
            t.put("learningGuide",guide);
        }
    }
    public static void hideSolutions(Map<String,Object> q){
        for(String k:Arrays.asList("answer","analysis","reference_answer","learningGuide","questionFingerprint"))q.remove(k);
    }
}
