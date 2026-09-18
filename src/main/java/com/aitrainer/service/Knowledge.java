package com.aitrainer.service;

import java.util.*;
import java.util.regex.Pattern;

/** Heuristic learning tags; never represented as an official exam item taxonomy. */
public final class Knowledge {
    public static final String[] IDS = {"ethics", "foundation", "business", "training", "design", "coaching"};
    public static final String[] NAMES = {"职业道德", "基础知识", "业务分析", "智能训练", "智能系统设计", "培训与指导"};
    public static final int[] THEORY = {5, 10, 20, 30, 30, 5};
    public static final int[] PRACTICAL = {0, 0, 25, 35, 35, 5};
    private static final String[][] RULES = {
        {"coaching", "培训|讲义|教学|学员|授课|教材|教案|指导.*人员"},
        {"ethics", "道德|职业守则|诚信|敬业|职业操守|职业行为|职业素养"},
        {"foundation", "劳动法|劳动合同|知识产权|著作权|网络安全|个人信息|隐私|保密|计算机组成|操作系统|办公软件|Excel|Word|硬盘|内存|CPU|文件格式|数据库|SQL|存储|备份|恢复"},
        {"design", "人机|交互|用户体验|可用性|用户界面|监控|产品|界面设计|智能系统|漂移|可视化|推理|部署|反馈机制|系统设计"},
        {"business", "业务|流程|需求分析|数据采集|审核|业务模块|流程图|需求调研|业务场景|业务目标"},
        {"training", "训练|模型|算法|标注|清洗|特征|回归|分类|聚类|检验|统计|神经|卷积|数据集|抽样|指标|精确率|召回|过拟合|学习|决策|预测"}
    };
    public static String of(Map<String,Object> q) {
        String text = String.valueOf(q.get("question"));
        for (String[] rule : RULES) if (Pattern.compile(rule[1], Pattern.CASE_INSENSITIVE).matcher(text).find()) return rule[0];
        return "foundation";
    }
    public static String name(String id) { for (int i=0;i<IDS.length;i++) if(IDS[i].equals(id)) return NAMES[i]; return "未分类"; }
    public static String practical(String code) {
        return code.startsWith("1.") ? "business" : code.startsWith("2.") ? "training" : code.startsWith("3.") ? "design" : "coaching";
    }
    public static boolean valid(String id) { return Arrays.asList(IDS).contains(id); }
}
