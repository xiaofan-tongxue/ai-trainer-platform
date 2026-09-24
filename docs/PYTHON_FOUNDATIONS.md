# Python 从零开始学习指南

入口为 `/python.html`。课程按12个单元拆为48小课，每课先说明概念和用途，再展示完整代码与逐行解释，随后完成一道理解题、一道跟做题及一道独立练习。原41道练习移至 `/python-practice.html`，作为基础课程之后的补充。

## 学习顺序

| 小课 | 内容 |
| --- | --- |
| 01–04 | 运行代码、输出、注释、读懂报错 |
| 05–08 | 变量、更新值、类型、运算 |
| 09–12 | 类型转换、输入、格式化、字符串 |
| 13–16 | 比较、分支、多个条件 |
| 17–20 | 列表、索引、切片、修改与排序 |
| 21–24 | 遍历、range、累加、循环退出 |
| 25–28 | 字典、缺失键、多行记录、元组与集合 |
| 29–32 | 函数、参数、返回值、作用域 |
| 33–36 | 模块、异常、断言、解释器与虚拟环境 |
| 37–40 | 文本文件、CSV、JSON |
| 41–44 | NumPy、pandas、缺失值、分组统计 |
| 45–48 | 标签清洗、去重、准确率、训练与测试划分 |

每课至少预留30分钟理解与尝试，48课约24小时基础阅读及练习，另预留12–24小时复习与小项目；困难内容可以拆成多次。每天只有15分钟时只做一个步骤。七天学习建议从前几课开始，不能把一周计划或四节备考概览等同于学完Python。

卡住时先点“我还是不知道从哪一步开始”，再看逐行解释与常见错误；跟做题的提示逐条出现。可以查看参考解法，但建议合上答案再试。运行结果与给定样例相符只说明这一组输入输出吻合，不证明代码对所有输入正确，更不代表考试合格。

## 运行与记录

- 基础运行和结果对照不需要DeepSeek。首次加载浏览器Python及NumPy/pandas需要访问jsDelivr；加载失败会提示重试。
- 代码在独立Web Worker中运行，提供停止按钮和超时终止。练习文件位于浏览器虚拟文件系统，每次运行重置，不读取电脑个人文件。
- 已读、理解题答对、跟做/独立练习完成记录和最近课程保存到当前账号的数据库；重新登录或使用连接同一平台的其他浏览器可恢复。修改练习代码不会抹掉历史完成记录。“已读”可手动标记，与完成三项练习分开显示。
- 保存状态显示在课程顶部。断网时显示“尚未同步”，保持页面打开并点击“重试同步”；退出登录会先等待保存。旧标签页中仍存在的进度会自动补存，已被浏览器清除的旧记录无法凭空恢复。
- 代码草稿仍暂存在当前标签页，不上传到进度接口，重要代码请点击下载。学习标记是个人学习记录，不是服务器执行代码的成绩，也不纳入考试通过率估计。
- 只有主动点击“请AI解释这次代码”才发送该题代码、输入和输出供DeepSeek解释；无需配置AI即可继续学习。
- 服务端基础计划仍依据24节备考概览的完成情况。48课记录用于学习导航，不改变基础计划或正式考试估计，不能据此声称已掌握Python。

## 资料与数据

课程讲解、示范及题目为本项目编写。官方文档用于核对技术细节和延伸阅读；Python官方教程主要面向已有编程基础者，因此不直接用文档目录替代零基础教学。

- [Python入门入口](https://www.python.org/about/gettingstarted/)及[中文教程](https://docs.python.org/zh-cn/3.13/tutorial/)
- [CSV官方说明](https://docs.python.org/zh-cn/3.13/library/csv.html)
- [NumPy初学者指南](https://numpy.org/doc/stable/user/absolute_beginners.html)
- [pandas入门教程](https://pandas.pydata.org/docs/getting_started/intro_tutorials/index.html)
- [scikit-learn小型数据集说明](https://scikit-learn.org/stable/datasets/toy_dataset.html)

5份练习数据包括3份纯模拟表、[UCI Iris](https://archive.ics.uci.edu/dataset/53/iris)和[UCI Wine](https://archive.ics.uci.edu/dataset/109/wine)。网页提供CSV下载、字段解释、练习任务及结果核对。真实公开数据依照CC BY 4.0署名，详情见[数据来源说明](../data/python-course/README.md)。这些是教学练习资料，不是科教兴川历次考试真题，也不作为具体批次考纲证据。

## 维护与验证

编辑 `tools/python-course/content.py` 后运行：

```text
python tools/python-course/build.py
python tools/python-course/verify.py
node tools/check-frontend.js
node tools/python-course/test-browser.js
```

生成器输出课程JSON及浏览器使用的JS数据。保留本地公开CSV时不会再次下载；明确更新数据时使用 `--refresh-datasets`。验证器需要Python、NumPy、pandas，会执行144段示例和参考答案，并核对数据行数与来源文件哈希。浏览器验证需要Node、Playwright、Edge及联网，使用独立模拟登录，不连接个人数据库。

源码中的课程更新不自动修改已发布的旧安装包。`v1.0.0`一键安装包不包含此次扩充。
