import json
from pathlib import Path
import numpy as np
import pandas as pd
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from sklearn.model_selection import train_test_split

# Your age 是区间文字。编码只保留这个类别，不把端点或中点写成精确岁数。
df = pd.read_csv("健康咨询客户数据集.csv")
df.columns = df.columns.str.strip()
df = df.drop(columns=[c for c in ["Timestamp", "Your name"] if c in df.columns])
print("删除缺失前行数：", len(df))
df = df.dropna().drop_duplicates().copy()
print("删除缺失和重复后行数：", len(df))

age_order = ["15 to 18", "19 to 25", "26 to 30", "30 to 40", "40 and above"]
age_values = set(df["Your age"].astype(str))
unknown_age = sorted(age_values - set(age_order))
if unknown_age:
    raise ValueError("出现未列入顺序的年龄区间：" + ", ".join(unknown_age))
df["Your age"] = pd.Categorical(df["Your age"].astype(str), categories=age_order, ordered=True)
assert not pd.api.types.is_numeric_dtype(df["Your age"])

fitness_col = "How do you describe your current level of fitness ?"
fitness_order = ["Unfit", "Average", "Good", "Very good", "Perfect"]
unknown_fitness = sorted(set(df[fitness_col].astype(str)) - set(fitness_order))
if unknown_fitness:
    raise ValueError("出现未列入顺序的健身水平：" + ", ".join(unknown_fitness))
df[fitness_col] = pd.Categorical(df[fitness_col].astype(str), categories=fitness_order, ordered=True)

frequency = df["How often do you exercise?"].astype(str).value_counts()
plt.figure(figsize=(8, 6))
plt.pie(frequency.values, labels=frequency.index, autopct="%1.1f%%", startangle=90)
plt.title("Exercise frequency")
plt.savefig("2.1.5_exercise_frequency.png", dpi=100, bbox_inches="tight")
plt.close()

# 标注对象是年龄区间。按区间分层，避免某个区间只出现在训练或测试的一边。
y = df["Your age"].astype(str)
assert y.value_counts().min() >= 2
X = df.drop(columns=["Your age"])
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y)
output = X.copy()
output["target"] = y
output["age_interval"] = y
output["split"] = np.where(output.index.isin(X_train.index), "train", "test")
assert set(output["age_interval"]).issubset(set(age_order))
assert not output["target"].str.fullmatch(r"\d+").any()
output.to_csv("2.1.5_cleaned_data.csv", index=False, encoding="utf-8-sig")
summary = {
    "rows": int(len(output)),
    "age_intervals": y.value_counts().to_dict(),
    "train_rows": int((output["split"] == "train").sum()),
    "test_rows": int((output["split"] == "test").sum()),
    "age_stored_as": "interval-label",
}
Path("2.1.5_summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(summary, ensure_ascii=False, indent=2))
print("已生成 2.1.5_cleaned_data.csv；年龄区间保持类别，未转换成精确年龄。")
