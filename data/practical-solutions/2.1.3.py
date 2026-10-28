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

df = clean_columns(pd.read_csv("finance数据集.csv"))
target = "SeriousDlqin2yrs"
df[target] = pd.to_numeric(df[target], errors="coerce")
df = df[df[target].isin([0, 1])].copy()
X, y = df.drop(columns=[target]), df[target].astype(int)

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y)
processor = prepare(X_train)
train_features = processor.fit_transform(X_train)
test_features = processor.transform(X_test)
assert len(X_train) and len(X_test) and set(X_train.index).isdisjoint(X_test.index)
# 导出原始单位的数据与划分标记，便于复核；不能把目标和标准化特征混写。
output = X.copy()
output["target"] = y
output["split"] = np.where(output.index.isin(X_train.index), "train", "test")
output.to_csv("2.1.3_cleaned_data.csv", index=False, encoding="utf-8-sig")
print("划分后特征形状：", train_features.shape, test_features.shape)
print("已生成 2.1.3_cleaned_data.csv；target 保留原单位，split 记录划分。")
