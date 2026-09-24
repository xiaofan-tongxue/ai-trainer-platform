package com.aitrainer.service;
import com.aitrainer.db.DB;
import com.aitrainer.util.Json;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.io.*;
import java.util.*;
import static com.aitrainer.service.Curriculum.map;

/** Dependency-free behavioral tests; integration fixtures are removed in finally. */
public class PlatformTest {
    private static final String BASE=System.getProperty("test.baseUrl","http://127.0.0.1:19001");
    private static int checks=0;
    private static void check(boolean b,String label){if(!b)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
    private static void rejects(Runnable fn,String label){boolean rejected=false;try{fn.run();}catch(IllegalArgumentException|DeepSeekClient.AiFailure e){rejected=true;}check(rejected,label);}
    private static Map<String,Object> exam(String mode,double score){List<Object> detail=new ArrayList<Object>();Map<String,Object> domains=new LinkedHashMap<String,Object>();for(int i=2;i<6;i++){detail.add(map("mode","deepseek_review"));domains.put(Knowledge.IDS[i],map("max",Knowledge.PRACTICAL[i],"score",score*Knowledge.PRACTICAL[i]/100));}return map("mode",mode,"submitted_at","2026-09-11","result",map("complete",true,"score",score,"durationSec",600,"gradingMode",mode.equals("theory")?"objective":"deepseek_review","detail",detail,"domains",domains));}
    public static void main(String[] args)throws Exception{
        if(Arrays.asList(args).contains("--ui-create")){createUi();return;}
        if(Arrays.asList(args).contains("--ui-cleanup")){cleanupUi();return;}
        unit();adapter();
        if(Arrays.asList(args).contains("--integration"))integration();
        System.out.println("ALL "+checks+" CHECKS PASSED");
    }
    private static void unit(){
        check(PythonProgressService.lesson("py-01")&&PythonProgressService.lesson("py-48")&&!PythonProgressService.lesson("py-49"),"Python progress accepts only actual lessons");
        rejects(()->PythonProgressService.validate(map("progress",map("py-01",map("quiz","true")))),"Python progress rejects non-boolean milestones");
        rejects(()->PythonProgressService.validate(map("progress",map(),"userId",2)),"Python progress never accepts a client user id");
        rejects(()->PythonProgressService.validate(map("progress",map("py-99",map("read",true)))),"Python progress rejects unknown lessons");
        Map<String,Object> empty=ReadinessEngine.evaluate(Collections.emptyList(),Collections.emptyList());
        check(Json.obj(empty.get("estimate")).get("value")==null,"no evidence gives no probability");
        Map<String,Object> diagnostic=ReadinessEngine.evaluate(Arrays.asList(exam("diagnostic",50)),Collections.emptyList());
        check(Json.arr(diagnostic.get("domains")).stream().anyMatch(o->Json.obj(o).get("mastery")!=null),"diagnostic informs domain learning priorities");
        check(Json.obj(diagnostic.get("estimate")).get("value")==null,"diagnostic never supplies pass probability evidence");
        List<Map<String,Object>> evidence=new ArrayList<Map<String,Object>>();for(int i=0;i<3;i++)evidence.add(exam("theory",85));
        check(!Boolean.TRUE.equals(Json.obj(ReadinessEngine.evaluate(evidence,Collections.emptyList()).get("estimate")).get("available")),"theory alone never gives joint estimate");
        evidence.add(exam("practical",85));evidence.add(exam("practical",85));Map<String,Object> high=ReadinessEngine.evaluate(evidence,Collections.emptyList());
        check(Boolean.TRUE.equals(Json.obj(high.get("estimate")).get("available")),"minimum dual-subject evidence enables estimate");
        int highP=Json.integer(Json.obj(high.get("estimate")).get("value"),0);
        evidence.set(3,exam("practical",40));evidence.set(4,exam("practical",45));int lowP=Json.integer(Json.obj(ReadinessEngine.evaluate(evidence,Collections.emptyList()).get("estimate")).get("value"),0);
        check(lowP<highP&&lowP<50,"weak practical cannot be compensated by high theory");
        Json.obj(evidence.get(4).get("result")).put("durationSec",1);
        check(!Boolean.TRUE.equals(Json.obj(ReadinessEngine.evaluate(evidence,Collections.emptyList()).get("estimate")).get("available")),"speed submissions excluded");
        List<Map<String,Object>> repeated=Arrays.asList(map("question_id",1,"is_correct",1,"domain","foundation"),map("question_id",1,"is_correct",0,"domain","foundation"));
        check(Json.integer(ReadinessEngine.evaluate(Collections.emptyList(),repeated).get("uniquePractice"),0)==1,"practice duplicates do not inflate sample size");
        Map<String,Object> paper=map("questions",Arrays.asList(map("id",1,"question","q1","domain","business","type","SINGLE","answer","A","points",50),map("id",2,"question","q2","domain","training","type","MULTIPLE","answer","AB","points",50)));
        check(Json.integer(ExamService.gradeTheory(paper,map("1","A")).get("score"),0)==50,"unanswered questions stay in denominator");
        check(Json.integer(ExamService.gradeTheory(paper,map("1","A","2","BA")).get("score"),0)==100,"order independent exact multiple choice");
        check(Json.integer(ExamService.gradeTheory(paper,map("2","ABC")).get("score"),0)==0,"extra multiple choice option scores zero");
        rejects(()->DeepSeekClient.validateEndpoint("https://example.org/chat/completions"),"key cannot be routed to non-provider host");
        rejects(()->DeepSeekClient.validateEndpoint("http://api.deepseek.com/chat/completions"),"insecure endpoint rejected");
        Map<String,Object> grade=map("dimensions",map("task",41,"method",30,"evidence",20,"delivery",10),"covered",Arrays.asList("x"),"missing",Collections.emptyList(),"comment","x","analysis","x");
        rejects(()->AiAgentService.validatedReview(grade),"out of range AI score rejected");
        Json.obj(grade.get("dimensions")).put("task",40);check(AiAgentService.validatedReview(grade).score==100,"AI score calculated from validated rubric");
        List<Object> plan=new ArrayList<Object>();for(int i=1;i<=7;i++)plan.add(map("day",i,"minutes",60,"task","test"));
        Map<String,Object> report=map("summary","x","strengths",Collections.emptyList(),"priorities",Arrays.asList(map("domain","training","reason","x","action","x","target","x")),"weeklyPlan",plan,"examStrategy",Collections.emptyList());
        check(Json.arr(LearningService.validateReport(report,60).get("weeklyPlan")).size()==7,"seven-day report accepted");
        rejects(()->LearningService.validateReport(report,30),"AI plan exceeding daily budget rejected");
        Json.obj(plan.get(0)).put("task","完成1次四领域完整技能模拟");
        rejects(()->LearningService.validateReport(report,60),"120 minute full skill paper cannot fit 60 minute plan");
        Json.obj(plan.get(0)).put("task","专项训练");report.put("examStrategy",Arrays.asList("理论每题至少60秒"));
        rejects(()->LearningService.validateReport(report,60),"AI per-question timing hallucination rejected");
        empty.put("profile",map("foundation","beginner","daily_minutes",15));empty.put("foundationLessons",0);
        Map<String,Object> beginnerPlan=LearningService.localPlan(empty);
        check(Json.str(Json.obj(Json.arr(beginnerPlan.get("weeklyPlan")).get(0)).get("task")).contains("文件"),"zero foundation plan starts with computer basics rather than model training");
        check(Json.arr(beginnerPlan.get("weeklyPlan")).stream().allMatch(o->Json.integer(Json.obj(o).get("minutes"),0)<=15),"foundation plan respects fifteen minute budget");
        empty.put("foundationLessons",4);check(!LearningService.needsFoundation(empty),"passed prerequisites unlock evidence based planning");
        Json.obj(empty.get("profile")).put("foundation","experienced");
        check(Json.str(Json.obj(Json.arr(LearningService.localPlan(empty).get("weeklyPlan")).get(0)).get("task")).contains("另外预约20分钟"),"short budget schedules diagnosis in separate time slot");
        check(Curriculum.lessons().size()==24,"all 24 lessons loaded");
        check(!Json.obj(Json.arr(Json.obj(Json.arr(Curriculum.view().get("lessons")).get(0)).get("quiz")).get(0)).containsKey("answer"),"course answers hidden before checking");
    }
    private static void adapter()throws Exception{
        HttpServer fixture=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);final int[] calls={0};
        fixture.createContext("/",ex->{calls[0]++;String path=ex.getRequestURI().getPath();while(ex.getRequestBody().read()!=-1){}int code=path.equals("/auth")?401:200;
            String content=path.equals("/bad")?"{}":path.equals("/cut")?Json.stringify(map("choices",Arrays.asList(map("finish_reason","length","message",map("content","{}"))))):Json.stringify(map("choices",Arrays.asList(map("finish_reason","stop","message",map("content","{\"ok\":true}")))));
            byte[] b=content.getBytes("UTF-8");ex.sendResponseHeaders(code,b.length);ex.getResponseBody().write(b);ex.close();});fixture.start();
        String base="http://127.0.0.1:"+fixture.getAddress().getPort();
        try{check(Boolean.TRUE.equals(DeepSeekClient.request(base,"fixture-key","fixture-model","json","test").get("ok")),"provider JSON adapter roundtrip");
            rejects(()->DeepSeekClient.request(base+"/auth","fixture-key","m","json","x"),"provider auth failure is surfaced");
            rejects(()->DeepSeekClient.request(base+"/bad","fixture-key","m","json","x"),"empty provider reply rejected");
            rejects(()->DeepSeekClient.request(base+"/cut","fixture-key","m","json","x"),"truncated provider reply rejected");
        }finally{fixture.stop(0);}
    }
    private static Object request(String path,String method,String token,Object body)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(BASE+"/api"+path).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(120000);c.setRequestMethod(method);if(token!=null)c.setRequestProperty("Authorization","Bearer "+token);
        if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream out=c.getOutputStream()){out.write(Json.stringify(body).getBytes("UTF-8"));}}
        int status=c.getResponseCode();InputStream in=status<400?c.getInputStream():c.getErrorStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)bytes.write(b,0,n);c.disconnect();
        Map<String,Object> response=Json.obj(Json.parse(new String(bytes.toByteArray(),"UTF-8")));return map("status",status,"response",response,"data",response.get("data"));
    }
    private static Map<String,Object> get(String path,String token)throws Exception{return Json.obj(Json.obj(request(path,"GET",token,null)).get("data"));}
    private static Map<String,Object> post(String path,String token,Object body)throws Exception{return Json.obj(Json.obj(request(path,"POST",token,body)).get("data"));}
    private static void createUi()throws Exception{
        String username="ui_verify_"+System.currentTimeMillis();Map<String,Object> r=post("/auth/register",null,map("username",username,"password","UiVerify!6327","nickname","界面验收临时账号","email",""));long id=Json.lng(Json.obj(r.get("user")).get("id"),0);if(id<=0)throw new AssertionError("UI fixture registration failed");
        java.nio.file.Files.write(java.nio.file.Paths.get("build/ui-fixture.json"),Json.stringify(map("id",id,"username",username)).getBytes("UTF-8"));System.out.println("UI fixture username: "+username);
    }
    private static void cleanupUi()throws Exception{
        DB.init();Map<String,Object> fixture=Json.obj(Json.parse(new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("build/ui-fixture.json")),"UTF-8")));long uid=Json.lng(fixture.get("id"),0);String name=Json.str(fixture.get("username"));
        if(!name.startsWith("ui_verify_")||DB.count("SELECT COUNT(*) FROM users WHERE id=? AND username_lookup=?",uid,com.aitrainer.security.Crypto.lookup(name))!=1)throw new AssertionError("fixture mismatch, cleanup stopped");
        for(String table:Arrays.asList("learning_reports","lesson_progress","learning_profiles","exam_sessions","exam_records","practice_records","mistakes","login_logs","operation_logs"))DB.update("DELETE FROM "+table+" WHERE user_id=?",uid);
        DB.update("DELETE FROM practical_reviews WHERE submission_id IN(SELECT id FROM practical_submissions WHERE user_id=?)",uid);DB.update("DELETE FROM practical_submissions WHERE user_id=?",uid);DB.update("DELETE FROM users WHERE id=? AND username_lookup=?",uid,com.aitrainer.security.Crypto.lookup(name));java.nio.file.Files.delete(java.nio.file.Paths.get("build/ui-fixture.json"));System.out.println("UI fixture and its records removed");
    }
    private static void integration()throws Exception{
        DB.init();List<Long> users=new ArrayList<Long>();String suffix=Long.toString(System.currentTimeMillis());
        try{
            Map<String,Object> registered=post("/auth/register",null,map("username","verify_"+suffix,"password","Verify!Security6327","nickname","临时验收","email",""));
            String token=Json.str(registered.get("token"));long uid=Json.lng(Json.obj(registered.get("user")).get("id"),0);if(uid==0)throw new AssertionError("fixture registration failed");users.add(uid);
            Map<String,Object> other=post("/auth/register",null,map("username","other_"+suffix,"password","Verify!Security6327","nickname","临时验收2","email",""));long otherId=Json.lng(Json.obj(other.get("user")).get("id"),0);users.add(otherId);String otherToken=Json.str(other.get("token"));
            check(!get("/settings",null).containsKey("ai_api_key"),"public settings never return API key");
            check(!get("/learning/snapshot",token).toString().contains("NaN"),"new learner snapshot has no invalid numbers");
            check(Json.obj(get("/learning/snapshot",token).get("estimate")).get("value")==null,"new learner sees insufficient evidence");
            post("/learning/profile",token,map("institution","科教兴川","daily_minutes",75,"foundation","beginner","exam_date","2026-12-01"));check(Json.integer(get("/learning/profile",token).get("daily_minutes"),0)==75,"learning profile persisted");
            Map<String,Object> lesson=Curriculum.lesson("start-1"),qa=new LinkedHashMap<String,Object>();List<Object> quiz=Json.arr(lesson.get("quiz"));for(int i=0;i<quiz.size();i++)qa.put(""+i,Json.obj(quiz.get(i)).get("answer"));
            check(Boolean.TRUE.equals(post("/learning/check",token,map("lessonId","start-1","answers",qa)).get("passed")),"course self-check persisted");
            check(Json.integer(get("/learning/curriculum",otherToken).get("completed"),-1)==0,"course progress isolated by user");
            check(Json.integer(Json.obj(request("/learning/python-progress","GET",null,null)).get("status"),0)==401,"Python progress requires authentication");
            post("/learning/python-progress",token,map("progress",map("py-01",map("read",true,"quiz",true,"guided",true,"independent",true)),"lastLesson","py-02"));
            post("/learning/python-progress",token,map("progress",map("py-02",map("read",true))));
            post("/learning/python-progress",token,map("progress",map("py-01",map("quiz",false))));
            Map<String,Object> pyState=get("/learning/python-progress",token);
            check(Boolean.TRUE.equals(Json.obj(Json.obj(pyState.get("progress")).get("py-01")).get("quiz")),"stale Python progress cannot erase a completed milestone");
            check(Json.obj(pyState.get("progress")).size()==2&&"py-02".equals(pyState.get("lastLesson")),"Python records merge lessons and preserve bookmark");
            check(Json.obj(get("/learning/python-progress",otherToken).get("progress")).isEmpty(),"Python progress is isolated between accounts");
            check(Json.integer(Json.obj(request("/learning/python-progress","POST",token,map("userId",otherId,"progress",map()))).get("status"),0)==400,"Python progress rejects ownership spoofing");
            check(Json.integer(get("/learning/snapshot",token).get("completedLessons"),-1)==1,"Python milestones do not inflate overview or exam progress");
            post("/auth/logout",token,map());
            token=Json.str(post("/auth/login",null,map("username","verify_"+suffix,"password","Verify!Security6327")).get("token"));
            check(Json.stringify(get("/learning/python-progress",token)).equals(Json.stringify(pyState)),"Python progress survives logout and fresh login");
            java.sql.Connection pyConnection=DB.conn();
            try(java.sql.PreparedStatement stmt=pyConnection.prepareStatement("SELECT score FROM lesson_progress WHERE user_id=? AND lesson_id='python:last'")){
                stmt.setLong(1,uid);try(java.sql.ResultSet rows=stmt.executeQuery()){check(rows.next()&&rows.getString(1).startsWith("ENC:v1:"),"Python bookmark is encrypted at rest");}
            }finally{DB.release(pyConnection);}
            for(String mode:Arrays.asList("diagnostic","theory","shanghai","practical")){
                Map<String,Object> p=post("/exam/generate?mode="+mode,token,map());String id=Json.str(p.get("sessionId"));check(!id.isEmpty(),mode+" paper generated");
                List<Object> questions=Json.arr(p.get("questions"));check(questions.size()==(mode.equals("theory")?100:mode.equals("diagnostic")?18:mode.equals("shanghai")?190:4),mode+" question count");
                for(Object o:questions){Map<String,Object> q=Json.obj(o);if(q.containsKey("answer")||q.containsKey("reference_answer"))throw new AssertionError("answer leak");}
                check(id.equals(post("/exam/generate?mode="+mode,token,map()).get("sessionId")),mode+" resumes active paper");
                check(Json.integer(Json.obj(request("/exam/session/"+id,"GET",otherToken,null)).get("status"),0)==400,"exam session ownership enforced");
                check(Json.integer(Json.obj(request("/exam/save","POST",token,map("sessionId",id,"answers",map("99999999","A")))).get("status"),0)==400,"out-of-paper answer rejected");
                if(!mode.equals("practical")){
                    Map<String,Object> raw=Json.obj(Json.parse(Json.str(DB.queryOne("SELECT paper FROM exam_sessions WHERE id=?",id).get("paper"))));Map<String,Object> first=Json.obj(Json.arr(raw.get("questions")).get(0));String qid=Json.str(first.get("id"));
                    post("/exam/save",token,map("sessionId",id,"answers",map(qid,first.get("answer"))));
                    check(Json.obj(get("/exam/session/"+id,token).get("answers")).containsKey(qid),"answer survives reload");
                    Map<String,Object> result=post("/exam/submit",token,map("sessionId",id,"answers",Collections.emptyMap()));
                    check(Json.integer(result.get("total"),0)==questions.size(),"submission includes every assigned question");
                    check(Json.stringify(result).equals(Json.stringify(post("/exam/submit",token,map("sessionId",id,"answers",map(qid,"WRONG"))))),"duplicate submit is idempotent");
                    check(Json.integer(Json.obj(request("/exam/record/"+result.get("recordId"),"GET",otherToken,null)).get("status"),0)==404,"legacy record ownership enforced");
                }else{
                    DB.update("UPDATE exam_sessions SET deadline_at=? WHERE id=?",System.currentTimeMillis()-1000,id);
                    Map<String,Object> result=post("/exam/submit",token,map("sessionId",id,"answers",map(Json.str(Json.obj(questions.get(0)).get("id")),"late answer"),"review",false));
                    check("PENDING".equals(result.get("status")),"expired skill paper persists pending review");
                    check(Json.obj(get("/exam/session/"+id,token).get("answers")).isEmpty(),"late answers are not accepted");
                    Map<String,Object> graded=post("/exam/submit",token,map("sessionId",id,"answers",Collections.emptyMap(),"review",true));check(Json.integer(graded.get("score"),-1)==0,"unanswered practical gets zero without paid AI call");
                }
            }
            Map<String,Object> questions=get("/questions?type=ALL&domain=training&size=1",token);Map<String,Object> q=Json.obj(Json.arr(questions.get("list")).get(0));check(!q.containsKey("answer"),"practice answers only returned after submission");
            post("/practice/answer",token,map("questionId",q.get("id"),"answer","wrong"));post("/practice/answer",token,map("questionId",q.get("id"),"answer","wrong"));check(Json.integer(get("/learning/snapshot",token).get("uniquePractice"),0)==1,"repeated practice counts once in actual database");
            check(Json.integer(get("/questions?type=ALL&mode=wrong",token).get("total"),0)>0,"wrong-question drill filters owned mistakes");
            List<Map<String,Object>> mats=DB.query("SELECT id,materials FROM practical_tasks WHERE materials IS NOT NULL");boolean downloaded=false;
            for(Map<String,Object> row:mats){List<Object> files=Json.arr(Json.parse(Json.str(row.get("materials"),"[]")));if(files.isEmpty())continue;String name=Json.str(Json.obj(files.get(0)).get("name"));HttpURLConnection c=(HttpURLConnection)new URL(BASE+"/api/practical/"+row.get("id")+"/material?file="+URLEncoder.encode(name,"UTF-8")).openConnection();c.setRequestProperty("Authorization","Bearer "+token);downloaded=c.getResponseCode()==200;c.disconnect();break;}
            check(downloaded,"authenticated material download succeeds");
        }finally{for(long uid:users){for(String table:Arrays.asList("learning_reports","lesson_progress","learning_profiles","exam_sessions","exam_records","practice_records","mistakes","login_logs","operation_logs"))DB.update("DELETE FROM "+table+" WHERE user_id=?",uid);DB.update("DELETE FROM users WHERE id=?",uid);}}
    }
}
