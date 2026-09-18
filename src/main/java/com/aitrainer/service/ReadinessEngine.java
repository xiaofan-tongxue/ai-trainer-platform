package com.aitrainer.service;

import java.util.*;
import com.aitrainer.util.Json;
import static com.aitrainer.service.Curriculum.map;

/** Transparent practice-based estimate. No historical outcome calibration is claimed. */
public final class ReadinessEngine {
    public static final String METHOD="heuristic-v2";
    public static Map<String,Object> evaluate(List<Map<String,Object>> exams,List<Map<String,Object>> practice){
        List<Double> theory=new ArrayList<Double>(),skills=new ArrayList<Double>();
        double[] earned=new double[6],max=new double[6];int[] counts=new int[6];
        Set<String> seen=new HashSet<String>();List<Object> history=new ArrayList<Object>();int excluded=0;
        for(Map<String,Object> e:exams){
            String mode=Json.str(e.get("mode"));Map<String,Object> r=Json.obj(e.get("result"));
            if(!Boolean.TRUE.equals(r.get("complete")))continue;
            double score=num(r.get("score"));boolean valid=Double.isFinite(score)&&score>=0&&score<=100;
            if(mode.equals("theory"))valid&=Json.lng(r.get("durationSec"),0)>=60;
            else if(mode.equals("practical")){
                valid&=Json.lng(r.get("durationSec"),0)>=120&&"deepseek_review".equals(r.get("gradingMode"));
                int reviewed=0;for(Object d:Json.arr(r.get("detail")))if("deepseek_review".equals(Json.obj(d).get("mode")))reviewed++;
                valid&=reviewed==4;
            }else valid=false;
            history.add(map("mode",mode,"date",e.get("submitted_at"),"score",r.get("score"),"eligible",valid));
            if(!valid){excluded++;if(!mode.equals("diagnostic"))continue;}
            else (mode.equals("theory")?theory:skills).add(score);
            Map<String,Object> domains=Json.obj(r.get("domains"));
            for(int i=0;i<6;i++){Map<String,Object> d=Json.obj(domains.get(Knowledge.IDS[i]));double m=num(d.get("max")),s=num(d.get("score"));if(m>0&&Double.isFinite(s)){earned[i]+=s;max[i]+=m;counts[i]++;}}
        }
        for(Map<String,Object> p:practice){String id=Json.str(p.get("question_id"));if(!seen.add(id))continue;
            String domain=Json.str(p.get("domain"));for(int i=0;i<6;i++)if(Knowledge.IDS[i].equals(domain)){earned[i]+=Json.integer(p.get("is_correct"),0)==1?1:0;max[i]++;counts[i]++;}
        }
        List<Map<String,Object>> domains=new ArrayList<Map<String,Object>>();
        for(int i=0;i<6;i++){Double mastery=max[i]>0?round(100*earned[i]/max[i]):null;
            // A domain with no observations is a learning priority, not a zero score.
            double priority=(mastery==null?80:100-mastery)*(Knowledge.THEORY[i]+Knowledge.PRACTICAL[i])/2.0;
            domains.add(map("id",Knowledge.IDS[i],"name",Knowledge.NAMES[i],"mastery",mastery,"observations",counts[i],"priority",round(priority),"theoryWeight",Knowledge.THEORY[i],"practicalWeight",Knowledge.PRACTICAL[i],"action",action(Knowledge.IDS[i]),"link","/practice.html?domain="+Knowledge.IDS[i]));
        }
        Collections.sort(domains,(a,b)->Double.compare(num(b.get("priority")),num(a.get("priority"))));
        boolean enough=theory.size()>=3&&skills.size()>=2;
        Map<String,Object> estimate=map("available",enough,"label",enough?"双科通过可能性 · 规则估计":"测评记录不足","confidence","低：尚无科教兴川考后结果校准","value",null,"low",null,"high",null,"method",METHOD);
        if(enough){double t=mean(theory),s=mean(skills),tn=Math.max(8,sd(theory)+5),sn=Math.max(12,sd(skills)+8);
            estimate.put("value",Math.round(100*prob(t,tn)*prob(s,sn)));
            estimate.put("low",Math.round(100*prob(t-5,tn)*prob(s-10,sn)));
            estimate.put("high",Math.round(100*prob(t+5,tn)*prob(s+10,sn)));
        }
        estimate.put("explanation","近30天有效理论至少3次、技能至少2次。以各科均分距60分的差值和波动估计单科可能性，再相乘；区间为分数扰动的敏感性范围，不是统计置信区间。题库重复、AI审阅误差与实际考场差异均可能使估计偏高。");
        estimate.put("formula","p=1/(1+exp(-(均分-60)/尺度)); 理论尺度=max(8,标准差+5)，技能尺度=max(12,标准差+8)；双科=p理论×p技能；下/上界将理论均分±5、技能均分±10。");
        List<String> gaps=new ArrayList<String>();if(theory.size()<3)gaps.add("补充 "+(3-theory.size())+" 次国标覆盖理论模拟");if(skills.size()<2)gaps.add("补充 "+(2-skills.size())+" 次四领域完整技能模拟并完成 DeepSeek 审阅");
        if(enough&&(mean(theory)<75||mean(skills)<75))gaps.add("优先修补薄弱科目，争取两科连续达到75分以上的自定训练目标");
        return map("estimate",estimate,"domains",domains,"theory",stats(theory),"practical",stats(skills),"gaps",gaps,"uniquePractice",seen.size(),"excludedExams",excluded,"history",history,"windowDays",30,"evidenceRule","仅纳入近30天国标覆盖模拟；理论作答至少60秒，技能至少120秒且四题均完成AI审阅。诊断、上海补充卷、速交空卷、关键词分与课程自测不用于概率计算。这些筛选仍不能证明独立完成。");
    }
    public static String action(String id){if(id.equals("ethics"))return "复习授权、脱敏与数据安全，完成10道专项题并解释错因";if(id.equals("foundation"))return "完成Python变量、循环和函数练习，再独立读写一份CSV";if(id.equals("business"))return "绘制采集→处理→审核流程，补充异常与验收规则";if(id.equals("training"))return "独立完成清洗、划分、训练与测试，检查泄漏并解释指标";if(id.equals("design"))return "完成监控指标、异常归因和人机交互方案，写出验收条件";return "制作一份面向初级学员的教案，设计演示、练习和验收";}
    private static Map<String,Object> stats(List<Double> a){return map("count",a.size(),"average",a.isEmpty()?null:round(mean(a)),"latest",a.isEmpty()?null:a.get(0),"deviation",a.size()<2?null:round(sd(a)));}
    private static double prob(double m,double scale){return 1/(1+Math.exp(-(m-60)/scale));}
    private static double mean(List<Double> a){double v=0;for(double n:a)v+=n;return a.isEmpty()?0:v/a.size();}
    private static double sd(List<Double> a){double m=mean(a),v=0;for(double n:a)v+=(n-m)*(n-m);return a.size()<2?0:Math.sqrt(v/(a.size()-1));}
    private static double round(double n){return Math.round(n*10)/10.0;}
    private static double num(Object o){return o instanceof Number?((Number)o).doubleValue():Double.NaN;}
}
