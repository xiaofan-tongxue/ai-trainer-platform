"""Audit the exam-facing content map; this checks coverage evidence, not exam eligibility."""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
curriculum = json.loads((ROOT / "data/curriculum.json").read_text(encoding="utf-8"))
matrix = json.loads((ROOT / "data/competency_matrix.json").read_text(encoding="utf-8"))
seed = json.loads((ROOT / "data/seed_data.json").read_text(encoding="utf-8"))
errors = []
questions = seed["questions"]
practical = seed["practical"]
valid = {d["id"] for d in matrix["domains"]}
counts = {d: 0 for d in valid}
for q in questions:
    if q.get("knowledge") not in valid:
        errors.append("question without competency tag")
    else:
        counts[q["knowledge"]] += 1
for d in matrix["domains"]:
    if counts[d["id"]] != d["questionCount"]:
        errors.append(f"question count drift: {d['id']}")
    if d["theoryWeight"] + d["practicalWeight"] == 0:
        errors.append(f"zero-weight domain: {d['id']}")
    if not d["evidence"] or not d["knowledge"]:
        errors.append(f"missing learning evidence: {d['id']}")
for stage in curriculum["stages"]:
    if not stage.get("description") or not stage.get("target"):
        errors.append(f"stage missing target: {stage['id']}")
codes = [str(t.get("code", "")) for t in practical]
for prefix in ("1.", "2.", "3.", "4."):
    if not any(c.startswith(prefix) for c in codes):
        errors.append(f"practical domain missing: {prefix}")
required_text = "理论和技能均按百分制计分，均须达到 60 分"
if required_text not in curriculum["stages"][0].get("lessons", [{}])[0].get("content", ""):
    # The current curriculum stores this in the top-level overview lesson list.
    flattened = json.dumps(curriculum, ensure_ascii=False)
    if "均须达到 60 分" not in flattened or "120 分钟" not in flattened:
        errors.append("pass rule or skill duration missing")
print(json.dumps({"questions": len(questions), "practical": len(practical), "domains": counts, "errors": errors}, ensure_ascii=False, indent=2))
sys.exit(bool(errors))
