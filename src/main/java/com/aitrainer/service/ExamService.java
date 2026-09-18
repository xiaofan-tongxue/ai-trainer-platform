package com.aitrainer.service;

import com.aitrainer.dao.*;
import com.aitrainer.db.DB;
import com.aitrainer.util.Json;
import java.sql.*;
import java.util.*;
import static com.aitrainer.service.Curriculum.map;

/** Persistent server-assigned papers, clock, answer snapshots and idempotent grading. */
public class ExamService {
    private static final Object[] LOCKS=new Object[64];
    static{for(int i=0;i<LOCKS.length;i++)LOCKS[i]=new Object();}
    private Object lock(long uid){return LOCKS[(int)(uid%LOCKS.length)];}
    public Map<String,Object> generate(long userId,String mode){synchronized(lock(userId)){
        if(!Arrays.asList("theory","diagnostic","practical","shanghai").contains(mode))throw new IllegalArgumentException("未知考试模式");
        Map<String,Object> active=DB.queryOne("SELECT * FROM exam_sessions WHERE user_id=? AND mode=? AND status='ACTIVE' ORDER BY started_at DESC LIMIT 1",userId,mode);
        if(active!=null){
            if(now()<=Json.lng(active.get("deadline_at"),0))return view(active);
            submit(userId,Json.str(active.get("id")),Collections.<String,Object>emptyMap(),false);
        }
        List<Map<String,Object>> questions=new ArrayList<Map<String,Object>>();
        if(mode.equals("practical")){
            for(int i=2;i<Knowledge.IDS.length;i++){
                String prefix=String.valueOf(i-1)+".%";
                Map<String,Object> q=DB.queryOne("SELECT * FROM practical_tasks WHERE code LIKE ? ORDER BY RAND() LIMIT 1",prefix);
                if(q==null)throw new IllegalStateException("实操领域缺少题目："+Knowledge.NAMES[i]);
                PracticalDao.decorate(q);q.put("domain",Knowledge.IDS[i]);q.put("points",Knowledge.PRACTICAL[i]);questions.add(q);
            }
        }else if(mode.equals("shanghai")){
            String[] types={"JUDGE","SINGLE","MULTIPLE"};int[] counts={40,140,10};
            for(int i=0;i<types.length;i++){
                List<Map<String,Object>> list=new QuestionDao().randomByType(types[i],counts[i]);
                if(list.size()!=counts[i])throw new IllegalStateException("题库题型数量不足");
                for(Map<String,Object> q:list){q.put("domain",Knowledge.of(q));q.put("points",i==2?1.0:0.5);questions.add(q);}
            }
        }else{
            List<Map<String,Object>> all=new QuestionDao().allTagged();Collections.shuffle(all);
            for(int i=0;i<Knowledge.IDS.length;i++){
                int count=mode.equals("diagnostic")?3:Knowledge.THEORY[i];int n=0;
                for(Map<String,Object> q:all)if(Knowledge.IDS[i].equals(q.get("domain"))&&n<count){q.put("points",mode.equals("diagnostic")?100.0/18:1.0);questions.add(q);n++;}
                if(n<count)throw new IllegalStateException("题库领域数量不足："+Knowledge.NAMES[i]);
            }
            Collections.shuffle(questions);
        }
        String title=mode.equals("practical")?"国标覆盖 · 技能模拟":mode.equals("diagnostic")?"入门诊断":mode.equals("shanghai")?"上海公开资料 · 补充模拟":"国标覆盖 · 理论模拟";
        int sec=mode.equals("diagnostic")?1200:mode.equals("practical")?7200:5400;
        String id=UUID.randomUUID().toString();long start=now();
        Map<String,Object> paper=map("title",title,"mode",mode,"questions",questions,"timeLimitSec",sec,"passScore",60,"notice",Curriculum.data().get("exam"));
        DB.update("INSERT INTO exam_sessions(id,user_id,mode,title,paper,answers,status,started_at,deadline_at) VALUES(?,?,?,?,?,?,'ACTIVE',?,?)",id,userId,mode,title,secret("paper",Json.stringify(paper)),secret("answers","{}"),start,start+sec*1000L);
        return view(owned(userId,id));
    }}
    private static long now(){return System.currentTimeMillis();}
    private Map<String,Object> owned(long uid,String id){
        Map<String,Object> row=DB.queryOne("SELECT * FROM exam_sessions WHERE id=? AND user_id=?",id,uid);
        if(row==null)throw new IllegalArgumentException("考试不存在或不属于当前学员");return row;
    }
    private Map<String,Object> view(Map<String,Object> row){
        Map<String,Object> paper=Json.obj(Json.parse(Json.str(row.get("paper"))));
        for(Object o:Json.arr(paper.get("questions"))){Map<String,Object> q=Json.obj(o);q.remove("answer");q.remove("analysis");q.remove("reference_answer");}
        paper.put("sessionId",row.get("id"));paper.put("startedAt",row.get("started_at"));paper.put("deadlineAt",row.get("deadline_at"));paper.put("serverNow",now());paper.put("status",row.get("status"));
        paper.put("answers",Json.parse(Json.str(row.get("answers"),"{}")));
        if(row.get("result")!=null)paper.put("result",Json.parse(Json.str(row.get("result"))));
        return paper;
    }
    public Map<String,Object> session(long uid,String id){synchronized(lock(uid)){
        Map<String,Object> row=owned(uid,id);
        if("ACTIVE".equals(row.get("status"))&&now()>Json.lng(row.get("deadline_at"),0))submit(uid,id,Collections.<String,Object>emptyMap(),false);
        return view(owned(uid,id));
    }}
    private Map<String,Object> validateAnswers(Map<String,Object> row,Map<String,Object> submitted){
        Map<String,Object> paper=Json.obj(Json.parse(Json.str(row.get("paper"))));Set<String> ids=new HashSet<String>();
        for(Object o:Json.arr(paper.get("questions")))ids.add(Json.str(Json.obj(o).get("id")));
        Map<String,Object> clean=new LinkedHashMap<String,Object>();
        int max="practical".equals(row.get("mode"))?30000:10;
        for(Map.Entry<String,Object> e:submitted.entrySet()){
            if(!ids.contains(e.getKey())||!(e.getValue() instanceof String)||((String)e.getValue()).length()>max)throw new IllegalArgumentException("答案包含非本卷题目或无效内容");
            clean.put(e.getKey(),e.getValue());
        }return clean;
    }
    public Map<String,Object> save(long uid,String id,Map<String,Object> submitted){synchronized(lock(uid)){
        Map<String,Object> row=owned(uid,id);if(!"ACTIVE".equals(row.get("status")))throw new IllegalArgumentException("本卷已交卷，作答已锁定");
        if(now()>Json.lng(row.get("deadline_at"),0))throw new IllegalArgumentException("考试时间已到，请交卷；已保存的作答会被保留");
        Map<String,Object> answers=Json.obj(Json.parse(Json.str(row.get("answers"),"{}")));answers.putAll(validateAnswers(row,submitted));
        DB.update("UPDATE exam_sessions SET answers=? WHERE id=? AND user_id=? AND status='ACTIVE'",secret("answers",Json.stringify(answers)),id,uid);
        return map("saved",true,"serverNow",now(),"deadlineAt",row.get("deadline_at"));
    }}
    public Map<String,Object> submit(long uid,String id,Map<String,Object> submitted,boolean review){synchronized(lock(uid)){
        Map<String,Object> row=owned(uid,id);
        if("SUBMITTED".equals(row.get("status")))return Json.obj(Json.parse(Json.str(row.get("result"))));
        boolean expired=now()>Json.lng(row.get("deadline_at"),0);
        if("ACTIVE".equals(row.get("status"))){
            Map<String,Object> answers=Json.obj(Json.parse(Json.str(row.get("answers"),"{}")));
            // After deadline, only previously saved answers are accepted.
            if(!expired)answers.putAll(validateAnswers(row,submitted));
            DB.update("UPDATE exam_sessions SET answers=?,status='PENDING',submitted_at=NOW() WHERE id=? AND user_id=?",secret("answers",Json.stringify(answers)),id,uid);
            row=owned(uid,id);
        }
        Map<String,Object> paper=Json.obj(Json.parse(Json.str(row.get("paper"))));
        Map<String,Object> answers=Json.obj(Json.parse(Json.str(row.get("answers"),"{}")));
        long submittedAt=Timestamp.valueOf(Json.str(row.get("submitted_at"))).getTime();
        expired=submittedAt>=Json.lng(row.get("deadline_at"),0);
        long duration=Math.max(0,Math.min(Json.lng(row.get("deadline_at"),0),submittedAt)-Json.lng(row.get("started_at"),0))/1000;
        if("practical".equals(row.get("mode"))){
            if(!review)return map("sessionId",id,"status","PENDING","mode","practical","message","技能作答已锁定并保存，请点击审阅生成分数");
            Map<String,Object> result=practicalGrade(row,paper,answers);
            result.put("sessionId",id);result.put("mode","practical");result.put("durationSec",duration);result.put("expired",expired);
            boolean done=Boolean.TRUE.equals(result.get("complete"));
            DB.update("UPDATE exam_sessions SET result=?,status=? WHERE id=? AND user_id=?",secret("result",Json.stringify(result)),done?"SUBMITTED":"PENDING",id,uid);
            return result;
        }
        final Map<String,Object> result=gradeTheory(paper,answers);result.put("sessionId",id);result.put("mode",row.get("mode"));result.put("durationSec",duration);result.put("expired",expired);result.put("complete",true);
        final Map<String,Object> finalRow=row;
        DB.tx(c->{
            try(PreparedStatement p=c.prepareStatement("INSERT INTO exam_records(user_id,title,total,correct,score,passed,duration_sec,detail,created_at) VALUES(?,?,?,?,?,?,?,?,NOW())",Statement.RETURN_GENERATED_KEYS)){
                p.setLong(1,uid);p.setString(2,Json.str(finalRow.get("title")));
                p.setString(3,recordSecret("total",result.get("total")));p.setString(4,recordSecret("correct",result.get("correct")));p.setString(5,recordSecret("score",result.get("score")));p.setString(6,recordSecret("passed",Boolean.TRUE.equals(result.get("passed"))?1:0));p.setString(7,recordSecret("duration_sec",duration));p.setString(8,recordSecret("detail",Json.stringify(result.get("detail"))));p.executeUpdate();try(ResultSet keys=p.getGeneratedKeys()){if(keys.next())result.put("recordId",keys.getLong(1));}
            }
            for(Object o:Json.arr(result.get("detail"))){Map<String,Object> d=Json.obj(o);if(!Boolean.TRUE.equals(d.get("correct")))try(PreparedStatement p=c.prepareStatement("INSERT INTO mistakes(user_id,question_id,wrong_count,mastered,last_wrong_at,created_at) VALUES(?,?,1,0,NOW(),NOW()) ON DUPLICATE KEY UPDATE wrong_count=wrong_count+1,mastered=0,last_wrong_at=NOW()")){p.setLong(1,uid);p.setLong(2,Json.lng(d.get("questionId"),0));p.executeUpdate();}}
            try(PreparedStatement p=c.prepareStatement("UPDATE exam_sessions SET result=?,status='SUBMITTED' WHERE id=? AND user_id=?")){p.setString(1,secret("result",Json.stringify(result)));p.setString(2,id);p.setLong(3,uid);p.executeUpdate();}return null;
        });
        return result;
    }}
    public static Map<String,Object> gradeTheory(Map<String,Object> paper,Map<String,Object> answers){
        List<Object> detail=new ArrayList<Object>();Map<String,Object> domains=new LinkedHashMap<String,Object>();double score=0;int correct=0;
        for(Object o:Json.arr(paper.get("questions"))){Map<String,Object> q=Json.obj(o);String id=Json.str(q.get("id"));String answer=Json.str(answers.get(id),"");boolean ok=QuestionService.judge(Json.str(q.get("type")),Json.str(q.get("answer")),answer);double points=((Number)q.get("points")).doubleValue();if(ok){score+=points;correct++;}
            String domain=Json.str(q.get("domain"));Map<String,Object> d=Json.obj(domains.get(domain));double max=d.containsKey("max")?((Number)d.get("max")).doubleValue():0;double got=d.containsKey("score")?((Number)d.get("score")).doubleValue():0;domains.put(domain,map("max",max+points,"score",got+(ok?points:0)));
            detail.add(map("questionId",q.get("id"),"question",q.get("question"),"type",q.get("type"),"options",q.get("options"),"domain",domain,"yourAnswer",answer,"correctAnswer",q.get("answer"),"correct",ok,"analysis",q.get("analysis"),"score",points));
        }
        score=Math.round(score*10)/10.0;
        return map("total",detail.size(),"correct",correct,"score",score,"passed",score>=60,"detail",detail,"domains",domains,"gradingMode","objective");
    }
    private Map<String,Object> practicalGrade(Map<String,Object> row,Map<String,Object> paper,Map<String,Object> answers){
        Map<String,Object> previous=Json.obj(Json.parse(Json.str(row.get("result"),"{}")));List<Object> detail=new ArrayList<Object>(Json.arr(previous.get("detail")));Set<String> done=new HashSet<String>();for(Object o:detail)done.add(Json.str(Json.obj(o).get("questionId")));
        String failure=null;
        for(Object o:Json.arr(paper.get("questions"))){Map<String,Object> q=Json.obj(o);String id=Json.str(q.get("id"));if(done.contains(id))continue;
            String answer=Json.str(answers.get(id),"");
            try{
                AiAgentService.GradeResult g;
                if(answer.trim().isEmpty()){g=new AiAgentService.GradeResult();g.score=0;g.mode="empty";g.comment="未作答";g.analysis="本题未提交，按零分计。";}
                else g=new AiAgentService().review(Json.str(q.get("task_desc"),"")+"\n技能要求："+Json.str(q.get("skills"),"")+"\n质量要求："+Json.str(q.get("quality"),""),Json.str(q.get("reference_answer"),""),answer);
                detail.add(map("questionId",q.get("id"),"code",q.get("code"),"title",q.get("title"),"domain",q.get("domain"),"points",q.get("points"),"score",g.score,"mode",g.mode,"comment",g.comment,"analysis",g.analysis,"yourAnswer",answer));
                DB.update("UPDATE exam_sessions SET result=? WHERE id=?",secret("result",Json.stringify(map("detail",detail,"complete",false))),row.get("id"));
            }catch(DeepSeekClient.AiFailure e){failure=e.getMessage();break;}
        }
        boolean complete=detail.size()==Json.arr(paper.get("questions")).size();double total=0;Map<String,Object> domains=new LinkedHashMap<String,Object>();
        for(Object o:detail){Map<String,Object> d=Json.obj(o);double points=((Number)d.get("points")).doubleValue();double got=((Number)d.get("score")).doubleValue()*points/100;total+=got;domains.put(Json.str(d.get("domain")),map("max",points,"score",got));}
        return map("detail",detail,"domains",domains,"complete",complete,"score",complete?Math.round(total*10)/10.0:null,"passed",complete&&total>=60,"gradingMode","deepseek_review","message",failure==null?"AI 审阅不等于代码执行验证":failure);
    }
    public List<Map<String,Object>> records(long uid){return new ExamDao().listByUser(uid);}
    private static String secret(String field,Object value){return com.aitrainer.security.ProtectedFields.seal("exam_sessions",field,value);}
    private static String recordSecret(String field,Object value){return com.aitrainer.security.ProtectedFields.seal("exam_records",field,value);}
    public List<Map<String,Object>> sessions(long uid){return DB.query("SELECT id,mode,title,status,started_at,deadline_at,submitted_at,result FROM exam_sessions WHERE user_id=? ORDER BY started_at DESC LIMIT 50",uid);}
    public Map<String,Object> record(long uid,long id){Map<String,Object> r=DB.queryOne("SELECT * FROM exam_records WHERE id=? AND user_id=?",id,uid);if(r!=null)r.put("detail",Json.parse(Json.str(r.get("detail"),"[]")));return r;}
}
