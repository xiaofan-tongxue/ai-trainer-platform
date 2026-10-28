"""Audit the exam-facing content map; this checks coverage evidence, not exam eligibility."""
import json
import pathlib
import re
import sys
from importlib.util import spec_from_file_location, module_from_spec

ROOT = pathlib.Path(__file__).resolve().parents[1]
curriculum = json.loads((ROOT / "data/curriculum.json").read_text(encoding="utf-8"))
matrix = json.loads((ROOT / "data/competency_matrix.json").read_text(encoding="utf-8"))
seed = json.loads((ROOT / "data/seed_data.json").read_text(encoding="utf-8"))
errors = []
warnings = []
if curriculum["hours"] != sum(s["hours"] for s in curriculum["stages"]):
    errors.append("total training hours disagree with stage hours")
bridge = json.loads((ROOT / "data/bridge-course.json").read_text(encoding="utf-8"))
lessons = curriculum["lessons"] + bridge["lessons"]
by_id = {l["id"]: l for l in lessons}
if len(by_id) != len(lessons):
    errors.append("duplicate lesson id")
for lesson in bridge["lessons"]:
    if lesson["after"] not in {l["id"] for l in curriculum["lessons"]}:
        errors.append("invalid insertion point: " + lesson["id"])
    if not lesson.get("prerequisite") or len(lesson.get("evidenceChecks", [])) < 1:
        errors.append("missing prerequisites or deliverable checks: " + lesson["id"])
for lesson in lessons:
    for q in lesson.get("quiz", []):
        if q["answer"] not in {o["label"] for o in q["options"]} or not q.get("explanation"):
            errors.append("invalid quiz: " + lesson["id"])
for key in ("theoryWeight", "practicalWeight"):
    if sum(d[key] for d in matrix["domains"]) != 100:
        errors.append("invalid weight sum: " + key)
required = {"1.1", "1.2", "2.1", "2.2", "3.1", "3.2", "4.1", "4.2"}
if {c["section"] for c in matrix.get("capabilities", [])} != required:
    errors.append("missing national level-three work-content mapping")
for capability in matrix.get("capabilities", []):
    if not capability.get("evidence") or not capability.get("lessons"):
        errors.append("missing capability evidence: " + capability["section"])
    for lid in capability.get("lessons", []):
        if lid not in by_id:
            errors.append("unknown mapped lesson: " + lid)
questions = seed["questions"]
missing_analysis = sum(not str(q.get("analysis") or "").strip() for q in questions)
spec = spec_from_file_location('question_build', ROOT/'tools/build-question-explanations.py')
helper = module_from_spec(spec); spec.loader.exec_module(helper)
overlay = json.loads((ROOT/'data/question-explanations.json').read_text(encoding='utf-8'))['questions']
guidance = json.loads((ROOT/'data/practical-guidance.json').read_text(encoding='utf-8'))['tasks']
supplemented = sum(not str(q.get('analysis') or '').strip() and bool(overlay.get(helper.fingerprint(q),{}).get('analysis')) for q in questions)
quarantined = sum(not x['examEligible'] for x in overlay.values())
if missing_analysis > supplemented: errors.append('missing explanations after overlay')
if len(overlay)!=supplemented: errors.append('explanation no longer matches current question')
warnings.append(f'{supplemented} AI-assisted explanations; {quarantined} unresolved items excluded from assessment; remaining keys are not certified')
eligible_counts = {d['id']:sum(q['knowledge']==d['id'] and overlay.get(helper.fingerprint(q),{}).get('examEligible',True) for q in questions) for d in matrix['domains']}
for domain in matrix['domains']:
    if eligible_counts[domain['id']] < domain['theoryWeight']: errors.append('insufficient eligible theory pool: '+domain['id'])
practical = seed["practical"]
for task in practical:
    guide=guidance.get(task['code'],{})
    if not guide.get('checklist') or not guide.get('runSteps'): errors.append('missing practical steps: '+task['code'])
    if helper.hashlib.sha256(task['reference'].replace('\r\n','\n').encode()).hexdigest()!=guide.get('sourceReferenceSha256'): errors.append('stale practical overlay: '+task['code'])
    for lesson in guide.get('prerequisiteLessons',[]):
        if lesson not in by_id: errors.append('unknown practical prerequisite: '+lesson)
warnings.append(f"{sum(not g['examEligible'] for g in guidance.values())} practical tasks excluded for inconsistent materials; writing tasks still require human assessment")
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
    prefix = {"business": "1.", "training": "2.", "design": "3.", "coaching": "4."}.get(d["id"])
    actual = sum(str(t.get("code", "")).startswith(prefix) for t in practical) if prefix else 0
    if actual != d["practicalTaskCount"]:
        errors.append(f"practical count drift: {d['id']}")
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
print(json.dumps({"questions": len(questions), "practical": len(practical), "overviewLessons": len(curriculum["lessons"]), "bridgeLessons": len(bridge["lessons"]), "sourceMissingExplanations": missing_analysis, "supplementedExplanations":supplemented,"missingExplanations": missing_analysis-supplemented, "mappedWorkContents": len(matrix.get("capabilities", [])), "domains": counts,"eligibleDomains":eligible_counts, "warnings": warnings, "errors": errors}, ensure_ascii=False, indent=2))
sys.exit(bool(errors))
