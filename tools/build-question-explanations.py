"""Validate cached AI drafts, retain original keys, and quarantine unresolved items.

Runs only after explicit ReviewQuestionBank maintenance. Does not call any API.
The public overlay contains no student data, credentials, or local account paths.
"""
import hashlib
import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
seed = json.loads((ROOT/'data/seed_data.json').read_text(encoding='utf-8'))

def fingerprint(q):
    canonical = [q.get('type',''), q['question'], [[o['label'],o['text']] for o in q['options']], q['answer']]
    return hashlib.sha256(json.dumps(canonical,ensure_ascii=False,separators=(',',':')).replace('\r\n','\n').encode()).hexdigest()

SOURCES = {
 'national': '人工智能训练师国家职业技能标准（2021年版）；见平台考试依据页。',
 'python': 'Python 官方教程 https://docs.python.org/zh-cn/3/tutorial/',
 'pandas': 'pandas 用户指南 https://pandas.pydata.org/docs/user_guide/',
 'sklearn': 'scikit-learn 用户指南 https://scikit-learn.org/stable/user_guide.html',
 'numpy': 'NumPy 用户指南 https://numpy.org/doc/stable/user/',
 'w3c': 'W3C 无障碍教程 https://www.w3.org/WAI/tutorials/',
 'mlbook': '《动手学深度学习》公开教材 https://zh.d2l.ai/',
 'law': '国家法律法规数据库 https://flk.npc.gov.cn/ （须另查现行条文）'
}
# Targeted editorial review of high-risk generalizations found during sampling.
NOTES = {
 641: '“缺少法定必备条款”与“合同无效”不是同一概念，不能据此直接认定选项 C 无效。法律适用须核对现行劳动合同法及具体事实，AI 原稿的推断不作为结论。',
 621: '清缓存、检查 Cookie 和广告拦截前后差异也可能帮助理解网页行为；题干未限定“高级探索”的范围，不能把原键解释成普遍事实。',
 672: '数据质量维度依框架而异，安全、可信性和可访问性可能相关。重复率主要支持唯一性检查，不能单凭它证明数据准确。',
 715: '平均相对误差也可用于回归评估，只是分母接近零时不稳定。不能仅因它不是主干指标就断言原键错误。R² 还可为负。',
 751: '随机划分适用于近似独立同分布样本。时间序列应按时间划分，同一人或设备的重复记录应按组隔离；随机抽样不是所有场景的强制原则。',
 761: '检查点保存/恢复属于训练框架的能力，监控平台可能集成它，但并非所有监控工具自带。应明确具体工具后判断。',
 829: '编码表示会影响可用算法和内存消耗。稀疏矩阵不能无条件套用会中心化并变稠密的流程；可考虑 TruncatedSVD 并核对算法限制。',
 880: '设计系统、设计工具与插件的能力不完全相同，题干未限定版本或具体功能，暂不使用原键评分能力。',
 881: '未取得对应版本 Marvel 功能证据，点击、滑动等交互不能仅凭其他原型工具的经验推定支持。',
 882: '评审反馈可以发现问题，但静态原型不能证明后端功能完整实现；须明确评审范围。'
}
EDITORIAL_OPTIONS = {
 626: ['公式能对单元格引用进行加减乘除或调用函数，这是电子表格的基本计算方式。', '查找函数可按指定键寻找对应数据，但需要明确匹配方式和未找到时的处理。', 'MATCH 等函数可定位匹配项，查找组合能关联不同表中的记录。', 'COUNT、AVERAGE 等函数可计算数量和均值，统计前应检查空值和数据类型。', '普通单元格公式主要返回计算结果，动画通常涉及其他机制，不能当作常规公式用途。'],
 641: ['约定实施违法行为涉及合同效力问题，应结合具体约定和现行法律审查。', '法定义务通常不能通过双方约定随意免除，社保条款须按现行强制性规定核对。', '缺少必备条款可能需要补正或承担责任，不宜直接等同于整个合同或条款无效。', '是否约定违约金与合同是否有效是不同问题，还要核对哪些情形允许约定违约金。', '主体合并通常涉及权利义务承继，不能仅因合并就认定合同无效。'],
 880: ['组件可复用按钮等界面元素，修改主组件便于统一设计，但应核对工具版本。', '自适应布局可以支持不同尺寸设计，但设计稿的布局能力不等于运行中的网页响应式实现。', '共享颜色和文字样式可保持视觉一致，须区分样式管理与字体授权。', '团队可维护图标组件库，是否自带某个完整图标库不能从“设计系统”一词直接推出。', '规范说明可以通过不同方式共享，文档导出能力取决于具体产品功能或插件，需核查。'],
 881: ['点击是常见原型触发方式，但本题仍须核查对应版本的实际支持。', '滑动既可能指滚动也可能指手势切换，题干应明确交互类型再查功能。', '长按需要工具提供对应触发事件，不能由支持点击推定也支持长按。', '拖拽包含按下、移动与释放过程，能否模拟取决于工具的交互能力。', '语音控制通常涉及语音输入能力，普通可点击原型不能自动具备，需具体证据。'],
 882: ['评审人员可以通过查看页面和操作原型发现布局问题，需要记录位置和影响。', '评审可讨论配色和对比度，涉及可读性时应进一步量化测量。', '观察用户完成任务可发现操作困难，但主观意见应与实际测试证据区分。', '原型评审可发现遗漏的流程，不能仅凭原型判断真实系统代码是否实现完整。', '把设计稿与品牌规范对照能识别风格偏差，需要明确使用的品牌准则。']
}

def main():
    drafts={}
    for file in sorted((ROOT/'build/question-review').glob('*.json')):
        for item in json.loads(file.read_text(encoding='utf-8'))['items']:
            assert item['id'] not in drafts, 'duplicate draft'
            drafts[item['id']]=item
    expected={i+1 for i,q in enumerate(seed['questions']) if not str(q.get('analysis') or '').strip()}
    assert set(drafts)==expected, 'incomplete or unexpected draft IDs'
    overlay={};counts=Counter()
    for index in sorted(drafts):
        item=drafts[index];q=seed['questions'][index-1]
        labels=[o['label'] for o in q['options']]
        assert [o['label'] for o in item['options']]==labels
        assert isinstance(item['needsReview'],bool)
        assert set(item['answer'])<=set(labels) and len(set(item['answer']))==len(item['answer'])
        for key in ['concept','reason','example']:
            assert isinstance(item[key],str) and item[key].strip()
        assert item['sourceIds'] and set(item['sourceIds'])<=SOURCES.keys()
        disputed=item['needsReview'] or ''.join(sorted(item['answer']))!=''.join(sorted(q['answer'])) or 'law' in item['sourceIds'] or index in NOTES
        lines=['【待复核练习题：不纳入模拟和能力评估】' if disputed else '【AI 辅助解析：尚未逐题完成专家复核】',
               '原题答案保持不变。以下为学习解释，不是官方答案；参考资料仅指向相关主题，未逐条核验出处。']
        if disputed:
            lines += ['复核提示：'+NOTES.get(index,item['reason']), '请先辨析题干条件，勿将 AI 判断直接当成新的标准答案。']
        lines += ['', '概念：'+item['concept'], '各选项的学习分析（待核对）：']
        for position,o in enumerate(item['options']):
            value=EDITORIAL_OPTIONS[index][position] if index in EDITORIAL_OPTIONS else o.get('explanation') or o.get('text')
            assert isinstance(value,str) and len(value.strip())>=8, f'invalid explanation {index}'
            assert value.strip()!=q['options'][position]['text'].strip(), f'repeated option {index}'
            lines.append(o['label']+'：'+value.strip())
        lines += ['小例子：'+item['example']]
        if index in NOTES: lines += ['编辑复核补充：'+NOTES[index]]
        lines += ['延伸阅读（主题相关，不是本题直接证据）：']+[SOURCES[k] for k in item['sourceIds']]
        key=fingerprint(q)
        assert key not in overlay
        overlay[key]={'sourceIndex':index,'examEligible':not disputed,
                      'reviewStatus':'待复核，暂停评估' if disputed else 'AI 辅助解析，待专家复核',
                      'analysis':'\n'.join(lines), 'sourceIds':item['sourceIds'],
                      'method':'DeepSeek assisted; schema checked; selected editorial review; original key retained'}
        counts['supplemented']+=1;counts['quarantined']+=disputed
    destination=ROOT/'data/question-explanations.json'
    destination.write_text(json.dumps({'version':'2026-10-26','questions':overlay},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    eligible=Counter(q['knowledge'] for q in seed['questions'] if overlay.get(fingerprint(q),{}).get('examEligible',True))
    print(json.dumps({'counts':dict(counts),'eligibleDomains':dict(eligible)},ensure_ascii=False,indent=2))

if __name__=='__main__': main()
