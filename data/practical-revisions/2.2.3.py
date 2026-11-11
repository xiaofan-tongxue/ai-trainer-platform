import json
from pathlib import Path
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
from sklearn.impute import SimpleImputer
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, f1_score, confusion_matrix
from sklearn.base import clone

# 数据先划分。填补值、标准化参数和类别编码只从训练集学习。
def prepare(X):
    numeric = X.select_dtypes(include="number").columns.tolist()
    categorical = [c for c in X.columns if c not in numeric]
    return ColumnTransformer([
        ("number", Pipeline([("fill", SimpleImputer(strategy="median")),
                             ("scale", StandardScaler())]), numeric),
        ("category", Pipeline([("fill", SimpleImputer(strategy="most_frequent")),
                               ("encode", OneHotEncoder(handle_unknown="ignore"))]), categorical)
    ])

df = pd.read_csv("fitness analysis.csv")
df.columns = df.columns.str.strip()
df = df.drop(columns=[c for c in ["Timestamp", "Your name"] if c in df.columns]).drop_duplicates()

# 素材没有“日常运动量”字段。Your age 是区间，分类目标保留原文，不做回归。
target = "Your age"
age_order = ["15 to 18", "19 to 25", "26 to 30", "30 to 40", "40 and above"]
features = ["Your gender", "How important is exercise to you ?", "How healthy do you consider yourself?"]
missing = [c for c in [target, *features] if c not in df.columns]
if missing:
    raise ValueError("列名与素材不一致：" + ", ".join(missing))
df = df.dropna(subset=[target, *features]).copy()
unknown_age = sorted(set(df[target].astype(str)) - set(age_order))
if unknown_age:
    raise ValueError("出现未列入顺序的年龄区间：" + ", ".join(unknown_age))
y = df[target].astype(str)
assert not pd.api.types.is_numeric_dtype(y)
X = df[features]
assert y.value_counts().min() >= 2

X_dev, X_test, y_dev, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y)
X_train, X_valid, y_train, y_valid = train_test_split(
    X_dev, y_dev, test_size=0.25, random_state=42, stratify=y_dev)
assert set(X_train.index).isdisjoint(X_valid.index)
assert set(X_dev.index).isdisjoint(X_test.index)

candidates = {
    "random_forest": RandomForestClassifier(n_estimators=100, max_depth=6, random_state=42, n_jobs=2),
    "logistic": LogisticRegression(max_iter=1500),
}
validation = {}
for name, estimator in candidates.items():
    model = Pipeline([("prepare", prepare(X_train)), ("model", estimator)])
    model.fit(X_train, y_train)
    pred = model.predict(X_valid)
    validation[name] = float(f1_score(y_valid, pred, average="macro", zero_division=0))
chosen = max(validation, key=validation.get)
print("验证集宏平均 F1：", {k: round(v, 4) for k, v in validation.items()})
print("按验证集选择：", chosen)

final_model = Pipeline([("prepare", prepare(X_dev)), ("model", clone(candidates[chosen]))])
final_model.fit(X_dev, y_dev)
prediction = final_model.predict(X_test)
labels = final_model.classes_.tolist()
metrics = {
    "task": "age-interval-classification",
    "chosen_model": chosen,
    "validation_macro_f1": validation,
    "accuracy": float(accuracy_score(y_test, prediction)),
    "macro_f1": float(f1_score(y_test, prediction, average="macro", zero_division=0)),
    "labels": labels,
    "label_counts": y.value_counts().to_dict(),
    "confusion_matrix": confusion_matrix(y_test, prediction, labels=labels).tolist(),
    "train_rows": int(len(X_train)),
    "validation_rows": int(len(X_valid)),
    "test_rows": int(len(X_test)),
    "random_state": 42,
}
assert "R2" not in metrics and "MAE" not in metrics
assert all(np.isfinite(v) for v in metrics["validation_macro_f1"].values())
assert np.isfinite(metrics["accuracy"]) and np.isfinite(metrics["macro_f1"])
Path("2.2.3_metrics.json").write_text(json.dumps(metrics, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(metrics, ensure_ascii=False, indent=2))
print("已生成 2.2.3_metrics.json；年龄区间是分类目标。准确率接近最大区间占比时，以宏平均 F1 和混淆矩阵为准。")
