"""Build versioned teaching corrections without replacing an installation's database."""
import hashlib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
seed = json.loads((ROOT/'data/seed_data.json').read_text(encoding='utf-8'))
solutions = ROOT/'data/practical-solutions'
solutions.mkdir(exist_ok=True)

COMMON = '''import json
from pathlib import Path
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
from sklearn.impute import SimpleImputer
from sklearn.preprocessing import StandardScaler, OneHotEncoder

# 数据先划分。填补值、标准化参数、类别编码只从训练集学习。
def prepare(X):
    numeric = X.select_dtypes(include="number").columns.tolist()
    categorical = [c for c in X.columns if c not in numeric]
    return ColumnTransformer([
        ("number", Pipeline([("fill", SimpleImputer(strategy="median")),
                             ("scale", StandardScaler())]), numeric),
        ("category", Pipeline([("fill", SimpleImputer(strategy="most_frequent")),
                               ("encode", OneHotEncoder(handle_unknown="ignore"))]), categorical)
    ])

def clean_columns(df):
    df.columns = df.columns.str.strip()
    return df.drop(columns=[c for c in df if c.startswith("Unnamed:") or c in
                           ["Timestamp", "Your name", "序号", "所用时间", "car name"]], errors="ignore").drop_duplicates()

'''

LOADERS = {
 'auto': '''df = clean_columns(pd.read_csv("auto-mpg.csv", na_values=["?"]))
target = "mpg"
df[target] = pd.to_numeric(df[target], errors="coerce")
df = df.dropna(subset=[target])
X, y = df.drop(columns=[target]), df[target]
''',
 'finance': '''df = clean_columns(pd.read_csv("finance数据集.csv"))
target = "SeriousDlqin2yrs"
df[target] = pd.to_numeric(df[target], errors="coerce")
df = df[df[target].isin([0, 1])].copy()
X, y = df.drop(columns=[target]), df[target].astype(int)
''',
 'proxy': '''df = clean_columns(pd.read_excel("大学生低碳生活行为的影响因素数据集.xlsx"))
# 这四项问卷均值只是演示用代理目标，不是经过外部验证的行为测量标准。
label_columns = ["15.日常生活中，我会......—对垃圾进行分类", "重复利用废旧物品", "避免使用一次性产品", "劝说周围的人进行低碳生活"]
labels = df[label_columns].apply(pd.to_numeric, errors="coerce")
df = df.loc[labels.notna().all(axis=1)].copy()
y = labels.loc[df.index].mean(axis=1)
X = df.drop(columns=label_columns)
assert not set(label_columns).intersection(X.columns), "目标构成项不能作为输入特征"
''',
 'steps': '''df = clean_columns(pd.read_csv("fitness analysis.csv"))
target = "daily_steps"
df[target] = pd.to_numeric(df[target], errors="coerce")
df = df.dropna(subset=[target])
X, y = df.drop(columns=[target]), df[target]
'''
}

def preprocessing(loader, code, classification=False):
    return COMMON + LOADERS[loader] + f'''
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify={'y' if classification else 'None'})
processor = prepare(X_train)
train_features = processor.fit_transform(X_train)
test_features = processor.transform(X_test)
assert len(X_train) and len(X_test) and set(X_train.index).isdisjoint(X_test.index)
# 导出原始单位的数据与划分标记，便于复核；不能把目标和标准化特征混写。
output = X.copy()
output["target"] = y
output["split"] = np.where(output.index.isin(X_train.index), "train", "test")
output.to_csv("{code}_cleaned_data.csv", index=False, encoding="utf-8-sig")
print("划分后特征形状：", train_features.shape, test_features.shape)
print("已生成 {code}_cleaned_data.csv；target 保留原单位，split 记录划分。")
'''

def modeling(loader, code, model, classification=False):
    return COMMON + '''from sklearn.linear_model import LogisticRegression, LinearRegression
from sklearn.ensemble import RandomForestRegressor, RandomForestClassifier
from sklearn.tree import DecisionTreeRegressor
from sklearn.metrics import (accuracy_score, f1_score, confusion_matrix, roc_auc_score,
                             mean_absolute_error, mean_squared_error, r2_score)
from sklearn.base import clone

''' + LOADERS[loader] + f'''
# 先留出最终测试集，再从剩余数据中分出验证集。验证集可用于调参。
X_dev, X_test, y_dev, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify={'y' if classification else 'None'})
X_train, X_valid, y_train, y_valid = train_test_split(
    X_dev, y_dev, test_size=0.25, random_state=42, stratify={'y_dev' if classification else 'None'})
assert set(X_train.index).isdisjoint(X_valid.index)
assert set(X_dev.index).isdisjoint(X_test.index)
model = Pipeline([("prepare", prepare(X_train)), ("model", {model})])
model.fit(X_train, y_train)
print("验证集评分：", round(model.score(X_valid, y_valid), 4))
# 本例固定超参数。练习改进只能看验证结果，确定方案后才打开最终测试集。
final_model = clone(model).fit(X_dev, y_dev)
prediction = final_model.predict(X_test)
''' + ('''
metrics = {"accuracy": float(accuracy_score(y_test, prediction)),
           "macro_f1": float(f1_score(y_test, prediction, average="macro", zero_division=0)),
           "labels": final_model.classes_.tolist(),
           "confusion_matrix": confusion_matrix(y_test, prediction, labels=final_model.classes_).tolist()}
if len(final_model.classes_) == 2:
    metrics["roc_auc"] = float(roc_auc_score(y_test, final_model.predict_proba(X_test)[:, 1]))
''' if classification else '''
metrics = {"MAE": float(mean_absolute_error(y_test, prediction)),
           "RMSE": float(np.sqrt(mean_squared_error(y_test, prediction))),
           "R2": float(r2_score(y_test, prediction))}
# MAE、RMSE 与目标同单位；R² 可以为负，不是分类准确率。
''') + f'''
metrics.update(train_rows=len(X_train), validation_rows=len(X_valid), test_rows=len(X_test), random_state=42)
Path("{code}_metrics.json").write_text(json.dumps(metrics, ensure_ascii=False, indent=2), encoding="utf-8")
assert all(np.isfinite(v) for v in metrics.values() if isinstance(v, float))
print(json.dumps(metrics, ensure_ascii=False, indent=2))
print("已生成 {code}_metrics.json；一次固定划分只提供本数据集的测试证据。")
'''

def revision_source(code):
    return (ROOT/'data/practical-revisions'/(code+'.py')).read_text(encoding='utf-8')

codes = {
 '2.1.1': preprocessing('auto','2.1.1'),
 '2.1.2': revision_source('2.1.2'),
 '2.1.3': preprocessing('finance','2.1.3',True),
 '2.1.5': revision_source('2.1.5'),
 '2.2.1': modeling('finance','2.2.1','LogisticRegression(max_iter=1500, class_weight="balanced")',True),
 '2.2.2': modeling('auto','2.2.2','RandomForestRegressor(n_estimators=100, max_depth=8, random_state=42, n_jobs=2)'),
 '2.2.3': revision_source('2.2.3'),
 '2.2.4': modeling('proxy','2.2.4','LinearRegression()'),
 '2.2.5': modeling('steps','2.2.5','DecisionTreeRegressor(max_depth=8, min_samples_leaf=5, random_state=42)')
}
codes['2.1.4'] = '''import json
from pathlib import Path
import numpy as np
import pandas as pd

df = pd.read_csv("medical_data.csv", encoding="gbk").drop_duplicates()
counts = {"input_rows": len(df)}
# 示例边界必须由业务方确认；越界设为缺失并记录，不凭空补造医疗事实。
for col, low, high in [("年龄", 0, 120), ("身高", 30, 250), ("体重", 1, 400)]:
    values = pd.to_numeric(df[col], errors="coerce")
    invalid = values.notna() & ~values.between(low, high)
    counts[col + "_invalid"] = int(invalid.sum())
    df[col] = values.where(values.between(low, high))
for col in ["就诊日期", "诊断日期"]:
    df[col] = pd.to_datetime(df[col], errors="coerce")
df["日期待核查"] = df["诊断日期"] < df["就诊日期"]
df["BMI"] = df["体重"] / (df["身高"] / 100) ** 2
assert not np.isinf(df["BMI"].dropna()).any()
# 去除直接标识，不打印患者明细；原文件仍只留在本地。
df = df.drop(columns=[c for c in df if any(k in c for k in ["姓名", "电话", "身份证", "患者ID"])])
df.to_csv("2.1.4_cleaned_data.csv", index=False, encoding="utf-8-sig")
counts["output_rows"] = len(df)
counts["date_review_rows"] = int(df["日期待核查"].sum())
Path("2.1.4_quality.json").write_text(json.dumps(counts, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(counts, ensure_ascii=False, indent=2))
print("输出仅用于教学；字段去除不等于已完成匿名化或可公开医疗数据。")
'''

# 题面修正只放在这份叠加资料里。已有安装按原参考答案指纹匹配后读取，不需要重新导入数据库。
# assessmentRevision 用来挡住修订前已经生成的旧试卷；没有该标记的历史成绩仍不计入能力评估。
REVISIONS = {
 '2.1.2': {
  'examEligible': False,
  'taskTitle': '低碳行为标签质量检查：常量目标不能训练',
  'taskDesc': '所附“大学生低碳生活行为的影响因素数据集”有318行。原要求把“低碳行为积极性”当作预测目标，但该列318行全部为0。本题只做标签质量检查：加载数据，统计缺失、重复和目标列取值，确认它不能区分样本。不要编造、插补或替换标签，也不要继续标准化、划分或训练。把结论写入2.1.2_quality.json，并在答题卷里写明：标签来源未核实之前不能建立预测模型。补全2.1.2.ipynb。这是辨错练习，不进入技能模拟，也不做AI评分。',
  'taskSkills': '（1）能够在训练前检查标签有没有区分度，并在标签无效时停止建模；',
  'taskQuality': '（1）质量报告与原始数据一致，没有编造标签或伪造可训练指标；',
  'notice': '所附数据中“低碳行为积极性”318条记录全部为0。本题只练习标签质量检查，不要编造标签。没有可用预测目标，因此不纳入技能模拟和 AI 评分。',
  'firstStep': '先统计“低碳行为积极性”有几个不同取值。只有一个取值时，写出不能训练的证据并停止。',
  'checklist': ['记录行数、缺失单元格数和重复行数', '写出目标列每个取值及其行数，并判断能否训练', '说明不要编造标签；月生活费是选项代号，不是连续金额'],
  'explanation': '先核对目标列的取值个数。只有一个取值时，清洗统计可以做，训练不能做。配套目标列为常量。不要复制报告里的数字代替自己的运行结果。'
 },
 '2.1.5': {
  'examEligible': True,
  'assessmentRevision': '2026-11-11-age-interval',
  'taskTitle': '健康咨询数据预处理：把年龄区间编码为类别',
  'taskDesc': '健康咨询客户数据中的 Your age 是“15 to 18”“19 to 25”这样的区间，不是精确岁数。请补全2.1.5.ipynb：查看类型、缺失和重复；删除含缺失值的行并去重；按给定顺序把 Your age 编码为有序类别，保留区间原文。不要用 to_numeric、区间端点或区间中点冒充年龄。健身水平也按 Unfit、Average、Good、Very good、Perfect 编码为类别，不要把文字等级归一化成连续测量。绘制运动频率饼图。以年龄区间为标注对象，按区间分层做 8:2 划分（random_state=42），保存 2.1.5_cleaned_data.csv。答题卷说明为什么区间不能写成精确年龄。',
  'taskSkills': '（1）能够区分区间类别和精确数值，并按已知类别顺序编码；',
  'taskQuality': '（1）输出仍能看到原始年龄区间，没有任何一列被写成整数岁；',
  'notice': '年龄是区间文字。任务已改为按类别编码并保留原文，不再要求转成精确年龄。题面、笔记本和参考脚本一致，可以纳入技能模拟。修订前的旧试卷没有本次标记，不计入能力评估。',
  'firstStep': '先打印 Your age 的原文取值。确认它们是区间后，再按给定顺序编码，不要转成整数岁。',
  'checklist': ['年龄列导出后仍是区间原文，没有整数岁', '健身水平按给定顺序编码为类别，而不是连续测量', '保存清洗结果、运动频率图，以及按年龄区间分层的划分标记'],
  'explanation': '年龄区间保持文字。类别顺序只表示区间的先后，不是岁数，也不能用端点代替真实年龄。划分只服务后续标注。'
 },
 '2.2.3': {
  'examEligible': True,
  'assessmentRevision': '2026-11-11-age-interval-classification',
  'taskTitle': '年龄区间分类：用问卷特征预测年龄段',
  'taskDesc': '健身问卷里没有“日常运动量”这个连续字段。原标题写运动量，正文写预测年龄；表中的 Your age 实际是年龄区间。本题统一为年龄区间分类：用性别、对运动重要性的评分和健康自评，预测 Your age 的区间类别。不要把区间左端点当成精确年龄，不要把类别编号做回归，也不要报告 R²。先划分训练、验证和测试。只在验证集上用宏平均 F1 比较随机森林分类器和逻辑回归，选定后再用训练加验证重训，并只查看一次测试集的准确率、宏平均 F1 和混淆矩阵。人数最多的区间会抬高准确率，所以以宏平均 F1 为准。补全2.2.3.ipynb，保存 2.2.3_metrics.json，并在答题卷分析错例。',
  'taskSkills': '（1）能够按目标的真实类型选择分类，并用验证集选择模型、用测试集做一次评价；',
  'taskQuality': '（1）指标来自真实运行；同时报告宏平均 F1、各类人数和混淆矩阵，不把类别编号之间的距离当成误差；',
  'notice': '数据没有运动量目标，年龄是区间。任务已统一为年龄区间分类，并用分类指标评价。可以纳入技能模拟。修订前把类别编号做回归的旧试卷不计入能力评估。',
  'firstStep': '先确认预测目标是 Your age 的区间文字，再选择分类器。不要把区间换成整数后做回归。',
  'checklist': ['目标保持为年龄区间，输入只有性别、运动重要性和健康自评', '验证集比较分类器，测试集只评价一次', '提交真实的准确率、宏平均 F1、各类人数和混淆矩阵'],
  'explanation': '先读注释。验证集只用来比较分类器，测试集只评价一次。不要把年龄区间换成整数再做回归。准确率接近最大区间占比时，要看宏平均 F1。'
 },
 '3.2.4': {
  'examEligible': False,
  'taskTitle': '辨错：不要凭文件名判断花卉识别模型',
  'taskDesc': '素材文件名叫 flower-detection.onnx，原题要求识别花朵。请先核对文件，再决定能不能做花卉功能。计算该文件的 SHA-256，并与已核对的 ImageNet ResNet 摘要 af16a04a6ec48ac494065d4439fe9dea590d337b9ca6dc328160ccf04a217b9c 比较；再统计 labels.txt 是否为 1000 个 ImageNet 类别。摘要相同且标签是 ImageNet 时，不要把它写成花卉专用模型。权重存在时，可以对 flower_test.png 做一次通用图像推理，输出类别和 softmax 置信度。置信度不是准确率。即使最高类别碰巧是 daisy 这种花卉单词，它也只是 ImageNet 1000 类中的一类，同一份权重还会输出鱼类、器物等类别。答题卷写下核对结论：该文件不能支持花卉识别功能。补全3.2.4.ipynb。这是辨错练习，不进入技能模拟，也不做AI评分。',
  'taskSkills': '（1）能够用文件摘要、标签表和实际输出核对模型用途，而不只看文件名；',
  'taskQuality': '（1）结论与文件内容一致：没有把整份模型说成花卉专用，也没有把置信度写成准确率；',
  'notice': 'flower-detection.onnx 与 3.2.1 的 resnet.onnx SHA-256 同为 af16a04a6ec48ac494065d4439fe9dea590d337b9ca6dc328160ccf04a217b9c，labels.txt 是 ImageNet 1000 类。文件名不能证明花卉识别。本题只练习核对模型文件，不纳入技能模拟和 AI 评分。',
  'firstStep': '先读文件摘要和 labels.txt 的类别数。确认它与 ImageNet ResNet 相同后，再决定是否推理；不要按文件名设计花卉产品。',
  'checklist': ['记录 SHA-256、是否与已知 ImageNet ResNet 相同，以及标签数量', '若做推理，写明输出是置信度；类别来自 ImageNet，不是花卉名录', '结论明确：不能按文件名上线花卉识别'],
  'explanation': '先核对摘要和标签数量，再决定是否推理。softmax 是单张图片的置信度。预测词来自 ImageNet；即使出现 daisy，也不能证明这是花卉专用模型。'
 }
}
GUIDES = {
 '1.1': ('先读懂输入字段与业务目标，再画出数据从采集到审核的流向。', ['列出字段名、类型、单位、允许值和缺失规则', '按输入→处理→输出→复核写流程，并标明责任人', '用至少一个正常样本和一个异常样本验证规则'], ['bridge-process', 'bridge-clean']),
 '1.2': ('先找瓶颈，再提出能够测量效果的改进方案。', ['写清当前流程、等待点、返工原因和证据', '给出改进前后流程、风险与回退办法', '设定效率、质量、成本的验收口径与抽检方案'], ['bridge-improve', 'bridge-quality']),
 '2.1': ('先观察字段，再清洗；涉及建模时先划分数据，最后拟合预处理器。', ['记录输入行数、字段、缺失比例和重复数', '说明每条清洗规则及其依据，保留清洗前后统计', '把标注对象、标签、边界例、复核与争议裁决写入规范'], ['bridge-clean', 'bridge-text', 'bridge-split']),
 '2.2': ('先写清预测什么，再选择分类或回归，最后用独立测试集评价。', ['明确目标字段及单位，删除目标泄漏和身份字段', '记录训练/验证/测试划分、随机种子与训练参数', '提交真实指标、错误案例、改进假设与未解决限制'], ['bridge-pipeline', 'bridge-metrics', 'bridge-errors']),
 '3.1': ('先根据表格计算指标，再提出优化建议；参考文案中的数字必须自行核算。', ['写明指标分子、分母、单位、时间范围和缺失处理', '计算总体和分组统计，画图并标明样本量', '用证据区分现象与原因，提出可验证的改进及告警阈值'], ['bridge-monitor', 'bridge-errors']),
 '3.2': ('先运行一次模型推理，再设计用户如何输入、纠错、理解和导出结果。', ['核对模型、标签、输入尺寸、通道和数据类型', '保存实际输出并解释置信度与准确率的区别', '设计正常、空输入、错误文件、低置信度、失败重试和隐私处理'], ['bridge-interaction', 'bridge-metrics']),
 '4.1': ('把“讲什么”改成“学员最终独立做出什么”，每个知识点接演示和练习。', ['写对象、前置知识、可观察学习目标与时间安排', '设计讲解→示范→跟做→独立练习，给出具体练习材料', '准备评分标准、常见错误、补救步骤和课后复测'], ['bridge-teach', 'bridge-guide']),
 '4.2': ('指导方案应让另一位学员照着完成工作，并能发现和处理异常。', ['写工具准备、授权范围、操作步骤和每步检查点', '给出合格/不合格样例、质量阈值、抽检及争议升级路径', '安排示范、学员复述操作、独立验收与反馈记录'], ['bridge-guide', 'bridge-delivery'])
}

tasks = {}
lesson_titles = {l['id']:l['title'] for l in json.loads((ROOT/'data/bridge-course.json').read_text(encoding='utf-8'))['lessons']}
for t in seed['practical']:
    code = t['code']; prefix = code[:3]
    entry, checks, lessons = GUIDES[prefix]
    reference = t['reference']
    original_hash = hashlib.sha256(reference.replace('\r\n','\n').encode()).hexdigest()
    if code.startswith('3.2.') and code not in REVISIONS:
        # Last block is the complete script; remove platform-specific folder assumption.
        source = re.findall(r'```python\s*\n(.*?)```', reference, re.S)[-1]
        source = source.replace("'onnx/", "'").replace('"onnx/', '"')
        source = source.replace('Image.ANTIALIAS', 'Image.Resampling.LANCZOS')
        source = source.replace('准确率', '模型置信度')
        codes[code] = source
    revision = REVISIONS.get(code, {})
    if (ROOT/'data/practical-revisions'/(code+'.py')).is_file():
        codes[code] = (ROOT/'data/practical-revisions'/(code+'.py')).read_text(encoding='utf-8')
    if code in codes:
        source = codes[code].strip()+'\n'
        (solutions/(code+'.py')).write_text(source, encoding='utf-8')
        explanation = revision.get('explanation',
                       '先阅读每段中文注释，再逐段运行。训练集用于学习参数；验证集用于比较方案；最终测试集仅在方案确定后评价。不要复制示例数字作为自己的运行结果。'
                       if code.startswith('2.') else '核对当前目录与素材文件名，再核对输入维度。这里仅执行单张样本推理；置信度不是在带真值测试集上测得的准确率，表情类别也不能证明人的真实心理状态。')
        shown_title = revision.get('taskTitle', t['title'])
        reference = '# 修订教学参考：'+shown_title+'\n\n'+revision.get('notice', '本例用于学习，不是四川指定批次的官方标准答案。')+'\n\n## 操作步骤\n\n'+explanation+'\n\n```python\n'+source+'```\n\n## 交付与复核\n\n'+'\n'.join('- '+c for c in revision.get('checklist', checks))
        if code=='2.2.4': reference += '\n\n目标为四项问卷均值，须说明代理指标的定义与局限，不能宣称已证明真实低碳行为或因果关系。'
    # 1.1 runnable references are retained; writing answers are examples, not executed evidence.
    executable = bool(re.search(r'```python', reference))
    tasks[code] = dict(sourceReferenceSha256=original_hash,
                      examEligible=revision.get('examEligible', True),
                      notice=revision.get('notice', '教学练习；示例与实际考试的工具、题型和评分细则可能不同。'),
                      firstStep=revision.get('firstStep', entry),
                      checklist=revision.get('checklist', checks),
                      prerequisiteLessons=lessons,
                      prerequisites=[{'id':lid,'title':lesson_titles[lid]} for lid in lessons],
                      runSteps=['把本题所有素材下载到同一文件夹，带子目录的文件保持目录结构。',
                                '使用一键安装环境的 Python/Jupyter，先完成 Python 基础与对应衔接课。',
                                '将参考代码保存为 reference.py 并放在素材旁；在此目录运行 python reference.py（依赖清单 requirements-labs.txt 位于项目根目录）。',
                                '保存真实输出、图表和报错处理记录，再关闭参考答案独立重做。'] if executable else
                               ['下载并阅读题面，列出尚不明确的假设。', '按下方验收清单撰写自己的方案；参考文案只是示例。', '用真实素材核算数字，提交流程、证据与可执行验收标准。'],
                      troubleshooting=['找不到文件：核对文件名、扩展名及当前工作目录。',
                                       '缺少库：使用项目学习环境，先在项目根目录执行 python -m pip install -r requirements-labs.txt。',
                                       '列名不匹配：查看 df.columns；不要为使程序通过而随意更改目标字段。'] if executable else
                                      ['只有口号没有步骤：补充输入、动作、产物、检查方法。', '数字没有来源：写出计算公式与数据范围，不抄参考数值。'],
                      validationScope='bundled-code' if executable else 'materials-only', reference=reference)
    for key in ('taskTitle','taskDesc','taskSkills','taskQuality','assessmentRevision'):
        if key in revision: tasks[code][key] = revision[key]

(ROOT/'data/practical-guidance.json').write_text(json.dumps({'version':'2026-11-11','tasks':tasks},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Generated guidance for',len(tasks),'tasks; standalone revised scripts:',len(codes))
print('Quarantined:', [c for c,v in tasks.items() if not v['examEligible']])
