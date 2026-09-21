# 练习数据与来源

`course.json`为生成的课程、练习、资源和数据说明；浏览器对应文件为 `webapp/js/python-course-data.js`。修改课程请编辑 `tools/python-course/content.py`，再运行生成器，避免两份内容不一致。

## 模拟数据

`scores.csv`（3行）、`labels_dirty.csv`（6行）、`predictions.csv`（4行）由项目生成，人物编号及记录均为虚构。分别用于读取筛选、缺失值与重复记录清洗、准确率核算。字段解释和练习答案包含在课程数据及网页中。

## 公开数据

| 文件 | 记录 | 署名与来源 | 处理 |
| --- | --- | --- | --- |
| iris.csv | 150行、4特征及类别 | Fisher, R. A. (1936), Iris, UCI Machine Learning Repository, [DOI:10.24432/C56C76](https://doi.org/10.24432/C56C76) | 官方ZIP中的iris.data，添加英文表头、规范换行，保留原始数据值 |
| wine.csv | 178行、13特征及类别 | Aeberhard, S. & Forina, M. (1992), Wine, UCI Machine Learning Repository, [DOI:10.24432/C5PC7J](https://doi.org/10.24432/C5PC7J) | 官方ZIP中的wine.data，添加英文表头、规范换行，保留原始数据值 |

两份数据遵循 [Creative Commons Attribution 4.0 International（CC BY 4.0）](https://creativecommons.org/licenses/by/4.0/)。本项目不是原始数据作者；使用时请保留署名和修改说明。原始网址、下载地址、核对日期和文件SHA-256记录于 `sources.json`。课程中使用Iris时沿用原始iris.data，不声称已修订原始记录。

这些数据用于教学，并非四川或科教兴川考试原题素材。请勿把真实个人信息加入公共练习数据。
