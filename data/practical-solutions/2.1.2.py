import json
from pathlib import Path
import pandas as pd

# 本题只检查标签质量。目标列如果没有区分度，就停止，不要编造标签，也不要接着训练。
df = pd.read_excel("大学生低碳生活行为的影响因素数据集.xlsx")
target = "低碳行为积极性"
values = pd.to_numeric(df[target], errors="coerce")
counts = {str(k): int(v) for k, v in values.value_counts(dropna=False).items()}
allowance = [c for c in df.columns if "月生活费" in str(c)]
report = {
    "rows": int(len(df)),
    "columns": int(df.shape[1]),
    "missing_cells": int(df.isna().sum().sum()),
    "duplicate_rows": int(df.duplicated().sum()),
    "target": target,
    "target_nunique": int(values.nunique(dropna=True)),
    "target_counts": counts,
    "trainable": bool(values.nunique(dropna=True) >= 2),
    "allowance_column_is_option_code": bool(allowance),
}
assert report["rows"] == 318, "行数与所附数据不一致，请先核对是不是原文件"
assert report["trainable"] is False, "只有目标列出现多个取值时，才可以讨论预测模型"
report["decision"] = "目标列只有常量 0，不能训练有区分度的预测模型。不要编造、插补或替换标签。"
Path("2.1.2_quality.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
print("配套目标列为常量")
print(json.dumps(report, ensure_ascii=False, indent=2))
print("月生活费列名是金额区间，表内已是选项代号，不是连续金额。本题到质量结论为止。")
