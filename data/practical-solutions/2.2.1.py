import json
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

from sklearn.linear_model import LogisticRegression, LinearRegression
from sklearn.ensemble import RandomForestRegressor, RandomForestClassifier
from sklearn.tree import DecisionTreeRegressor
from sklearn.metrics import (accuracy_score, f1_score, confusion_matrix, roc_auc_score,
                             mean_absolute_error, mean_squared_error, r2_score)
from sklearn.base import clone

df = clean_columns(pd.read_csv("finance数据集.csv"))
target = "SeriousDlqin2yrs"
df[target] = pd.to_numeric(df[target], errors="coerce")
df = df[df[target].isin([0, 1])].copy()
X, y = df.drop(columns=[target]), df[target].astype(int)

# 先留出最终测试集，再从剩余数据中分出验证集。验证集可用于调参。
X_dev, X_test, y_dev, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y)
X_train, X_valid, y_train, y_valid = train_test_split(
    X_dev, y_dev, test_size=0.25, random_state=42, stratify=y_dev)
assert set(X_train.index).isdisjoint(X_valid.index)
assert set(X_dev.index).isdisjoint(X_test.index)
model = Pipeline([("prepare", prepare(X_train)), ("model", LogisticRegression(max_iter=1500, class_weight="balanced"))])
model.fit(X_train, y_train)
print("验证集评分：", round(model.score(X_valid, y_valid), 4))
# 本例固定超参数。练习改进只能看验证结果，确定方案后才打开最终测试集。
final_model = clone(model).fit(X_dev, y_dev)
prediction = final_model.predict(X_test)

metrics = {"accuracy": float(accuracy_score(y_test, prediction)),
           "macro_f1": float(f1_score(y_test, prediction, average="macro", zero_division=0)),
           "labels": final_model.classes_.tolist(),
           "confusion_matrix": confusion_matrix(y_test, prediction, labels=final_model.classes_).tolist()}
if len(final_model.classes_) == 2:
    metrics["roc_auc"] = float(roc_auc_score(y_test, final_model.predict_proba(X_test)[:, 1]))

metrics.update(train_rows=len(X_train), validation_rows=len(X_valid), test_rows=len(X_test), random_state=42)
Path("2.2.1_metrics.json").write_text(json.dumps(metrics, ensure_ascii=False, indent=2), encoding="utf-8")
assert all(np.isfinite(v) for v in metrics.values() if isinstance(v, float))
print(json.dumps(metrics, ensure_ascii=False, indent=2))
print("已生成 2.2.1_metrics.json；一次固定划分只提供本数据集的测试证据。")
