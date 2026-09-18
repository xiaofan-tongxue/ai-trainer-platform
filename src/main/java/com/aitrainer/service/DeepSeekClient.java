package com.aitrainer.service;
import com.aitrainer.dao.SettingsDao;
import com.aitrainer.util.Json;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Semaphore;
import static com.aitrainer.service.Curriculum.map;

/** Server-only adapter. Errors never include credentials or upstream response bodies. */
public class DeepSeekClient {
    private static final Semaphore SLOTS = new Semaphore(2);
    private final SettingsDao settings = new SettingsDao();
    public static final String DEFAULT_MODEL="deepseek-flash", DEFAULT_URL="https://api.deepseek.com/chat/completions";
    private String setting(String env,String db,String fallback){String v=System.getenv(env);return v!=null&&!v.trim().isEmpty()?v.trim():settings.get(db,fallback).trim();}
    public String key(){return setting("DEEPSEEK_API_KEY","ai_api_key","");}
    public boolean configured(){return !key().isEmpty();}
    public String model(){String m=setting("DEEPSEEK_MODEL","ai_api_model",DEFAULT_MODEL);return m.isEmpty()||m.equals("deepseek-chat")||m.equals("deepseek-reasoner")?DEFAULT_MODEL:m;}
    public String endpoint(){return setting("DEEPSEEK_API_URL","ai_api_url",DEFAULT_URL);}
    public static void validateEndpoint(String endpoint){
        try{URI u=new URI(endpoint);boolean p="/chat/completions".equals(u.getPath())||"/v1/chat/completions".equals(u.getPath());
            if(!"https".equals(u.getScheme())||!"api.deepseek.com".equalsIgnoreCase(u.getHost())||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null||(u.getPort()!=-1&&u.getPort()!=443)||!p)throw new IllegalArgumentException();
        }catch(Exception e){throw new IllegalArgumentException("请使用 DeepSeek 官方 HTTPS chat/completions 地址");}
    }
    public Map<String,Object> configuration(){return map("keyConfigured",configured(),"apiUrl",endpoint(),"apiModel",model(),"keySource",System.getenv("DEEPSEEK_API_KEY")!=null&&!System.getenv("DEEPSEEK_API_KEY").trim().isEmpty()?"environment":"settings");}
    public Map<String,Object> complete(String system,String user){String k=key();if(k.isEmpty())throw new AiFailure("NOT_CONFIGURED","尚未配置 DeepSeek API Key");String u=endpoint();validateEndpoint(u);return request(u,k,model(),system,user);}
    // Package entry for a local HTTP test fixture. Public endpoints always pass validateEndpoint.
    static Map<String,Object> request(String url,String key,String model,String system,String user){
        if(!SLOTS.tryAcquire())throw new AiFailure("BUSY","AI 正在处理其他任务，请稍后重试");
        try{for(int attempt=0;attempt<2;attempt++){
            HttpURLConnection c=null;
            try{
                c=(HttpURLConnection)new URL(url).openConnection();c.setInstanceFollowRedirects(false);c.setRequestMethod("POST");c.setDoOutput(true);
                c.setConnectTimeout(10000);c.setReadTimeout(45000);c.setRequestProperty("Authorization","Bearer "+key);c.setRequestProperty("Content-Type","application/json; charset=utf-8");
                Map<String,Object> body=map("model",model,"messages",Arrays.asList(map("role","system","content",system),map("role","user","content",user)),"response_format",map("type","json_object"),"max_tokens",2400,"stream",false);
                if(model.startsWith("deepseek-"))body.put("thinking",map("type","disabled"));
                try(OutputStream out=c.getOutputStream()){out.write(Json.stringify(body).getBytes(StandardCharsets.UTF_8));}
                int status=c.getResponseCode();
                if((status==429||status>=500)&&attempt==0)continue;
                if(status==401||status==403)throw new AiFailure("AUTH","DeepSeek 密钥无效或无权限，请管理员检查配置");
                if(status==402)throw new AiFailure("BALANCE","DeepSeek 余额不足，请管理员检查账户");
                if(status==429)throw new AiFailure("RATE_LIMIT","DeepSeek 请求限流，请稍后再试");
                if(status!=200)throw new AiFailure("UPSTREAM","DeepSeek 暂不可用（HTTP "+status+"），本地学情仍可查看");
                Map<String,Object> root=Json.obj(Json.parse(read(c.getInputStream())));List<Object> choices=Json.arr(root.get("choices"));
                if(choices.isEmpty())throw new AiFailure("FORMAT","DeepSeek 返回为空，请重试");
                Map<String,Object> choice=Json.obj(choices.get(0));
                if(!"stop".equals(Json.str(choice.get("finish_reason"))))throw new AiFailure("TRUNCATED","AI 回复未完成，请重试");
                Object parsed=Json.parse(Json.str(Json.obj(choice.get("message")).get("content"),"").trim());
                if(!(parsed instanceof Map)||Json.obj(parsed).isEmpty())throw new AiFailure("FORMAT","DeepSeek 未返回有效 JSON 对象，请重试");
                return Json.obj(parsed);
            }catch(AiFailure e){throw e;}catch(IOException e){if(attempt==1)throw new AiFailure("NETWORK","DeepSeek 连接超时或网络不可用，请稍后重试");}
            catch(Exception e){throw new AiFailure("FORMAT","DeepSeek 返回格式异常，本次未生成有效评估");}
            finally{if(c!=null)c.disconnect();}
        }throw new AiFailure("NETWORK","DeepSeek 暂时无法连接");}finally{SLOTS.release();}
    }
    private static String read(InputStream stream)throws IOException{try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>512000)throw new IOException("response too large");out.write(b,0,n);}return new String(out.toByteArray(),StandardCharsets.UTF_8);}}
    public static class AiFailure extends RuntimeException{public final String code;public AiFailure(String code,String message){super(message);this.code=code;}}
    public static String text(Map<String,Object> obj,String key,int max){Object v=obj.get(key);if(!(v instanceof String)||((String)v).trim().isEmpty())throw new AiFailure("FORMAT","AI 缺少必要分析字段，请重试");return ((String)v).substring(0,Math.min(max,((String)v).length()));}
    public static List<String> texts(Object value){if(!(value instanceof List))throw new AiFailure("FORMAT","AI 分析列表格式不正确");List<String> out=new ArrayList<String>();for(Object v:Json.arr(value)){if(!(v instanceof String))throw new AiFailure("FORMAT","AI 分析格式不正确");String s=(String)v;out.add(s.substring(0,Math.min(s.length(),1000)));if(out.size()==10)break;}return out;}
}
