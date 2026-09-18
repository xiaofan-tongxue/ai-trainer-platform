package com.aitrainer.security;
import java.util.*;
/** Data classification inventory also consumed by the migration and at-rest verifier. */
public final class ProtectedFields {
    public static final Map<String,List<String>> TABLES=new LinkedHashMap<String,List<String>>();
    static{
        add("users","username nickname email");add("settings","v");add("login_logs","username ip user_agent");add("operation_logs","username target detail ip");
        add("practice_records","user_answer is_correct");add("exam_records","detail score total correct passed duration_sec");add("practical_submissions","content score analysis");
        add("exam_sessions","paper answers result");add("learning_reports","report");add("learning_profiles","exam_date daily_minutes foundation institution");add("lesson_progress","score");
    }
    private static void add(String table,String fields){TABLES.put(table,Arrays.asList(fields.split(" ")));}
    public static boolean contains(String table,String field){return TABLES.containsKey(table)&&TABLES.get(table).contains(field);}
    public static String seal(String table,String field,Object v){return Crypto.seal(table+"."+field,v);}
}
