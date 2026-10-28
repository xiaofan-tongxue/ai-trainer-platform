import json
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
