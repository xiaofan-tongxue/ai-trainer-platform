package com.aitrainer.security;
import com.aitrainer.util.Json;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Versioned AES-256-GCM envelopes, unique 96-bit nonce, authenticated column context. */
public final class Crypto {
    public static final String PREFIX="ENC:v1:";
    private static final SecureRandom RNG=new SecureRandom();
    private static byte[] key(){byte[] k=Base64.getDecoder().decode(Secrets.get("data-key","DATA_ENCRYPTION_KEY"));if(k.length!=32)throw new IllegalStateException("数据主密钥必须为32字节");return k;}
    public static String seal(String context,Object value){if(value==null)return null;return encrypt(context,Json.stringify(value),key());}
    public static Object open(String context,String value){if(value==null)return null;if(!value.startsWith(PREFIX))throw new IllegalStateException("发现未加密的敏感字段，需完成安全迁移");return Json.parse(decrypt(context,value,key()));}
    public static String encrypt(String context,String value,byte[] k){try{byte[] iv=new byte[12];RNG.nextBytes(iv);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(k,"AES"),new GCMParameterSpec(128,iv));cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));byte[] ct=cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));byte[] all=new byte[iv.length+ct.length];System.arraycopy(iv,0,all,0,12);System.arraycopy(ct,0,all,12,ct.length);return PREFIX+Base64.getEncoder().encodeToString(all);}catch(Exception e){throw new IllegalStateException("敏感数据加密失败");}}
    public static String decrypt(String context,String value,byte[] k){try{if(!value.startsWith(PREFIX))throw new IllegalArgumentException();byte[] all=Base64.getDecoder().decode(value.substring(PREFIX.length()));if(all.length<28)throw new IllegalArgumentException();Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(k,"AES"),new GCMParameterSpec(128,Arrays.copyOf(all,12)));cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));return new String(cipher.doFinal(Arrays.copyOfRange(all,12,all.length)),StandardCharsets.UTF_8);}catch(Exception e){throw new IllegalStateException("敏感数据完整性校验失败或密钥不匹配");}}
    public static String lookup(String value){return mac("account-index",value.toLowerCase(Locale.ROOT));}
    public static String mac(String purpose,String value){try{Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(key(),"HmacSHA256"));byte[] digest=m.doFinal((purpose+"\u0000"+value).getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:digest)s.append(String.format("%02x",b&255));return s.toString();}catch(Exception e){throw new IllegalStateException("认证摘要计算失败");}}
}
