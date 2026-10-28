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
 'carbon': '''df = clean_columns(pd.read_excel("大学生低碳生活行为的影响因素数据集.xlsx"))
target = "低碳行为积极性"
df[target] = pd.to_numeric(df[target], errors="coerce")
df = df.dropna(subset=[target])
print("目标字段不同取值数：", df[target].nunique())
if df[target].nunique() < 2:
    print("注意：配套目标列为常量，仅练习预处理；请核查标签来源后再做预测训练。")
X, y = df.drop(columns=[target]), df[target]
''',
 'health': '''df = clean_columns(pd.read_csv("健康咨询客户数据集.csv"))
# Your age 是区间类别，不能直接转整数，也不应把区间中点当成真实年龄。
target = "How healthy do you consider yourself?"
df[target] = pd.to_numeric(df[target], errors="coerce")
df = df.dropna(subset=[target])
X, y = df.drop(columns=[target]), df[target]
''',
 'age': '''df = clean_columns(pd.read_csv("fitness analysis.csv"))
# 原题标题写运动量，正文写年龄；这里只演示年龄区间分类，不声称预测精确年龄。
target = "Your age"
features = ["Your gender", "How important is exercise to you ?", "How healthy do you consider yourself?"]
df = df.dropna(subset=[target])
X, y = df[features], df[target].astype(str)
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

codes = {
 '2.1.1': preprocessing('auto','2.1.1'),
 '2.1.2': preprocessing('carbon','2.1.2'),
 '2.1.3': preprocessing('finance','2.1.3',True),
 '2.1.5': preprocessing('health','2.1.5'),
 '2.2.1': modeling('finance','2.2.1','LogisticRegression(max_iter=1500, class_weight="balanced")',True),
 '2.2.2': modeling('auto','2.2.2','RandomForestRegressor(n_estimators=100, max_depth=8, random_state=42, n_jobs=2)'),
 '2.2.3': modeling('age','2.2.3','RandomForestClassifier(n_estimators=100, max_depth=6, random_state=42, n_jobs=2)',True),
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

WARNINGS = {
 '2.1.2': '所附数据中“低碳行为积极性”318条记录全部为0，无法据此学习有区分度的预测目标。仅练习清洗与数据质量检查，待标签来源核查后再纳入技能模拟。',
 '2.1.5': '年龄字段为区间类别，与转成精确整数年龄的要求不一致。修订例保留区间并编码，不推造真实年龄；本题暂不纳入技能模拟。',
 '2.2.3': '标题写运动量，正文写年龄，素材年龄为区间。旧例把类别编号做回归不合适；修订例仅演示年龄区间分类。本题暂不纳入技能模拟，待题面与数据统一。',
 '3.2.4': '所附 flower-detection.onnx 与 3.2.1 的 resnet.onnx 内容完全相同，标签为 ImageNet 1000 类，不能证明为花卉专用模型。仅演示通用分类推理，暂不纳入技能模拟。'
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
    if code.startswith('3.2.'):
        # Last block is the complete script; remove platform-specific folder assumption.
        source = re.findall(r'```python\s*\n(.*?)```', reference, re.S)[-1]
        source = source.replace("'onnx/", "'").replace('"onnx/', '"')
        source = source.replace('Image.ANTIALIAS', 'Image.Resampling.LANCZOS')
        source = source.replace('准确率', '模型置信度')
        codes[code] = source
    if code in codes:
        source = codes[code].strip()+'\n'
        (solutions/(code+'.py')).write_text(source, encoding='utf-8')
        explanation = ('先阅读每段中文注释，再逐段运行。训练集用于学习参数；验证集用于比较方案；最终测试集仅在方案确定后评价。不要复制示例数字作为自己的运行结果。'
                       if code.startswith('2.') else '核对当前目录与素材文件名，再核对输入维度。这里仅执行单张样本推理；置信度不是在带真值测试集上测得的准确率，表情类别也不能证明人的真实心理状态。')
        reference = '# 修订教学参考：'+t['title']+'\n\n'+WARNINGS.get(code,'本例用于学习，不是四川指定批次的官方标准答案。')+'\n\n## 操作步骤\n\n'+explanation+'\n\n```python\n'+source+'```\n\n## 交付与复核\n\n'+'\n'.join('- '+c for c in checks)
        if code=='2.2.4': reference += '\n\n目标为四项问卷均值，须说明代理指标的定义与局限，不能宣称已证明真实低碳行为或因果关系。'
    # 1.1 runnable references are retained; writing answers are examples, not executed evidence.
    executable = bool(re.search(r'```python', reference))
    tasks[code] = dict(sourceReferenceSha256=original_hash, examEligible=code not in WARNINGS,
                      notice=WARNINGS.get(code, '教学练习；示例与实际考试的工具、题型和评分细则可能不同。'),
                      firstStep=entry, checklist=checks, prerequisiteLessons=lessons,
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

(ROOT/'data/practical-guidance.json').write_text(json.dumps({'version':'2026-10-26','tasks':tasks},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Generated guidance for',len(tasks),'tasks; standalone revised scripts:',len(codes))
