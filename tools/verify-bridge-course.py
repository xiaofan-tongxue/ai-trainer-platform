"""Execute the teaching examples and corrected legacy reference, without learner data."""
import json
import ast
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]
course = json.loads((ROOT / 'data/bridge-course.json').read_text(encoding='utf-8'))
failures = []
executed = 0

def execute(code, folder, expected=None):
    global executed
    executed += 1
    result = subprocess.run([sys.executable, '-X', 'utf8', '-c', code], cwd=folder,
                            capture_output=True, text=True, encoding='utf-8', timeout=45,
                            env={**os.environ, 'PYTHONIOENCODING': 'utf-8'})
    if result.returncode or (expected is not None and result.stdout.strip() != expected):
        failures.append({'case': executed, 'stderr': result.stderr, 'stdout': result.stdout, 'expected': expected})

for lesson in course['lessons']:
    if 'example' not in lesson:
        continue
    ex = lesson['example']
    snippets = re.findall(r'```python\n(.*?)```', lesson['content'], flags=re.S)
    assert any(ast.dump(ast.parse(s)) == ast.dump(ast.parse(ex['code'])) for s in snippets), lesson['id'] + ': tested code differs from teaching text'
    with tempfile.TemporaryDirectory(prefix='bridge-example-') as folder:
        execute(ex['code'], folder, ex['expected'])

seed = json.loads((ROOT / 'data/seed_data.json').read_text(encoding='utf-8'))
reference = next(t['reference'] for t in seed['practical'] if t['code'] == '1.1.1')
code = '\n'.join(re.findall(r'```python\n(.*?)```', reference, flags=re.S))
with tempfile.TemporaryDirectory(prefix='reference-example-') as folder:
    shutil.copyfile(ROOT / 'materials/1.1.1/patient_data.csv', Path(folder) / 'patient_data.csv')
    execute(code + "\nassert data['BMIRange'].notna().all()\nassert data['AgeRange'].notna().all()\nassert int(bmi_patient_count.sum()) == len(data)\n", folder)

print(json.dumps({'bridgeLessons': len(course['lessons']), 'executed': executed, 'failures': failures}, ensure_ascii=False, indent=2))
sys.exit(bool(failures))
