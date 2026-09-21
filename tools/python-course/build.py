import csv
import hashlib
import io
import json
import pathlib
import urllib.request
import zipfile
import argparse
from content import LESSONS, UNITS

ROOT = pathlib.Path(__file__).resolve().parents[2]
DATA = ROOT / 'data' / 'python-course'
DATA.mkdir(parents=True, exist_ok=True)
SOURCES = [
 ('Python 入门入口', 'https://www.python.org/about/gettingstarted/', '零基础', '先认识解释器与安装入口，不必一次读完所有外链。'),
 ('Python 中文教程', 'https://docs.python.org/zh-cn/3.13/tutorial/', '学过后查阅', '官方教程假定已有编程基础；完成本课对应主题后再查用法。'),
 ('CSV 官方说明', 'https://docs.python.org/zh-cn/3.13/library/csv.html', '第39课后', '查 DictReader、编码与换行参数；不要用 split 代替完整 CSV 解析。'),
 ('NumPy 初学者指南', 'https://numpy.org/doc/stable/user/absolute_beginners.html', '第41课后', '重点阅读数组、维度和形状，先在小数组上练习。'),
 ('pandas 入门教程', 'https://pandas.pydata.org/docs/getting_started/intro_tutorials/index.html', '第42课后', '按读取、筛选、统计顺序阅读，不用背所有 API。'),
 ('scikit-learn 教学数据集', 'https://scikit-learn.org/stable/datasets/toy_dataset.html', '完成基础后', '进入模型实验时认识特征与目标，区分教学数据和真实业务数据。'),
 ('UCI Iris 数据集', 'https://archive.ics.uci.edu/dataset/53/iris', '公开数据实练', '150条植物记录，四个测量特征；学习读取、分组和分类。'),
 ('UCI Wine 数据集', 'https://archive.ics.uci.edu/dataset/109/wine', '进阶数据实练', '178条记录、13个数值特征；练习类型、尺度和分类数据检查。')
]

def obtain(refresh=False):
    specs = [
      ('iris.csv','https://archive.ics.uci.edu/static/public/53/iris.zip','iris.data',
       ['sepal_length','sepal_width','petal_length','petal_width','species'],150,
       'Fisher, R. (1936). Iris. https://doi.org/10.24432/C56C76', SOURCES[6][1]),
      ('wine.csv','https://archive.ics.uci.edu/static/public/109/wine.zip','wine.data',
       ['class','alcohol','malic_acid','ash','alcalinity_of_ash','magnesium','total_phenols','flavanoids','nonflavanoid_phenols','proanthocyanins','color_intensity','hue','od280_od315','proline'],178,
       'Aeberhard, S. & Forina, M. (1992). Wine. https://doi.org/10.24432/C5PC7J', SOURCES[7][1])
    ]
    manifest=[]
    for filename,url,member,columns,count,citation,page in specs:
        path=DATA / filename
        if refresh or not path.exists():
            with urllib.request.urlopen(url,timeout=60) as r: raw=r.read(2_000_000)
            with zipfile.ZipFile(io.BytesIO(raw)) as z:
                rows=[row for row in csv.reader(io.StringIO(z.read(member).decode('utf-8'))) if row]
            assert len(rows)==count and all(len(r)==len(columns) for r in rows)
            with path.open('w',encoding='utf-8',newline='') as f:
                writer=csv.writer(f,lineterminator='\n');writer.writerow(columns);writer.writerows(rows)
        with path.open(encoding='utf-8',newline='') as f:
            rows=list(csv.reader(f))
        assert rows[0]==columns and len(rows)-1==count
        manifest.append(dict(file=filename,rows=count,columns=columns,source=page,download=url,citation=citation,
            license='CC BY 4.0',licenseUrl='https://creativecommons.org/licenses/by/4.0/',
            changes='添加英文字段名，统一 UTF-8 与 LF 换行；原始数据值与行顺序不变。'+('Iris 使用归档中的 iris.data，未套用其他版本修正。' if filename=='iris.csv' else ''),
            checkedAt='2026-09-21',sha256=hashlib.sha256(path.read_bytes()).hexdigest()))
    (DATA/'sources.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    return manifest

def build(refresh=False):
    manifest=obtain(refresh)
    fixtures={
      'scores.csv':'id,score,group\nS01,50,A\nS02,60,A\nS03,80,B\n',
      'labels_dirty.csv':'id,label,score\nA01, Cat ,80\nA02,DOG,\nA01,cat,80\nA03, ,60\nA04,bird,101\nA05,dog,70\n',
      'predictions.csv':'id,truth,prediction\nP01,cat,cat\nP02,dog,cat\nP03,cat,cat\nP04,dog,dog\n'
    }
    for name,text in fixtures.items(): (DATA/name).write_text(text,encoding='utf-8')
    datasets=[
      dict(file='scores.csv',title='第一张小表：3条分数',rows=3,level='入门',source='平台原创模拟数据，不含真实学员信息',license='平台原创教学素材',
           fields='id：虚构编号；score：练习分数；group：分组。',
           tasks=['先手算总分与每组均值。','用 CSV 标准库读取，再用 pandas 重做。'],answer='总分190；A组均值55，B组均值80。',lesson='py-39'),
      dict(file='labels_dirty.csv',title='带问题的标注表：6条',rows=6,level='清洗',source='平台原创模拟数据，故意设置质量问题',license='平台原创教学素材',
           fields='id：记录编号；label：原始标签；score：0—100的质量分；空格和空单元格故意保留。',
           tasks=['原始表保留；标签去两端空格并转小写；按id保留首次出现。','只保留标签非空、分数可转数值且在0—100之间的记录，报告各原因数量。'],answer='先去重移除1条；余下缺失分数1条、空标签1条、超范围分数1条，最终保留A01、A05共2条。',lesson='py-46'),
      dict(file='predictions.csv',title='预测结果对照：4条',rows=4,level='指标',source='平台原创模拟数据，不代表真实模型性能',license='平台原创教学素材',
           fields='id：样本编号；truth：真实标签；prediction：预测标签。',
           tasks=['逐条对照标签，找出错误编号。','计算正确条数与准确率，说明准确率不是考试通过率。'],answer='P02错误；3/4正确，准确率0.75。',lesson='py-47')
    ]
    for m in manifest:
        iris=m['file']=='iris.csv'
        datasets.append(dict(**m,title='Iris 鸢尾花' if iris else 'Wine 数值分类数据',level='公开数据',fields='、'.join(m['columns']),
           tasks=['读取文件，核对行数、列名、缺失与类别数量。','按类别统计数量；不要把目标列作为输入特征。'],
           answer='150条、4个特征、3类各50条。' if iris else '178条、13个特征、3类数量分别为59、71、48。',lesson='py-48'))
    for d in datasets:d['text']=(DATA/d['file']).read_text(encoding='utf-8')
    for lesson in LESSONS:
        lesson['sourceIds']=[0,1] if lesson['unit']<9 else ([1,2] if lesson['unit']==9 else [3,4,5])
        lesson['packages']=['numpy'] if lesson['id']=='py-41' else ['pandas'] if lesson['id'] in ('py-42','py-43','py-44') else []
    course=dict(version=1,updatedAt='2026-09-21',units=[dict(id=i+1,title=t,goal=g) for i,(t,g) in enumerate(UNITS)],
                lessons=LESSONS,sources=[dict(title=t,url=u,level=l,note=n) for t,u,l,n in SOURCES],datasets=datasets,
                fixtures={**fixtures,**{d['file']:d['text'] for d in datasets}},
                glossary=[['解释器','读懂并执行 Python 指令的程序。'],['表达式','能计算出一个值的一段代码，例如 2+3。'],['变量','用名字保存或引用一个值。'],['类型','值的类别，决定可以进行哪些操作。'],['索引','元素的位置编号，通常从0开始。'],['缩进','行首空格，用来表示代码属于哪个块。'],['参数','函数接收的数据。'],['返回值','函数交回调用处的结果。'],['异常','运行中遇到问题时给出的错误信息。'],['样本','数据集中的一条观察或记录。'],['特征','用于描述样本、作为模型输入的信息。'],['标签','希望模型预测的目标或人工标注结果。']])
    (ROOT/'webapp/js/python-course-data.js').write_text('/* Generated by tools/python-course/build.py; original lessons and attributed datasets. */\nwindow.PYTHON_COURSE = '+json.dumps(course,ensure_ascii=False,indent=2)+';\n',encoding='utf-8')
    (DATA/'course.json').write_text(json.dumps(course,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('Built 48 lessons, 96 coding tasks, 48 comprehension checks and 5 datasets.')

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--refresh-datasets',action='store_true')
    build(p.parse_args().refresh_datasets)
