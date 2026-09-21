"""Execute every teaching example and reference solution in an isolated temp folder."""
import json
import os
import pathlib
import subprocess
import sys
import tempfile
import hashlib
import csv

ROOT=pathlib.Path(__file__).resolve().parents[2]
course=json.loads((ROOT/'data/python-course/course.json').read_text(encoding='utf-8'))
def norm(value):return value.replace('\r\n','\n').rstrip('\n')
checks=0
failures=[]
for index,lesson in enumerate(course['lessons']):
    assert lesson['id']==f'py-{index+1:02d}'
    assert lesson['prerequisite']==(f'py-{index:02d}' if index else None)
    assert len(lesson['concepts'])>=3 and len(lesson['lines'])>=2
    assert 0<=lesson['quiz']['answer']<len(lesson['quiz']['choices'])
    cases=[('example',dict(solution=lesson['example'],inputs=lesson.get('exampleInputs',''),expected=lesson['expected']))]
    cases.extend((kind,lesson[kind]) for kind in ('guided','independent'))
    for kind,task in cases:
        checks+=1
        with tempfile.TemporaryDirectory(prefix='python-course-') as folder:
            folder=pathlib.Path(folder)
            for name,text in course['fixtures'].items():(folder/name).write_text(text,encoding='utf-8')
            (folder/'exercise.py').write_text(task['solution'],encoding='utf-8')
            result=subprocess.run([sys.executable,'-X','utf8',str(folder/'exercise.py')],cwd=folder,input=task.get('inputs','')+'\n',
                encoding='utf-8',capture_output=True,timeout=20,env={**os.environ,'PYTHONIOENCODING':'utf-8'})
            if result.returncode or norm(result.stdout)!=norm(task['expected']):
                failures.append(dict(lesson=lesson['id'],kind=kind,error=result.stderr,expected=task['expected'],actual=result.stdout))
            for name,expected in task.get('files',{}).items():
                path=folder/name
                if not path.exists() or norm(path.read_text(encoding='utf-8'))!=norm(expected):failures.append(dict(lesson=lesson['id'],kind=kind,file=name,error='file content mismatch'))
for dataset in course['datasets']:
    rows=list(csv.reader(dataset['text'].splitlines()))
    assert len(rows)-1==dataset['rows'], dataset['file']
    assert all(len(row)==len(rows[0]) for row in rows),dataset['file']
    if 'sha256' in dataset:assert hashlib.sha256((ROOT/'data/python-course'/dataset['file']).read_bytes()).hexdigest()==dataset['sha256']
    assert dataset['text']==course['fixtures'][dataset['file']]
print(json.dumps(dict(executed=checks,lessons=len(course['lessons']),datasets=len(course['datasets']),failures=failures),ensure_ascii=False,indent=2))
sys.exit(bool(failures))
