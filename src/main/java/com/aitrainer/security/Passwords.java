package com.aitrainer.security;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.*;
import java.util.*;
import com.aitrainer.util.Str;

public final class Passwords {
    private static final int ITERATIONS=600000;
    public static void policy(String p){if(p==null||p.length()<12||p.length()>128||!p.matches("(?s).*[A-Za-z].*")||!p.matches("(?s).*[0-9].*"))throw new IllegalArgumentException("密码需12–128位，至少包含字母和数字");}
    public static String hash(String p){return encode(p,"pbkdf2-sha256");}
    public static String wrapLegacy(String stored){return encode(stored,"pbkdf2-sha256-legacy");}
    private static String encode(String p,String type){byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);return type+"$"+ITERATIONS+"$"+Base64.getEncoder().encodeToString(salt)+"$"+Base64.getEncoder().encodeToString(derive(p,salt,ITERATIONS));}
    private static byte[] derive(String p,byte[] salt,int iterations){PBEKeySpec spec=new PBEKeySpec(p.toCharArray(),salt,iterations,256);try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}catch(Exception e){throw new IllegalStateException("密码派生不可用");}finally{spec.clearPassword();}}
    public static boolean verify(String p,String stored){if(p==null||p.length()>128||stored==null)return false;try{String[] parts=stored.split("\\$");if(parts.length!=4||!Arrays.asList("pbkdf2-sha256","pbkdf2-sha256-legacy").contains(parts[0]))return false;int iterations=Integer.parseInt(parts[1]);if(iterations<ITERATIONS||iterations>2000000)return false;return MessageDigest.isEqual(Base64.getDecoder().decode(parts[3]),derive(parts[0].endsWith("-legacy")?Str.sha256("aitrainer:"+p):p,Base64.getDecoder().decode(parts[2]),iterations));}catch(Exception e){return false;}}
}
