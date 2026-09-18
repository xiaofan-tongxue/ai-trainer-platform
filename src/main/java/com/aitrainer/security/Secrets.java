package com.aitrainer.security;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Local Windows CurrentUser DPAPI vault. Production may inject secrets from a KMS. */
public final class Secrets {
    private static final Map<String,String> CACHE=new HashMap<String,String>();
    public static synchronized String get(String name,String env){
        String supplied=System.getenv(env);if(supplied!=null&&!supplied.isEmpty())return supplied;
        if(CACHE.containsKey(name))return CACHE.get(name);
        if(!name.matches("[a-z0-9-]+"))throw new IllegalArgumentException("Invalid secret name");
        Path path=Paths.get(System.getProperty("security.dir","runtime/security"),name+".dpapi").toAbsolutePath();
        if(!Files.isRegularFile(path))throw new IllegalStateException("缺少安全配置，请先运行 security-init.ps1 或注入环境变量 "+env);
        try{
            String script="$ErrorActionPreference='Stop'; $ProgressPreference='SilentlyContinue'; [Console]::OutputEncoding=[Text.UTF8Encoding]::new($false); Add-Type -AssemblyName System.Security; [Console]::Write([Text.Encoding]::UTF8.GetString([Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes('"+path.toString().replace("'","''")+"'),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)))";
            Process p=new ProcessBuilder("powershell.exe","-NoLogo","-NoProfile","-NonInteractive","-EncodedCommand",Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE))).start();
            if(!p.waitFor(15,TimeUnit.SECONDS)){p.destroyForcibly();throw new IllegalStateException("vault timeout");}
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] b=new byte[1024];int n;while((n=p.getInputStream().read(b))!=-1){if(out.size()+n>8192)throw new IllegalStateException("secret too large");out.write(b,0,n);}
            if(p.exitValue()!=0)throw new IllegalStateException("vault unavailable");String value=new String(out.toByteArray(),StandardCharsets.UTF_8);if(value.isEmpty())throw new IllegalStateException("empty secret");CACHE.put(name,value);return value;
        }catch(Exception e){throw new IllegalStateException("无法读取受保护配置，请使用初始化时的Windows账号启动服务");}
    }
}
