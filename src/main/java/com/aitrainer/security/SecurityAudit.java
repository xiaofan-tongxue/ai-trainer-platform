package com.aitrainer.security;
import com.aitrainer.web.Request;
import com.aitrainer.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static com.aitrainer.service.Curriculum.map;

/** Encrypted daily records with HMAC chaining. Export daily final hash to independent storage. */
public final class SecurityAudit {
    private static String activeDay="",previous="GENESIS";
    public static synchronized void record(Request r,int status){
        try{
            String day=LocalDate.now().toString();Path dir=Paths.get(System.getProperty("security.dir","runtime/security"),"audit");Files.createDirectories(dir);Path file=dir.resolve(day+".jsonl");
            if(!day.equals(activeDay)){previous=Files.exists(file)?verify(file):"GENESIS";activeDay=day;}
            SessionManager.Session session=SessionManager.get(r.bearerToken());
            String payload=Crypto.seal("audit.event",map("time",Instant.now().toString(),"requestId",UUID.randomUUID().toString(),"userId",session==null?null:session.userId,"method",r.method,"path",r.path,"ip",r.ip(),"status",status));
            String mac=Crypto.mac("audit-chain",previous+"|"+payload);String line=Json.stringify(map("previous",previous,"payload",payload,"mac",mac));
            Files.write(file,(line+"\n").getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);previous=mac;
        }catch(Exception e){System.err.println("SECURITY_AUDIT_WRITE_FAILURE：请检查审计存储与访问权限");}
    }
    public static String verify(Path p)throws Exception{String prev="GENESIS";for(String line:Files.readAllLines(p,StandardCharsets.UTF_8)){Map<String,Object> row=Json.obj(Json.parse(line));String payload=Json.str(row.get("payload"));if(!prev.equals(row.get("previous"))||!Crypto.mac("audit-chain",prev+"|"+payload).equals(row.get("mac")))throw new IllegalStateException("审计链校验失败");Crypto.open("audit.event",payload);prev=Json.str(row.get("mac"));}return prev;}
    public static void main(String[] args)throws Exception{Path dir=Paths.get(System.getProperty("security.dir","runtime/security"),"audit");if(!Files.exists(dir)){System.out.println("尚无审计文件");return;}try(java.util.stream.Stream<Path> files=Files.list(dir)){for(Path p:(Iterable<Path>)files.filter(f->f.toString().endsWith(".jsonl"))::iterator)System.out.println(p.getFileName()+" VERIFIED finalHmac="+verify(p));}}
}
