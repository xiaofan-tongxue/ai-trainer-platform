"""Run bundled reference examples in disposable folders; never execute student submissions.

This is a developer validation command, not a sandbox for untrusted uploads.
Original datasets/models are copied, not modified. No model or dataset downloads occur.
"""
import argparse
import ast
from concurrent.futures import ThreadPoolExecutor
import hashlib
import importlib.metadata
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import time

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--codes', nargs='*')
args = parser.parse_args()
seed = json.loads((ROOT/'data/seed_data.json').read_text(encoding='utf-8'))
out = ROOT/'build/practical-audit'
out.mkdir(parents=True, exist_ok=True)
patches_path = ROOT/'data/practical-guidance.json'
patches = json.loads(patches_path.read_text(encoding='utf-8')).get('tasks', {}) if patches_path.exists() else {}

def verify(task):
    code_id = task['code']
    reference = patches.get(code_id, {}).get('reference', task.get('reference', ''))
    blocks = re.findall(r'```python\s*\n(.*?)```', reference, re.S)
    result = {'code': code_id, 'title': task['title'], 'codeBlocks': len(blocks)}
    missing = []
    for m in task.get('materials', []):
        src = ROOT/'materials'/code_id/m['name']
        if not src.is_file(): missing.append(m['name'])
        elif src.stat().st_size < 200 and src.read_bytes().startswith(b'version https://git-lfs.github.com/spec/v1'):
            missing.append(m['name']+' (LFS pointer only)')
    result['missingMaterials'] = missing
    if not blocks:
        result['status'] = 'writing-task-materials-checked' if not missing else 'missing-materials'
        return result
    # ONNX references contain fill-in fragments followed by a complete standalone program.
    code = blocks[-1] if code_id.startswith('3.2.') else '\n'.join(blocks)
    result['codeSha256'] = hashlib.sha256(code.encode()).hexdigest()
    tree = ast.parse(code)
    forbidden = {'subprocess', 'socket', 'requests', 'urllib', 'http', 'ctypes'}
    for node in ast.walk(tree):
        modules = [a.name for a in node.names] if isinstance(node, ast.Import) else [node.module or ''] if isinstance(node, ast.ImportFrom) else []
        if any(m.split('.')[0] in forbidden for m in modules):
            raise ValueError('Unexpected network/process module in bundled reference: '+code_id)
    started = time.monotonic()
    with tempfile.TemporaryDirectory(prefix=code_id+'-', dir=out) as folder:
        folder = Path(folder)
        shutil.copytree(ROOT/'materials'/code_id, folder, dirs_exist_ok=True,
                        ignore=shutil.ignore_patterns('__pycache__', '*.pyc'))
        (folder/'reference.py').write_text(code, encoding='utf-8')
        env = {**os.environ, 'PYTHONIOENCODING':'utf-8', 'MPLBACKEND':'Agg',
               'OMP_NUM_THREADS':'2', 'OPENBLAS_NUM_THREADS':'2', 'MKL_NUM_THREADS':'2'}
        try:
            p = subprocess.run([sys.executable,'-X','utf8','reference.py'], cwd=folder, env=env,
                               text=True, encoding='utf-8', capture_output=True, timeout=180)
            result['status'] = 'execution-passed' if p.returncode == 0 else 'execution-failed'
            result['exitCode'] = p.returncode
            if p.returncode == 0 and code_id.startswith('2.'):
                if code_id.startswith('2.1.'):
                    import pandas as pd
                    output = folder/(code_id+'_cleaned_data.csv')
                    frame = pd.read_csv(output)
                    assert len(frame)>0, code_id+' empty output'
                    if code_id!='2.1.4':
                        assert set(frame['split'])=={'train','test'}, code_id+' missing split'
                        assert frame['target'].notna().all(), code_id+' missing labels'
                        if frame['target'].nunique()<2:
                            assert patches[code_id]['examEligible'] is False, 'constant label must be quarantined'
                    result['outputRows']=len(frame)
                else:
                    metrics=json.loads((folder/(code_id+'_metrics.json')).read_text(encoding='utf-8'))
                    assert all(metrics[k]>0 for k in ['train_rows','validation_rows','test_rows'])
                    result['metrics']=metrics
            (out/(code_id+'.log')).write_text(p.stdout+'\n'+p.stderr, encoding='utf-8')
            result['error'] = '\n'.join(p.stderr.splitlines()[-4:]) if p.returncode else None
        except subprocess.TimeoutExpired:
            result['status'] = 'timeout'; result['error'] = 'Exceeded 180 second validation budget'
    result['seconds'] = round(time.monotonic()-started, 2)
    print(code_id, result['status'], flush=True)
    return result

tasks = [t for t in seed['practical'] if not args.codes or t['code'] in args.codes]
with ThreadPoolExecutor(max_workers=2) as pool:
    results = list(pool.map(verify, tasks))
versions = {}
for name in ['numpy','pandas','scikit-learn','matplotlib','onnxruntime','Pillow','opencv-python','openpyxl','xgboost']:
    try: versions[name] = importlib.metadata.version(name)
    except importlib.metadata.PackageNotFoundError: versions[name] = 'not installed'
report = {'python':sys.version.split()[0], 'versions':versions, 'scope':'Bundled examples only; writing tasks are not software executions; no learner data', 'tasks':results}
(out/'results.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'tasks':len(results),'passed':sum(r['status']=='execution-passed' for r in results),
                  'writing':sum(r['status']=='writing-task-materials-checked' for r in results),
                  'failures':[{'code':r['code'],'error':r.get('error')} for r in results if r['status'] not in ['execution-passed','writing-task-materials-checked']]},ensure_ascii=False,indent=2))
sys.exit(any(r['status'] not in ['execution-passed','writing-task-materials-checked'] for r in results))
