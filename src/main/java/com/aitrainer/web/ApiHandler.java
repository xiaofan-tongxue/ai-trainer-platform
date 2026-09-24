package com.aitrainer.web;

import com.aitrainer.dao.ChapterDao;
import com.aitrainer.dao.QuestionDao;
import com.aitrainer.dao.SettingsDao;
import com.aitrainer.entity.User;
import com.aitrainer.service.AdminService;
import com.aitrainer.service.AuthService;
import com.aitrainer.service.ExamService;
import com.aitrainer.service.LogService;
import com.aitrainer.service.MistakeService;
import com.aitrainer.service.AiTutorService;
import com.aitrainer.service.PracticalService;
import com.aitrainer.service.QuestionService;
import com.aitrainer.service.LearningService;
import com.aitrainer.service.DeepSeekClient;
import com.aitrainer.service.Knowledge;
import com.aitrainer.service.Curriculum;
import com.aitrainer.db.DB;
import com.aitrainer.util.Json;
import com.aitrainer.util.SessionManager;
import com.aitrainer.util.Str;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApiHandler {
    private final AuthService auth = new AuthService();
    private final QuestionService questionService = new QuestionService();
    private final ExamService examService = new ExamService();
    private final PracticalService practicalService = new PracticalService();
    private final MistakeService mistakeService = new MistakeService();
    private final AdminService adminService = new AdminService();
    private final LogService logs = new LogService();
    private final ChapterDao chapterDao = new ChapterDao();
    private final QuestionDao questionDao = new QuestionDao();
    private final SettingsDao settingsDao = new SettingsDao();
    private final AiTutorService aiTutor = new AiTutorService();
    private final LearningService learning = new LearningService();

    public void handle(Request req, Response res) {
        String p = req.path; // /api/xxx
        String path = p.startsWith("/api") ? p.substring(4) : p; // 去掉 /api
        if (!path.startsWith("/")) path = "/" + path;
        String[] seg = path.split("/");
        // seg[0]="" , seg[1]=一级, seg[2]=二级...

        try {
            if(!validRoute(path,req.method)){res.fail(405,"接口路径或请求方法不支持");return;}
            route(req, res, seg);
        } catch (IllegalArgumentException e) {
            res.fail(400, e.getMessage());
        } catch (DeepSeekClient.AiFailure e) {
            res.fail(503, e.getMessage());
        } catch (Exception e) {
            System.err.println("API failure: " + req.path + " " + e.getClass().getSimpleName());
            res.fail(500, "服务暂时异常，请重试；学习记录已保存的部分会保留");
        }
    }

    private void route(Request req, Response res, String[] seg) throws Exception {
        String a = seg.length > 1 ? seg[1] : "";
        String b = seg.length > 2 ? seg[2] : "";
        String c = seg.length > 3 ? seg[3] : "";

        /* ---- 认证（公开接口） ---- */
        if (a.equals("auth")) {
            authRoute(req, res, b, c);
            return;
        }
        if (a.equals("settings") && req.method.equals("GET")) {
            res.ok(Curriculum.map("site_name",settingsDao.get("site_name","人工智能训练师三级学习平台")));
            return;
        }

        /* ---- 需要登录的接口 ---- */
        User user = currentUser(req);
        if (user == null) {
            res.unauthorized();
            return;
        }
        if(user.mustChangePassword){res.fail(403,"PASSWORD_CHANGE_REQUIRED");return;}
        if(!com.aitrainer.security.RateLimiter.allow("user:"+user.id,180,60000)){res.header("Retry-After","60");res.fail(429,"接口访问频率超限");return;}
        if((a.equals("python")||a.equals("practical")&&c.equals("submit")||a.equals("learning")&&b.equals("report")||a.equals("exam")&&b.equals("submit")||a.equals("admin")&&b.equals("ai"))&&req.method.equals("POST")&&!com.aitrainer.security.RateLimiter.allow("ai:"+user.id,20,600000)){res.fail(429,"AI与评分接口调用额度已达本时段上限");return;}

        if(a.equals("learning")) {
            if(b.equals("python-progress")){
                com.aitrainer.service.PythonProgressService progress=new com.aitrainer.service.PythonProgressService();
                res.ok(req.method.equals("GET")?progress.load(user.id):progress.save(user.id,asMap(req.json())));
            }
            else if(b.equals("curriculum")&&req.method.equals("GET"))res.ok(learning.journey(user.id));
            else if(b.equals("profile")&&req.method.equals("GET"))res.ok(learning.profile(user.id));
            else if(b.equals("profile")&&req.method.equals("POST"))res.ok(learning.saveProfile(user.id,asMap(req.json())));
            else if(b.equals("check")&&req.method.equals("POST"))res.ok(learning.check(user.id,asMap(req.json())));
            else if(b.equals("snapshot")&&req.method.equals("GET"))res.ok(learning.snapshot(user.id));
            else if(b.equals("report")&&req.method.equals("GET"))res.ok(learning.latest(user.id));
            else if(b.equals("report")&&req.method.equals("POST"))res.ok(learning.report(user.id));
            else res.fail(404,"学习接口不存在");
        } else if (a.equals("chapters")) {
            chapterRoute(req, res, user, b);
        } else if (a.equals("questions")) {
            questionRoute(req, res, user, b);
        } else if (a.equals("practice")) {
            practiceRoute(req, res, user, b);
        } else if (a.equals("exam")) {
            examRoute(req, res, user, b, c);
        } else if (a.equals("practical")) {
            practicalRoute(req, res, user, b, c);
        } else if (a.equals("mistakes")) {
            mistakeRoute(req, res, user, b);
        } else if (a.equals("profile")) {
            profileRoute(req, res, user, b);
        } else if (a.equals("python")) {
            pythonRoute(req, res, user, b);
        } else if (a.equals("admin")) {
            adminRoute(req, res, user, b, c);
        } else {
            res.fail(404, "接口不存在: " + a);
        }
    }

    /* ================= 认证 ================= */
    private void authRoute(Request req, Response res, String b, String c) {
        if (b.equals("login") && req.method.equals("POST")) {
            Map<String, Object> body = asMap(req.json());
            AuthService.AuthResult r = auth.login(
                    Json.str(body.get("username")), Json.str(body.get("password")),
                    req.ip(), req.userAgent());
            if (!r.ok) { res.fail(400, r.message); return; }
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            sessionResponse(req,res,r.token,out);
            out.put("user", r.user.toSafe());
            res.ok(out);
        } else if (b.equals("register") && req.method.equals("POST")) {
            Map<String, Object> body = asMap(req.json());
            AuthService.AuthResult r = auth.register(
                    Json.str(body.get("username")), Json.str(body.get("password")),
                    Json.str(body.get("nickname")), Json.str(body.get("email")), req.ip());
            if (!r.ok) { res.fail(400, r.message); return; }
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            sessionResponse(req,res,r.token,out);
            out.put("user", r.user.toSafe());
            res.ok(out);
        } else if (b.equals("logout") && req.method.equals("POST")) {
            SessionManager.remove(req.bearerToken());
            res.header("Set-Cookie","ait_session=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0"+(com.aitrainer.security.SecurityGate.production()?"; Secure":""));
            res.ok(true);
        } else if(b.equals("password")&&req.method.equals("POST")){
            User u=currentUser(req);if(u==null){res.unauthorized();return;}
            Map<String,Object> body=asMap(req.json());
            auth.changePassword(u,Json.str(body.get("oldPassword")),Json.str(body.get("newPassword")));
            res.header("Set-Cookie","ait_session=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0"+(com.aitrainer.security.SecurityGate.production()?"; Secure":""));
            logs.op(u.id,u.username,"PASSWORD_CHANGE","self","密码已更新，全部会话撤销",req.ip());res.ok(true);
        } else if (b.equals("me") && req.method.equals("GET")) {
            User u = currentUser(req);
            if (u == null) { res.unauthorized(); return; }
            res.ok(u.toSafe());
        } else {
            res.fail(404, "认证接口不存在");
        }
    }

    /* ================= 章节 ================= */
    private void chapterRoute(Request req, Response res, User user, String b) {
        if (b.isEmpty()) {
            res.ok(chapterDao.list());
        } else {
            long id = parseId(b);
            Map<String, Object> ch = chapterDao.findById(id);
            if (ch == null) { res.fail(404, "章节不存在"); return; }
            res.ok(ch);
        }
    }

    /* ================= 题目 ================= */
    private void questionRoute(Request req, Response res, User user, String b) {
        String type = req.query.get("type");
        int page = parseInt(req.query.get("page"), 1);
        int size = parseInt(req.query.get("size"), 20);
        size=Math.max(1,Math.min(100,size));
        if (page < 1) page = 1;
        page=Math.min(page,10000);
        int offset = (page - 1) * size;
        if (Str.empty(type)) {
            // 默认返回判断题
            type = "JUDGE";
        }
        type = "ALL".equals(type)?"ALL":normalizeType(type);
        String domain=req.query.get("domain");
        if(domain!=null&&!domain.isEmpty()&&!Knowledge.valid(domain))throw new IllegalArgumentException("未知知识领域");
        java.util.Set<Long> wrong=new java.util.HashSet<Long>();
        boolean onlyWrong="wrong".equals(req.query.get("mode"));
        if(onlyWrong)for(Map<String,Object> m:DB.query("SELECT question_id FROM mistakes WHERE user_id=? AND mastered=0",user.id))wrong.add(Json.lng(m.get("question_id"),0));
        List<Map<String,Object>> filtered=new java.util.ArrayList<Map<String,Object>>();
        for(Map<String,Object> q:questionDao.allTagged())if((type.equals("ALL")||type.equals(q.get("type")))&&(domain==null||domain.isEmpty()||domain.equals(q.get("domain")))&&(!onlyWrong||wrong.contains(Json.lng(q.get("id"),0))))filtered.add(QuestionDao.publicView(q));
        long total=filtered.size();
        List<Map<String,Object>> list=filtered.subList(Math.min(offset,filtered.size()),Math.min(offset+size,filtered.size()));
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("list", list);
        out.put("total", total);
        out.put("page", page);
        out.put("size", size);
        res.ok(out);
    }

    /* ================= 练习 ================= */
    private void practiceRoute(Request req, Response res, User user, String b) {
        if (b.equals("answer") && req.method.equals("POST")) {
            Map<String, Object> body = asMap(req.json());
            long qid = Json.lng(body.get("questionId"), 0);
            String ans = Json.str(body.get("answer"));
            Map<String, Object> r = questionService.answer(user.id, qid, ans);
            if (r.containsKey("error")) { res.fail(404, r.get("error").toString()); return; }
            res.ok(r);
        } else {
            res.fail(404, "练习接口不存在");
        }
    }

    /* ================= 考试 ================= */
    private void examRoute(Request req, Response res, User user, String b, String c) {
        if (b.equals("generate") && req.method.equals("POST")) {
            res.ok(examService.generate(user.id,req.query.containsKey("mode")?req.query.get("mode"):"theory"));
        } else if(b.equals("session")&&!c.isEmpty()&&req.method.equals("GET")) {
            res.ok(examService.session(user.id,c));
        } else if(b.equals("sessions")&&req.method.equals("GET")) {
            res.ok(examService.sessions(user.id));
        } else if(b.equals("save")&&req.method.equals("POST")) {
            Map<String,Object> body=asMap(req.json());res.ok(examService.save(user.id,Json.str(body.get("sessionId")),asMap(body.get("answers"))));
        } else if (b.equals("submit") && req.method.equals("POST")) {
            Map<String, Object> body = asMap(req.json());
            Map<String, Object> r = examService.submit(user.id,Json.str(body.get("sessionId")),asMap(body.get("answers")),Boolean.TRUE.equals(body.get("review")));
            if (r.containsKey("error")) { res.fail(400, r.get("error").toString()); return; }
            logs.op(user.id, user.username, "EXAM_SUBMIT", "exam",
                    "交卷 得分=" + r.get("score"), req.ip());
            res.ok(r);
        } else if (b.equals("records") && req.method.equals("GET")) {
            res.ok(examService.records(user.id));
        } else if (b.equals("record") && !c.isEmpty() && req.method.equals("GET")) {
            long id = parseId(c);
            Map<String, Object> rec = examService.record(user.id, id);
            if (rec == null) { res.fail(404, "考试记录不存在"); return; }
            res.ok(rec);
        } else {
            res.fail(404, "考试接口不存在");
        }
    }

    /* ================= 实操 ================= */
    private void practicalRoute(Request req, Response res, User user, String b, String c) {
        if (b.isEmpty() && req.method.equals("GET")) {
            res.ok(practicalService.list());
        } else if (b.equals("submissions") && req.method.equals("GET")) {
            res.ok(practicalService.submissions(user.id));
        } else if (c.equals("material") && req.method.equals("GET")) {
            MaterialHandler.serve(req, res, user, parseId(b));
        } else if (!b.isEmpty() && c.equals("submit") && req.method.equals("POST")) {
            long id = parseId(b);
            Map<String, Object> body = asMap(req.json());
            String content = Json.str(body.get("content"));
            Map<String, Object> r = practicalService.submit(user.id, id, content);
            if (r.containsKey("error")) { res.fail(404, r.get("error").toString()); return; }
            logs.op(user.id, user.username, "PRACTICAL_SUBMIT", r.get("taskCode") == null ? "" : r.get("taskCode").toString(),
                    "实操提交 得分=" + r.get("score"), req.ip());
            res.ok(r);
        } else if (!b.isEmpty() && req.method.equals("GET")) {
            long id = parseId(b);
            Map<String, Object> t = practicalService.detail(id);
            if (t == null) { res.fail(404, "题目不存在"); return; }
            res.ok(t);
        } else {
            res.fail(404, "实操接口不存在");
        }
    }

    /* ================= Python 练习题 AI 判题 ================= */
    private void pythonRoute(Request req, Response res, User user, String b) {
        if (b.equals("feedback") && req.method.equals("POST")) {
            Map<String, Object> body = asMap(req.json());
            Map<String, Object> r = aiTutor.feedback(
                    Json.str(body.get("title"), ""), Json.str(body.get("lesson"), ""),
                    Json.str(body.get("prompt"), ""), Json.str(body.get("code"), ""),
                    Json.str(body.get("output"), ""));
            if (r == null) {
                Map<String, Object> none = new LinkedHashMap<String, Object>();
                none.put("mode", "none");
                res.ok(none);
            } else {
                res.ok(r);
            }
        } else {
            res.fail(404, "Python 接口不存在");
        }
    }

    /* ================= 错题 ================= */
    private void mistakeRoute(Request req, Response res, User user, String b) {
        if (b.equals("stats") && req.method.equals("GET")) {
            res.ok(mistakeService.stats(user.id));
        } else if (b.equals("remove") && req.method.equals("POST")) {
            Map<String, Object> body = asMap(req.json());
            long qid = Json.lng(body.get("questionId"), 0);
            mistakeService.remove(user.id, qid);
            res.ok(true);
        } else if (b.isEmpty() && req.method.equals("GET")) {
            boolean only = !"false".equals(req.query.get("onlyUnmastered"));
            res.ok(mistakeService.list(user.id, only));
        } else {
            res.fail(404, "错题接口不存在");
        }
    }

    /* ================= 个人资料 ================= */
    private void profileRoute(Request req, Response res, User user, String b) {
        res.ok(user.toSafe());
    }

    /* ================= 管理员 ================= */
    private void adminRoute(Request req, Response res, User user, String b, String c) {
        if (!"ADMIN".equals(user.role)) { res.forbidden(); return; }
        if (b.equals("stats") && req.method.equals("GET")) {
            res.ok(adminService.stats());
        } else if (b.equals("users")) {
            if (req.method.equals("GET")) {
                res.ok(adminService.users(req.query.get("keyword")));
            } else if (req.method.equals("POST")) {
                Map<String, Object> body = asMap(req.json());
                Map<String, Object> r = adminService.createUser(
                        Json.str(body.get("username")), Json.str(body.get("password")),
                        Json.str(body.get("nickname")), Json.str(body.get("email")),
                        Json.str(body.get("role")));
                if (r.containsKey("error")) { res.fail(400, r.get("error").toString()); return; }
                logs.op(user.id, user.username, "USER_CREATE", Json.str(body.get("username")), "创建用户", req.ip());
                res.ok(r);
            } else {
                res.fail(405, "方法不支持");
            }
        } else if (b.equals("user") && !c.isEmpty()) {
            long id = parseId(c);
            String method = req.method;
            Map<String, Object> body = asMap(req.json());
            if (method.equals("PUT") || method.equals("POST")) {
                if (body.containsKey("status")) adminService.setStatus(id, Json.integer(body.get("status"), 1));
                if (body.containsKey("role")) adminService.setRole(id, Json.str(body.get("role")));
                if (body.containsKey("password")) adminService.resetPassword(id, Json.str(body.get("password")));
                logs.op(user.id, user.username, "USER_UPDATE", String.valueOf(id), "更新用户", req.ip());
                res.ok(true);
            } else if (method.equals("DELETE")) {
                if (id == user.id) { res.fail(400, "不能删除自己"); return; }
                adminService.deleteUser(id);
                logs.op(user.id, user.username, "USER_DELETE", String.valueOf(id), "删除用户", req.ip());
                res.ok(true);
            } else {
                res.fail(405, "方法不支持");
            }
        } else if (b.equals("login-logs") && req.method.equals("GET")) {
            res.ok(adminService.loginLogs(parseInt(req.query.get("limit"), 200)));
        } else if (b.equals("operation-logs") && req.method.equals("GET")) {
            res.ok(adminService.operationLogs(parseInt(req.query.get("limit"), 200)));
        } else if (b.equals("mistakes") && req.method.equals("GET")) {
            res.ok(adminService.allMistakes());
        } else if (b.equals("submissions") && req.method.equals("GET")) {
            res.ok(adminService.allSubmissions());
        } else if (b.equals("ai") && c.equals("test") && req.method.equals("POST")) {
            Map<String,Object> reply=new DeepSeekClient().complete("返回json对象，字段ok为true。","检查连接");
            if(!Boolean.TRUE.equals(reply.get("ok")))throw new DeepSeekClient.AiFailure("FORMAT","连接成功，但返回格式不符合要求");
            res.ok(Curriculum.map("connected",true,"model",new DeepSeekClient().model()));
        } else if (b.equals("ai") && req.method.equals("GET")) {
            res.ok(new DeepSeekClient().configuration());
        } else if (b.equals("ai") && req.method.equals("POST")) {
            Map<String, Object> body = asMap(req.json());
            String key=Json.str(body.get("apiKey"),"").trim(),url=Json.str(body.get("apiUrl"),DeepSeekClient.DEFAULT_URL).trim(),model=Json.str(body.get("apiModel"),DeepSeekClient.DEFAULT_MODEL).trim();
            DeepSeekClient.validateEndpoint(url);
            if(!model.matches("[A-Za-z0-9._-]{1,80}"))throw new IllegalArgumentException("模型名称格式错误");
            if(key.length()>512||key.contains("\n")||key.contains("\r"))throw new IllegalArgumentException("密钥格式错误");
            if(!key.isEmpty())settingsDao.set("ai_api_key",key);
            settingsDao.set("ai_api_url",url);
            settingsDao.set("ai_api_model",model);
            logs.op(user.id,user.username,"AI_CONFIG_CHANGE","settings","更新AI配置（不记录密钥）",req.ip());
            res.ok(true);
        } else {
            res.fail(404, "管理接口不存在");
        }

    }

    /* ================= 工具 ================= */
    private void sessionResponse(Request req,Response res,String token,Map<String,Object> out){
        SessionManager.Session session=SessionManager.get(token);
        if("cookie".equals(req.header("x-session-mode"))){res.header("Set-Cookie","ait_session="+token+"; Path=/; HttpOnly; SameSite=Strict"+(com.aitrainer.security.SecurityGate.production()?"; Secure":""));out.put("token","cookie");out.put("csrf",session.csrf);}
        else out.put("token",token);
    }
    private boolean validRoute(String p,String method){
        if(p.matches("/auth/(login|register|logout|password)"))return method.equals("POST");
        if(p.matches("/auth/me|/settings|/chapters(/[0-9]+)?|/questions|/profile|/learning/(curriculum|snapshot)|/exam/(sessions|records)|/exam/session/[A-Za-z0-9-]{36}|/exam/record/[0-9]+|/practical(/[0-9]+|/submissions|/[0-9]+/material)?|/mistakes(/stats)?|/admin/(stats|login-logs|operation-logs|mistakes|submissions)"))return method.equals("GET");
        if(p.matches("/learning/(profile|report|python-progress)|/admin/(users|ai)"))return method.equals("GET")||method.equals("POST");
        if(p.matches("/learning/check|/practice/answer|/exam/(generate|save|submit)|/practical/[0-9]+/submit|/mistakes/remove|/python/feedback|/admin/ai/test"))return method.equals("POST");
        return p.matches("/admin/user/[0-9]+")&&(method.equals("PUT")||method.equals("POST")||method.equals("DELETE"));
    }
    private User currentUser(Request req) {
        SessionManager.Session s = SessionManager.get(req.bearerToken());
        if (s == null) return null;
        User u=auth.current(s.userId);return u!=null&&u.status==1?u:null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<String, Object>();
    }

    private long parseId(String s) {
        try { return Long.parseLong(s); } catch (Exception e) { return 0; }
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private String normalizeType(String t) {
        String u = t.toUpperCase();
        if (u.startsWith("J") || u.equals("判断")) return "JUDGE";
        if (u.startsWith("S") || u.equals("单选")) return "SINGLE";
        if (u.startsWith("M") || u.equals("多选")) return "MULTIPLE";
        return "JUDGE";
    }
}
