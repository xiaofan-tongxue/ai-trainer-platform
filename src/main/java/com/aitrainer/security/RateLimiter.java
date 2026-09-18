package com.aitrainer.security;
import java.util.*;
public final class RateLimiter {
    private static final Map<String,long[]> WINDOWS=new HashMap<String,long[]>();
    public static synchronized boolean allow(String key,int max,long windowMs){long now=System.currentTimeMillis();WINDOWS.entrySet().removeIf(e->e.getValue()[1]<=now);long[] w=WINDOWS.get(key);if(w==null){if(WINDOWS.size()>=10000)return false;w=new long[]{0,now+windowMs};WINDOWS.put(key,w);}return ++w[0]<=max;}
}
