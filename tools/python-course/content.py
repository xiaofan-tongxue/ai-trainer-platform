"""Original beginner lessons. Build with build.py; not copied from linked tutorials."""
LESSONS = []
UNITS = [
 ("先认识代码", "会找到编辑区、运行代码、读输出，允许先照着做。"),
 ("数据与变量", "知道一个值是什么、放在哪里、如何参与计算。"),
 ("输入与文字", "把输入的文字变成数据，再把结果讲清楚。"),
 ("让程序判断", "先说清规则，再写条件，检查边界。"),
 ("用列表装数据", "从一个值走到一组值，理解位置和修改。"),
 ("一步步理解循环", "先手算每轮变化，再让程序批量执行。"),
 ("记录与容器", "用字段名描述一条记录，理解表格的数据结构。"),
 ("把步骤写成函数", "区分定义、调用、参数、返回值与作用域。"),
 ("模块、排错与环境", "遇到错误知道从哪里查，知道包与环境的区别。"),
 ("文件与表格", "保存、读取、解释 CSV 和 JSON，不跳过路径与编码。"),
 ("数据工具入门", "在列表和字典基础上理解 NumPy、pandas 和清洗。"),
 ("小项目与复习", "独立完成整理、去重、统计和数据划分。")
]

def task(prompt, starter, solution, expected, hint, inputs=""):
    return dict(prompt=prompt, starter=starter, solution=solution, expected=expected,
                hints=[hint, "先用纸或注释写出输入、处理、输出三步；运行后逐行对照预期结果。"], inputs=inputs)

def add(title, why, concepts, annotated, expected, guided, independent, bug, question, choices, answer, explanation):
    number=len(LESSONS)+1
    lines=[line.split(" | ",1) for line in annotated.splitlines()]
    assert all(len(line)==2 for line in lines), title
    LESSONS.append(dict(id=f"py-{number:02d}",unit=(number-1)//4,title=title,minutes=30,
        prerequisite=f"py-{number-1:02d}" if number>1 else None, why=why, concepts=concepts,
        example="\n".join(line[0] for line in lines), lines=[dict(code=c,explanation=e) for c,e in lines],
        expected=expected, guided=guided, independent=independent, bug=bug,
        quiz=dict(question=question,choices=choices,answer=answer,explanation=explanation)))

add("程序到底在哪里运行", "先认识三个位置：编辑区写指令，运行按钮启动解释器，输出区显示结果。你不需要先安装软件。",
 ["代码是一组按顺序执行的指令。Python 解释器负责读懂这些指令。网页的运行按钮会启动浏览器中的 Python，它不会替你猜出想做什么。",
  "本课只用 print：英文单词后紧跟一对英文圆括号，括号中的引号包住要显示的文字。点击『运行示例』，再对照两行输出，观察先后顺序。",
  "本页每次运行使用新的变量环境。网页文件是练习用的临时文件，不是电脑磁盘上的文件。首次运行需联网；加载失败仍可阅读所有讲解。"],
 'print("开始学习") | 第一条指令：把引号里的中文显示出来；引号本身不显示。\nprint("完成第一步") | 第二条指令在第一条之后执行，自动换到下一行。', "开始学习\n完成第一步",
 task("把第二行文字改成『我会运行了』，保留第一行。", 'print("开始学习")\nprint("完成第一步")', 'print("开始学习")\nprint("我会运行了")', "开始学习\n我会运行了", "只改第二行引号内部的文字，保留 print 和括号。"),
 task("独立写两条指令，先显示『读取数据』，再显示『检查结果』。", '# 第一步：显示读取数据\n# 第二步：显示检查结果', 'print("读取数据")\nprint("检查结果")', "读取数据\n检查结果", "一条 print 对应一行输出，顺序决定先显示哪一句。"),
 "点击运行却没有新结果：先确认代码已写入编辑区，等待首次运行时加载；不要连续点击。", "这两条指令以什么顺序执行？", ["从上到下", "从下到上", "随机"],0,"没有循环和分支时，Python 按书写顺序逐行执行。")

add("print、引号与数字", "分清程序语句和输出内容，避免把看见的文字直接当代码写。",
 ["print 是显示内容的工具，圆括号说明我们在调用它。英文双引号或单引号都能包住文字，左右必须配对。中文弯引号不是 Python 的字符串引号。",
  "数字不加引号时可以运算；加引号之后，它就是文字。print(2 + 3) 先计算再显示，print('2 + 3') 显示原样文本。",
  "逗号可以把多个内容交给 print，默认在内容之间放一个空格。输出区看到的 5 和文字 '5' 外观相同，后面我们会用 type 看区别。"],
 'print(2 + 3) | 先把 2 加 3 算成 5，再显示数字。\nprint("2 + 3") | 引号中的加号只是文字，不执行加法。\nprint("数量", 5) | 逗号分隔两个内容，输出时默认用空格分开。', "5\n2 + 3\n数量 5",
 task("把第一行改成计算 4 + 6，其余不变。", 'print(2 + 3)\nprint("2 + 3")\nprint("数量", 5)', 'print(4 + 6)\nprint("2 + 3")\nprint("数量", 5)', "10\n2 + 3\n数量 5", "算式应在引号外。"),
 task("分别输出 8 - 3 的计算结果、文字『8 - 3』，最后显示『结果 5』。", '# 三条 print：算式、文字、说明', 'print(8 - 3)\nprint("8 - 3")\nprint("结果", 5)', "5\n8 - 3\n结果 5", "最后一行可用 print('结果', 5)。"),
 "print(“你好”) 会报语法错误：把中文弯引号换为英文直引号。", "哪一句会进行加法计算？", ['print("2 + 3")', 'print(2 + 3)', 'print("加法")'],1,"只有引号外的 2 + 3 是运算表达式。")

add("注释、空行与可读性", "看懂哪些内容给人看，哪些指令会真正执行。",
 ["井号 # 开始的内容叫注释：解释器忽略它，但读代码的人能看到。用它说明目的，比每行都写『这是代码』更有帮助。",
  "引号里面的 # 是文字，不会开始注释。空行可以分隔步骤，通常不影响程序结果。print() 的空括号则会真正输出一个空白行。",
  "先把任务用中文注释拆成小步骤，再在每个步骤下补一条指令。不要一次写完一大段再找错。"],
 '# 展示处理流程 | 注释只解释目的，不出现在输出中。\nprint("采集") | 第一条实际执行的指令。\nprint() | 没有内容也会输出换行，形成一个空白行。\nprint("# 标注") | 这里的井号在引号里面，会作为文字显示。', "采集\n\n# 标注",
 task("将第一行输出改为『准备』，保留空白行和最后一行。", '# 展示处理流程\nprint("采集")\nprint()\nprint("# 标注")', '# 展示处理流程\nprint("准备")\nprint()\nprint("# 标注")', "准备\n\n# 标注", "改 print 的文字，不必改注释。"),
 task("用注释写『检查流程』，实际输出两行『检查』和『保存』，中间不要空白行。", '# 检查流程\n# 在下面补两条指令', '# 检查流程\nprint("检查")\nprint("保存")', "检查\n保存", "# 后的内容不会显示，显示内容需要 print。"),
 "只写 # 输出完成，不会真的输出『完成』；注释不能代替指令。", "哪一行不会产生屏幕输出？", ['print("# 完成")', '# 显示完成', 'print()'],1,"注释被解释器忽略；print() 虽没有文字，仍输出换行。")

add("第一次读报错：不要慌", "错误信息是定位提示。本课先认识拼写和括号问题，不要求背异常类型。",
 ["先看错误信息的最后一行，它通常给出错误类型和简短原因；再找提示的行号，回到编辑区检查这一行及上一行。",
  "SyntaxError 表示语法没拼好，常见于少引号或少右括号。NameError 表示 Python 不认识某个名字，例如把 print 写成 Print。大小写有区别。",
  "一次只修改一个疑点，然后重新运行。下面先展示一段正确代码；跟做任务故意给了一个拼写错误，请根据提示修好。红色输出不代表你不适合编程。"],
 'print("检查拼写") | print 全部小写，括号和引号成对。\nprint("检查括号") | 从左到右数一对引号、一对括号，确认闭合。', "检查拼写\n检查括号",
 task("把 Print 改为正确的函数名，使程序显示『修好了』。", 'Print("修好了")', 'print("修好了")', "修好了", "Python 区分大小写：内置显示函数是 print。"),
 task("修复缺少的右括号，输出『可以继续』。", 'print("可以继续"', 'print("可以继续")', "可以继续", "在行末补上与左括号配对的 )。"),
 "不要直接删除所有代码；先定位报错的那一行，保留已经正确的部分。", "看到报错后，较合适的第一步是？", ["反复点运行", "读最后一行错误和行号", "直接跳过本课"],1,"先定位原因，再小步修复，比盲目重写有效。")

add("变量：给一个值起名字", "先学会保存与读取一个值，再做复杂计算。",
 ["变量可以理解为贴在值上的名字。count = 3 的意思是让名字 count 指向数值 3；它不是数学中的相等判断。",
  "赋值先计算右边，再交给左边的名字。print(count) 读取这个名字目前指向的值；print('count') 只显示名字的文字。",
  "变量名用英文、数字、下划线组合，但不能以数字开头，不加空格。用 sample_count 表示样本数量，比随便取一个 x 更容易理解。"],
 'sample_count = 3 | 创建变量 sample_count，保存整数 3。\nprint(sample_count) | 不加引号，读取变量中的值。\nprint("sample_count") | 加引号，显示这串文字本身。', "3\nsample_count",
 task("把样本数量改成 6，两条 print 保留。", 'sample_count = 3\nprint(sample_count)\nprint("sample_count")', 'sample_count = 6\nprint(sample_count)\nprint("sample_count")', "6\nsample_count", "只改赋值右边的 3。"),
 task("创建变量 task_count，值为 4，再输出这个变量的值。", '# 创建 task_count\n# 读取并输出', 'task_count = 4\nprint(task_count)', "4", "先 task_count = 4，再 print(task_count)，不要给变量名加引号。"),
 "NameError: name 'count' is not defined：先赋值再读取，并检查拼写是否一致。", "sample_count = 3 中的等号表示？", ["赋值", "判断是否相等", "输出"],0,"等号让左侧名字指向右侧的值；比较相等要用两个等号。")

add("变量更新：跟踪每一步", "能在纸上写出每行之后变量的值，循环累加就不再神秘。",
 ["同一个变量可以再次赋值，新值会替换旧的绑定。total = total + 2 先读取旧 total，算出结果，再把新值交给 total。",
  "这里不是要求某个数学方程成立。假设旧值是 3，右边先算成 5，左边就更新成 5。",
  "可以在每次更新后加 print 观察中间值。以后看到 total += 2，它是这种累加写法的简写；先理解长写法再使用简写。"],
 'total = 3 | 初始值是 3。\nprint(total) | 先观察初始值。\ntotal = total + 2 | 读取旧值 3，算出 5，再更新 total。\nprint(total) | 现在读到的是新值 5。', "3\n5",
 task("把增加量改为 4，观察更新前后。", 'total = 3\nprint(total)\ntotal = total + 2\nprint(total)', 'total = 3\nprint(total)\ntotal = total + 4\nprint(total)', "3\n7", "保留初始值，只修改加号后的数字。"),
 task("库存 stock 初始为 10，减少 3 后输出剩余库存。", 'stock = 10\n# 更新 stock，然后输出', 'stock = 10\nstock = stock - 3\nprint(stock)', "7", "右边使用旧 stock；只是写 stock - 3 不会保存结果。"),
 "写 total + 2 但不赋值，total 的值不会被更新。", "执行 n = 2，再执行 n = n + 1 后，n 是？", ["2", "3", "报错"],1,"先用旧值 2 算出 3，再赋给 n。")

add("类型：数字、文字和真假", "相同外观的数据可能属于不同类型，先认清再运算。",
 ["int 表示整数，float 表示小数，str 表示文字，bool 表示真假。True 和 False 是布尔值，首字母大写；加引号后就成了普通文字。",
  "type(值) 可以询问类型。程序不会把 '3' 自动当成数字 3；两段文字相加是连接，而两个数字相加才是计算。",
  "变量不需要提前声明类型，但保存的值仍然有类型。读取 CSV 时很多看起来像数字的字段其实是文字，后面需要显式转换。"],
 'print(type(3)) | 整数的类型是 int。\nprint(type("3")) | 带引号的 3 是 str。\nprint(type(3.5)) | 含小数点的数通常是 float。\nprint(type(True)) | 布尔值的类型是 bool。', "<class 'int'>\n<class 'str'>\n<class 'float'>\n<class 'bool'>",
 task("输出 type(8) 和 type('8')，各占一行。", 'print(type(8))\n# 再查询文字的类型', 'print(type(8))\nprint(type("8"))', "<class 'int'>\n<class 'str'>", "第二次在 8 两边加英文引号。"),
 task("分别输出 3 + 4 和 '3' + '4'，观察差异。", '# 数字相加\n# 文字连接', 'print(3 + 4)\nprint("3" + "4")', "7\n34", "文字连接不会进行数值加法。"),
 "print('数量' + 3) 会出现 TypeError；先用逗号分隔，或在第9课学习类型转换。", "CSV 字段 '60' 带引号时属于什么类型？", ["int", "bool", "str"],2,"引号包住的内容是字符串，即使里面都是数字。")

add("四则运算与括号", "先用小数字观察运算规则，不把复杂数学当成学编程的门槛。",
 ["+、-、*、/ 分别是加、减、乘、除。Python 用星号乘法，不用数学里的 ×。/ 得到除法结果，// 取整除结果，% 取余数，** 表示乘方。",
  "先算括号，再乘除，再加减。2 + 3 * 4 得到 14，(2 + 3) * 4 得到 20。拿不准时用括号明确意图。",
  "除数不能为零。小数计算存在精度误差，金额等业务要专门选用 Decimal；这一步先掌握运算顺序，不要求立刻处理金融精度。"],
 'print(2 + 3 * 4) | 先算 3*4，再加 2。\nprint((2 + 3) * 4) | 括号改变运算顺序。\nprint(7 // 3) | 7 分成每组 3 个，共 2 个完整组。\nprint(7 % 3) | 分组后还剩 1 个。', "14\n20\n2\n1",
 task("有 10 条数据，每组 3 条，输出完整组数和剩余条数。", 'print(10 // 3)\n# 再输出余数', 'print(10 // 3)\nprint(10 % 3)', "3\n1", "完整组数用 //，剩余数量用 %。"),
 task("每条任务用时 5 分钟，3 条任务加上 2 分钟准备，计算并输出总分钟数。", 'task_minutes = 5\ntask_count = 3\n# 计算总用时', 'task_minutes = 5\ntask_count = 3\ntotal = task_minutes * task_count + 2\nprint(total)', "17", "先乘任务数，再加准备时间。"),
 "把乘法写成 x 或 × 都不是 Python 乘法；请用键盘上的 *。", "(2 + 3) * 4 的结果是？", ["14", "20", "24"],1,"先执行括号中的加法，5 再乘 4。")

add("把文字转换成数字", "从输入和表格读取的数字常常是字符串，要先转换再计算。",
 ["int('12') 把整数字符串变成整数，float('12.5') 把小数字符串变成小数，str(12) 把数字变成文字。函数括号里放需要转换的值。",
  "转换不会自动改动原变量：count = int(raw) 创建数字 count，而 raw 仍然是字符串。给原始值和处理结果不同的名字更容易排查问题。",
  "int('十二') 和 int('12.5') 都会失败，不能把任意文字变成整数。处理小数字符串要先确定业务是否允许小数。不要靠截断小数假装数据正确。"],
 'raw = "12" | 模拟表格读到的文字。\ncount = int(raw) | 把可识别的整数字符串转成整数。\nprint(count + 1) | 数字加 1 得到 13。\nprint(raw + "1") | 原始文字仍是字符串，连接后得到 121。', "13\n121",
 task("将 raw 改成 '20'，保持后面三行，比较结果。", 'raw = "12"\ncount = int(raw)\nprint(count + 1)\nprint(raw + "1")', 'raw = "20"\ncount = int(raw)\nprint(count + 1)\nprint(raw + "1")', "21\n201", "只修改原始输入文字。"),
 task("把 raw = '2.5' 转成小数，再乘 2，输出结果。", 'raw = "2.5"\n# 用 float 转换', 'raw = "2.5"\nvalue = float(raw)\nprint(value * 2)', "5.0", "带小数点的字符串使用 float。"),
 "ValueError 表示值不适合转换，例如 int('abc')；先检查原始数据而不是反复运行。", "把 '2.5' 转成小数应使用？", ["float", "int", "print"],0,"float 接受小数字符串；int 不接受带小数点的字符串。")

add("input：向程序提供输入", "理解输入永远先是文字，知道程序为何等待输入。",
 ["input() 会读取一行输入并把它作为字符串返回。把结果赋给变量，之后才能使用。input('提示') 还能显示提示文字，但那段提示也会进入输出。",
  "本页运行区提供『输入数据』文本框：每行对应一次 input()。它不是代码区。本课示例默认提供一行 3，首次先使用这个值观察流程。",
  "如果要做加法，先 int 转换。输入不足会出现 EOFError；检查 input 调用次数和输入行数是否一致。本地终端中的 input 会等待键盘回车，本页使用预先填写的行。"],
 'raw = input() | 从输入框取第一行，本课默认是文字 3。\ncount = int(raw) | 将输入转成整数。\nprint(count + 2) | 输出 3 加 2 的结果。', "5",
 task("输入框填 4，把程序改为输入数加 3。", 'raw = input()\ncount = int(raw)\nprint(count + 2)', 'raw = input()\ncount = int(raw)\nprint(count + 3)', "7", "跟做的输入是 4，最后加 3。", "4"),
 task("输入框两行分别是 2 和 5，读取两个整数并输出它们的和。", '# 读取第一行\n# 读取第二行\n# 输出相加结果', 'first = int(input())\nsecond = int(input())\nprint(first + second)', "7", "input() 每调用一次就取下一行，两次都要转整数。", "2\n5"),
 "把 input 写成 input 而没有括号，只是引用函数，没有读取输入。", "input() 返回的原始类型是？", ["总是整数", "字符串", "由用户输入自动决定"],1,"即使输入 3，得到的也是字符串 '3'。")
LESSONS[-1]['exampleInputs']='3'

add("把结果写清楚：f-string", "不只得到一个数字，还能让读结果的人知道它代表什么。",
 ["在字符串前加 f，可以在花括号中读取变量或表达式。f'数量：{count}' 中的 count 会替换成它的值。没有 f 时花括号会原样显示。",
  "{value:.2f} 的含义是按小数格式显示两位小数。冒号后是显示规则，不是赋值，也不会改变 value 原本的数值。",
  "先用 print(count) 验证数字，再给它加说明文字。格式化属于表达结果，不应和复杂公式一起首次学习。"],
 'count = 3 | 准备数量变量。\nrate = 0.8 | 准备一个小数。\nprint(f"数量：{count}") | 花括号读取 count 的值。\nprint(f"比例：{rate:.2f}") | 显示两位小数，即 0.80。', "数量：3\n比例：0.80",
 task("把 count 改为 5，其余代码不变。", 'count = 3\nrate = 0.8\nprint(f"数量：{count}")\nprint(f"比例：{rate:.2f}")', 'count = 5\nrate = 0.8\nprint(f"数量：{count}")\nprint(f"比例：{rate:.2f}")', "数量：5\n比例：0.80", "只修改变量 count。"),
 task("设 average = 7.5，输出『平均值：7.50』。", 'average = 7.5\n# 用 f-string 保留两位小数', 'average = 7.5\nprint(f"平均值：{average:.2f}")', "平均值：7.50", "把 average:.2f 写在花括号中，字符串前不要漏 f。"),
 "输出变成『数量：{count}』：检查引号前是否少了 f。", "保留两位小数的显示规则是？", [".2f", "+2", "int"],0,"在 f-string 的花括号中使用冒号和 .2f。")

add("文字整理：strip、split、join", "学习清理文字的三个小工具，为读取表格做准备。",
 ["方法是某个对象提供的操作。raw.strip() 中，点号表示使用 raw 这个字符串的方法；括号表示真正执行。strip 去掉两端空白，不删除中间的空格。",
  "split(',') 按逗号切分，得到一组文字，称为列表；这节先观察它的显示形式，下一单元会专门学习列表。join 则用指定分隔符重新连接。",
  "字符串方法返回新的结果，不会直接修改原字符串。用新的变量接住每一步结果，可以清楚看到清洗过程。"],
 'raw = "  猫,狗  " | 原始文字两端有空格，中间用逗号分隔。\nclean = raw.strip() | 去掉两端空格，保留中间内容。\nparts = clean.split(",") | 按逗号分成猫和狗两项。\nprint(" / ".join(parts)) | 用空格、斜线、空格连接两项。', "猫 / 狗",
 task("把原始文字改成 '  红,蓝  '，保持清理步骤。", 'raw = "  猫,狗  "\nclean = raw.strip()\nparts = clean.split(",")\nprint(" / ".join(parts))', 'raw = "  红,蓝  "\nclean = raw.strip()\nparts = clean.split(",")\nprint(" / ".join(parts))', "红 / 蓝", "只改 raw 的文字，仍保留逗号。"),
 task("把 '  训练;测试  ' 去掉两端空格，再按分号切分，最后用 '、' 连接并输出。", 'raw = "  训练;测试  "\n# 清理、切分、连接', 'raw = "  训练;测试  "\nclean = raw.strip()\nparts = clean.split(";")\nprint("、".join(parts))', "训练、测试", "split 使用分号，join 前面的字符串是顿号。"),
 "raw.strip 少了括号，不会执行清理；需要 raw.strip()。", "strip() 默认去掉哪里空白？", ["所有位置", "两端", "只去掉中间"],1,"strip 默认处理两端空白，不改变中间的空格。")

add("比较：程序怎样得到真假", "在写 if 前，先单独看一个条件的结果。",
 ["比较表达式会产生 True 或 False。== 判断相等，!= 判断不相等；>、<、>=、<= 用来比较大小。两个等号不要写成赋值用的一个等号。", "score >= 60 包含刚好 60，score > 60 不包含。先用边界值验证条件，可以避免『大多数时候对，刚好达线时错』。", "通常只对含义一致的数据作比较。数字 60 和文字 '60' 不应直接进行大小比较，先检查类型、完成转换。"],
 'score = 60 | 把边界值保存起来。\nprint(score >= 60) | 等于 60 也满足大于等于条件。\nprint(score > 60) | 等于 60 不满足严格大于。\nprint(score == 60) | 两个等号用于判断相等。', 'True\nFalse\nTrue',
 task('把 score 改为 59，观察三个条件。','score = 60\nprint(score >= 60)\nprint(score > 60)\nprint(score == 60)','score = 59\nprint(score >= 60)\nprint(score > 60)\nprint(score == 60)','False\nFalse\nFalse','只改分数，分别判断它与 60 的关系。'),
 task('设 count = 10，依次输出它是否等于 10、是否不等于 8。','count = 10\n# 写两个比较','count = 10\nprint(count == 10)\nprint(count != 8)','True\nTrue','不等于写成 !=。'),
 '比较用 ==；赋值用 =，两者用途不同。','哪一个条件包含分数正好 60？',['score > 60','score >= 60','score < 60'],1,'大于等于的等号包含边界。')

add("if 与 else：一次只走一条路", "把一句规则翻译成程序，先只写两个分支。",
 ["if 后放条件，行末加英文冒号。下一行缩进四个空格，表示这一行属于 if。else 表示条件不成立时走这里，也需要冒号和缩进。", "缩进不是装饰，它决定代码的归属。同一层使用同样数量空格。分支外的代码恢复到最左边，不论走哪条路都会执行。", "先把 score 设为 60，再改为 59 对照结果。每改一个输入，就先在脑中预测会走哪条分支。"],
 'score = 60 | 准备输入值。\nif score >= 60: | 检查条件，冒号表示下面开始一个代码块。\n    print("达线") | 条件为真才执行，前面有四个空格。\nelse: | 条件为假时走另一条路。\n    print("复习") | 这行属于 else。', '达线',
 task('将 score 改成 59，使程序走 else。','score = 60\nif score >= 60:\n    print("达线")\nelse:\n    print("复习")','score = 59\nif score >= 60:\n    print("达线")\nelse:\n    print("复习")','复习','不需要修改条件，只改变输入分数。'),
 task('设 count = 0；如果 count > 0 输出『有数据』，否则输出『无数据』。','count = 0\n# 写 if 和 else','count = 0\nif count > 0:\n    print("有数据")\nelse:\n    print("无数据")','无数据','两条 print 都要缩进四个空格。'),
 'IndentationError 通常是该缩进时没有缩进，或同一层空格数不一致。','else 中的代码什么时候执行？',['每次都会执行','if 条件不成立时','if 条件成立时'],1,'if 和 else 在一次判断中只走其中一条路径。')

add("elif：多个区间与边界", "先画出三个区间，再按顺序写判断。",
 ["elif 是『前面的条件不成立，再检查这个条件』。一旦某个分支匹配，后面的 elif 和 else 就不再执行。", "把分数分为 80 以上、60 到 79、60 以下三个区间。先判断较高门槛，才不会让高分被较低门槛提前接走。", "测试时选 59、60、79、80 四个值，覆盖边界两侧。一个测试值通过并不表示所有区间都正确。"],
 'score = 75 | 此值位于中间区间。\nif score >= 80: | 先检查较高门槛。\n    print("稳定") | 高分进入这里。\nelif score >= 60: | 能走到这里说明前面的 >=80 已经不成立。\n    print("继续巩固") | 60 至 79 进入这里。\nelse: | 前面两个条件都不成立。\n    print("回到基础") | 低于 60 的情况。', '继续巩固',
 task('将分数改为 80，验证高门槛边界。','score = 75\nif score >= 80:\n    print("稳定")\nelif score >= 60:\n    print("继续巩固")\nelse:\n    print("回到基础")','score = 80\nif score >= 80:\n    print("稳定")\nelif score >= 60:\n    print("继续巩固")\nelse:\n    print("回到基础")','稳定','只改 score，观察首个条件。'),
 task('温度 temp = 0；大于 0 输出『正温』，小于 0 输出『负温』，其余输出『零度』。','temp = 0\n# 写三个互斥分支','temp = 0\nif temp > 0:\n    print("正温")\nelif temp < 0:\n    print("负温")\nelse:\n    print("零度")','零度','先分清 >0、<0 和剩下的 ==0。'),
 '若先写 >=60 再写 >=80，高分会被第一个分支接走。','if 已经匹配后，后续 elif 是否还执行？',['不执行','全部执行','随机执行'],0,'同一组 if/elif/else 只执行首个匹配分支。')

add("and、or、not：组合规则", "把『同时』『至少一个』『不是』对应到三个逻辑词。",
 ["and 要求两边同时成立。or 要求至少一边成立。not 把真假反过来。先把每个小条件写对，再把它们连接。", "理论与技能都达线用 and。注意不要写成 theory >= 60 and skill，右边应完整写出 skill >= 60。", "复杂条件先拆成有名字的变量，例如 theory_ok 和 skill_ok，再观察它们的值。不要为了少写一行而把规则挤得看不懂。"],
 'theory = 65 | 理论成绩。\nskill = 55 | 技能成绩。\nprint(theory >= 60 and skill >= 60) | 第二个条件不成立，因此整体为假。\nprint(theory >= 60 or skill >= 60) | 至少一个条件成立，因此整体为真。\nprint(not skill >= 60) | 对技能是否达线的结果取反。', 'False\nTrue\nTrue',
 task('把 skill 改成 60，观察三个逻辑结果。','theory = 65\nskill = 55\nprint(theory >= 60 and skill >= 60)\nprint(theory >= 60 or skill >= 60)\nprint(not skill >= 60)','theory = 65\nskill = 60\nprint(theory >= 60 and skill >= 60)\nprint(theory >= 60 or skill >= 60)\nprint(not skill >= 60)','True\nTrue\nFalse','分别判断两个分数，再应用逻辑词。'),
 task('设 value = 5，判断它是否同时满足大于等于 0 且小于等于 10，输出真假。','value = 5\n# 连接两个完整比较','value = 5\nprint(value >= 0 and value <= 10)','True','区间包含两端，所以两边都使用等号。'),
 '用 or 表达『两项都满足』会放过只有一项达标的情况。','两项条件都必须成立应使用？',['or','not','and'],2,'and 对应同时满足。')

add("列表：把多个值放在一起", "从一个样本走向一组样本，认识中括号和元素。",
 ["列表用中括号包住多个元素，元素之间用逗号分隔，例如 [60, 70, 80]。列表本身也是一个值，可以赋给变量。", "len(scores) 返回元素个数，而不是各元素的和。空列表 [] 的长度是 0。列表顺序会保留，后面可以按位置取数据。", "先保持同一列表中的元素含义一致，例如都放分数。虽然 Python 允许混合类型，但学习阶段不要把成绩、姓名、文件对象随意混在一起。"],
 'scores = [60, 70, 80] | 创建含三个整数的列表。\nprint(scores) | 直接显示整个列表，包含中括号和逗号。\nprint(len(scores)) | 查询元素个数。', '[60, 70, 80]\n3',
 task('给示例列表末尾再增加 90，显示列表和长度。','scores = [60, 70, 80]\nprint(scores)\nprint(len(scores))','scores = [60, 70, 80, 90]\nprint(scores)\nprint(len(scores))','[60, 70, 80, 90]\n4','在列表内补逗号和 90。'),
 task('创建 labels = ["猫", "狗"]，只输出列表长度。','# 创建标签列表并统计数量','labels = ["猫", "狗"]\nprint(len(labels))','2','两个文字元素都要加引号。'),
 'len([10,20]) 是 2，不是 30；长度和求和是不同的问题。','空列表 [] 的长度是？',['0','1','无法计算'],0,'没有任何元素时，长度为 0。')

add("索引：为什么从 0 开始", "用位置找到一个元素，避免越界。",
 ["列表第一项的位置编号是 0，第二项是 1，第三项是 2。这个位置编号叫索引。scores[0] 只取一个元素，不再是整个列表。", "长度为 3 的列表，最后一个非负索引是 2。索引等于长度时已经越界，会出现 IndexError。", "-1 表示最后一项，-2 表示倒数第二项。写代码前先在纸上给列表元素标号，能够很快判断位置是否正确。"],
 'labels = ["猫", "狗", "鸟"] | 三项的位置分别是 0、1、2。\nprint(labels[0]) | 取第一项。\nprint(labels[1]) | 取第二项。\nprint(labels[-1]) | 取最后一项。', '猫\n狗\n鸟',
 task('只输出列表第三项，请将索引修正。','labels = ["猫", "狗", "鸟"]\nprint(labels[3])','labels = ["猫", "狗", "鸟"]\nprint(labels[2])','鸟','长度是 3，但第三项的索引是 2。'),
 task('列表 scores = [50, 60, 90]，分别输出第一项与最后一项。','scores = [50, 60, 90]\n# 两次取值','scores = [50, 60, 90]\nprint(scores[0])\nprint(scores[-1])','50\n90','第一个使用 0，最后一个可以使用 -1。'),
 'IndexError: list index out of range 表示位置超出了列表范围。','长度为 4 的列表，最后一个非负索引是？',['4','3','1'],1,'索引从 0 开始，因此最后一个是长度减 1。')

add("切片：取一段而不是一个", "把范围的起点和终点讲清楚，后面才能理解数据划分。",
 ["列表[start:stop] 取一段元素，包含 start，不包含 stop。scores[1:3] 取索引 1 和 2。可以把 stop 想成停在那个位置之前。", "省略起点默认从开头开始，省略终点默认到末尾。因此 [:2] 取前两项，[2:] 取剩下的项。切片结果仍是列表。", "索引取一个，切片取一段。后面训练集与测试集不能随意混在一起，理解切片先从小列表开始。"],
 'scores = [10, 20, 30, 40] | 四个有顺序的值。\nprint(scores[1:3]) | 取索引 1 和 2，不取索引 3。\nprint(scores[:2]) | 从开头取到索引 2 之前。\nprint(scores[2:]) | 从索引 2 一直取到末尾。', '[20, 30]\n[10, 20]\n[30, 40]',
 task('取前三项，将 [:2] 改成正确范围。','scores = [10, 20, 30, 40]\nprint(scores[:2])','scores = [10, 20, 30, 40]\nprint(scores[:3])','[10, 20, 30]','前三项对应索引 0、1、2，终点写 3。'),
 task('values = [1,2,3,4,5]，取中间的 [2,3,4] 并输出。','values = [1, 2, 3, 4, 5]\n# 使用切片','values = [1, 2, 3, 4, 5]\nprint(values[1:4])','[2, 3, 4]','从索引 1 开始，停在索引 4 之前。'),
 '把 stop 当成包含端点，是切片最常见的偏一错误。','values[1:3] 会取哪些索引？',['1、2、3','1、2','0、1、2'],1,'包含左端点，不包含右端点。')

add("修改列表与排序", "分清原地修改和返回新结果，不把列表变成 None。",
 ["append(值) 会把一个元素加到原列表末尾。它修改列表本身，返回值是 None，因此不要写 items = items.append(值)。", "items[0] = 新值 可以替换某个位置。sorted(items) 则创建排序后的新列表，原列表顺序不变。", "能否修改称为可变性。列表可变，而字符串的方法通常返回新字符串。先观察每一步原列表的状态，再选择是否创建副本。"],
 'scores = [30, 10] | 初始有两个元素。\nscores.append(20) | 原地添加 20。\nprint(scores) | 原列表顺序现在是 30、10、20。\nprint(sorted(scores)) | 显示一个新排序列表。', '[30, 10, 20]\n[10, 20, 30]',
 task('在原列表末尾添加 40，再显示排序后的新列表。','scores = [30, 10]\nscores.append(20)\n# 再添加 40\nprint(sorted(scores))','scores = [30, 10]\nscores.append(20)\nscores.append(40)\nprint(sorted(scores))','[10, 20, 30, 40]','第二次调用 scores.append(40)。'),
 task('items = [1,2]，将第一项改成 9，再在末尾添加 3，输出原列表。','items = [1, 2]\n# 替换，然后添加','items = [1, 2]\nitems[0] = 9\nitems.append(3)\nprint(items)','[9, 2, 3]','替换用索引赋值；添加用 append。'),
 'items = items.append(3) 会让 items 变成 None；直接调用 items.append(3)。','sorted(items) 会怎样？',['返回新的排序列表','必定修改原列表','删除原列表'],0,'sorted 返回新列表；与 list.sort 的原地行为要区分。')

add("for：逐项处理一组数据", "先跟踪三轮执行，不急着写复杂循环。",
 ["for score in scores 的意思是：依次从列表取一个元素，暂时叫它 score，再执行缩进中的代码。不是一次把整个列表交给 score。", "循环体必须缩进。第一轮 score 是 50，第二轮是 60，第三轮是 70。列表没有元素时，循环体一次也不执行。", "先手工写出三轮的变量值与输出，再运行对照。循环的意义是用一段相同逻辑处理任意数量的数据。"],
 'scores = [50, 60, 70] | 准备三项数据。\nfor score in scores: | 每一轮取出一个分数。\n    print(score + 1) | 每轮都用当前分数加 1 并输出。', '51\n61\n71',
 task('把每个分数增加量改为 2。','scores = [50, 60, 70]\nfor score in scores:\n    print(score + 1)','scores = [50, 60, 70]\nfor score in scores:\n    print(score + 2)','52\n62\n72','只修改循环体的加法。'),
 task('逐项遍历 labels = ["猫", "狗"]，每行输出一个标签。','labels = ["猫", "狗"]\n# for 循环','labels = ["猫", "狗"]\nfor label in labels:\n    print(label)','猫\n狗','循环变量 label 表示当前这一项。'),
 'print 没有缩进时不属于循环体；检查冒号和四个空格。','三项列表的 for 循环，正常会执行几轮？',['1','2','3'],2,'每个元素对应一轮循环。')

add("range 与编号", "看清开始、停止、步长，不把边界背成口诀。",
 ["range(1,4) 依次给出 1、2、3，停止位置 4 不包含。range(3) 省略起点，默认从 0 开始，即 0、1、2。", "range 的第三个参数是步长，例如 range(3,0,-1) 倒着产生 3、2、1。步长不能是 0。", "给已有列表编号可用 enumerate，它同时提供序号和元素。本课先掌握 range 的边界；复杂的解包写法稍后再学。"],
 'for number in range(1, 4): | 取到 1、2、3，遇到 4 前停止。\n    print(number) | 每轮输出当前整数。', '1\n2\n3',
 task('输出 1 到 5，每个数一行。','for number in range(1, 4):\n    print(number)','for number in range(1, 6):\n    print(number)','1\n2\n3\n4\n5','终点写 6 才能包含 5。'),
 task('使用 range 倒序输出 3、2、1。','# 起点 3，终点不包含 0，步长 -1','for number in range(3, 0, -1):\n    print(number)','3\n2\n1','负步长表示每次减 1。'),
 'range(1,5) 不会产生 5；终点是停止界限。','range(3) 产生什么？',['1、2、3','0、1、2','0、1、2、3'],1,'省略起点时从 0 开始，终点 3 不包含。')

add("累加器：追踪循环中的变化", "把统计总数拆成初值、更新、结果三个动作。",
 ["累加器是一个用来保存阶段结果的变量。它在循环开始前设为 0，每一轮把当前值加进去。", "对于 [2,3,4]，total 依次是 0→2→5→9。把 print 暂时放进循环，可以看到中间状态；放到循环外则只显示最终总数。", "初始化如果写在循环里面，每轮都会重置为 0，前面的结果就丢失了。sum 可以快速求和，但先理解过程，才会写带条件的统计。"],
 'values = [2, 3, 4] | 三个待累加的值。\ntotal = 0 | 只在循环之前初始化一次。\nfor value in values: | 每轮取一个值。\n    total = total + value | 在之前结果上继续增加。\nprint(total) | 退出循环后显示最终结果。', '9',
 task('将 values 改为 [1,2,3,4]，其他步骤保留。','values = [2, 3, 4]\ntotal = 0\nfor value in values:\n    total = total + value\nprint(total)','values = [1, 2, 3, 4]\ntotal = 0\nfor value in values:\n    total = total + value\nprint(total)','10','初始化仍放在循环外。'),
 task('统计 scores = [50,60,80] 中 >=60 的元素个数，只输出数量。','scores = [50, 60, 80]\ncount = 0\n# 循环并按条件增加 count','scores = [50, 60, 80]\ncount = 0\nfor score in scores:\n    if score >= 60:\n        count = count + 1\nprint(count)','2','满足条件时加 1，不是加分数；if 内再缩进一层。'),
 '把 total = 0 放进 for 循环会每轮清零，无法累计。','累加器通常在哪里初始化？',['循环前','每一轮循环里面','输出之后'],0,'初始化一次，之后逐轮更新。')

add("while 与停止条件", "理解为什么循环会结束，并学会处理不结束的代码。",
 ["while 在每一轮开始前检查条件。条件为真就执行循环体，为假就结束。必须安排变量更新，让条件最终有机会变为假。", "下面从 3 倒数，count 每轮减 1，变成 0 时条件 count > 0 不成立。break 可以提前结束循环，但不是每个循环都需要它。", "在本页，运行过久可以点击停止；系统也有超时终止。超时后先找是否漏了更新变量，不要简单地增加运行时间。"],
 'count = 3 | 设定起始值。\nwhile count > 0: | 每轮开始检查剩余次数。\n    print(count) | 显示当前值。\n    count = count - 1 | 更新是停止循环的关键。', '3\n2\n1',
 task('从 2 开始倒数，仍在 0 时停止。','count = 3\nwhile count > 0:\n    print(count)\n    count = count - 1','count = 2\nwhile count > 0:\n    print(count)\n    count = count - 1','2\n1','只改起始值，不要删除更新行。'),
 task('遍历 [1,2,3,4]，输出 1 和 2；遇到 3 立即 break，不再输出后续值。','values = [1, 2, 3, 4]\n# 先判断，再输出','values = [1, 2, 3, 4]\nfor value in values:\n    if value == 3:\n        break\n    print(value)','1\n2','把 break 的判断放在 print 之前。'),
 'while 中漏掉更新会形成无限循环；先停止，再检查状态变化。','count 从 3 开始且每轮减 1，count > 0 何时为假？',['count 为 2','count 为 1','count 为 0'],2,'当 count 变为 0，0 > 0 不成立。')

add("字典：用字段名找数据", "不再靠记住第几列，用字段名描述一条记录。",
 ["字典用大括号包住键和值，键和值之间用冒号，不同字段用逗号分隔。例如 {'label':'猫','score':80} 描述一条记录。", "record['label'] 按键读取值。这里 label 是文字键，所以需要引号；列表则通常按整数索引取值。", "键在同一个字典中唯一。用已经存在的键赋值会更新该字段，而不是增加一个同名字段。不要依赖猜测字段位置。"],
 'record = {"label": "猫", "score": 80} | label 和 score 是两个字段名。\nprint(record["label"]) | 按字段名读取标签。\nrecord["score"] = 90 | 更新已有字段的值。\nprint(record["score"]) | 读取更新后的分数。', '猫\n90',
 task('把更新后的 score 改成 95。','record = {"label": "猫", "score": 80}\nprint(record["label"])\nrecord["score"] = 90\nprint(record["score"])','record = {"label": "猫", "score": 80}\nprint(record["label"])\nrecord["score"] = 95\nprint(record["score"])','猫\n95','修改赋值语句的右侧数字。'),
 task('创建 record，字段 id 为 S01，字段 label 为狗；依次输出两个字段。','# 创建并读取两个字段','record = {"id": "S01", "label": "狗"}\nprint(record["id"])\nprint(record["label"])','S01\n狗','S01 和狗都是文字，都要加引号。'),
 'record[label] 会把 label 当成变量；文字字段名应写 record["label"]。','字典通过什么查找值？',['键','只能用行号','随机位置'],0,'字典是键到值的映射。')

add("缺失字段与 get", "数据不完整时，先明确缺失规则，不让程序突然中断。",
 ["使用不存在的键 record['note'] 会触发 KeyError。record.get('note','未填写') 在键缺失时返回默认值，适合可选字段。", "默认值只用于键不存在的情况。字段存在但值是空字符串时，get 仍返回空字符串，不能把『不存在』和『有字段但为空』混为一谈。", "'label' in record 可以判断某个键是否存在。关键字段缺失应该报告，不要随便填一个值掩盖问题。"],
 'record = {"label": "猫"} | 这条记录没有 note 字段。\nprint(record.get("note", "未填写")) | 键缺失时使用默认文字。\nprint("label" in record) | 检查键是否存在。', '未填写\nTrue',
 task('给 record 增加 note 字段，值为『已审核』，观察 get 的结果。','record = {"label": "猫"}\nprint(record.get("note", "未填写"))\nprint("label" in record)','record = {"label": "猫", "note": "已审核"}\nprint(record.get("note", "未填写"))\nprint("label" in record)','已审核\nTrue','字段存在时返回原值。'),
 task('record = {"id":"S02"}，用 get 读取 label，缺失时显示『待标注』。','record = {"id": "S02"}\n# 安全读取标签','record = {"id": "S02"}\nprint(record.get("label", "待标注"))','待标注','get 的第二个参数是默认值。'),
 'get 不是通用清洗器：字段存在但为空，需要另外判断。','get 的默认值何时使用？',['值为任意假值时','键不存在时','任何情况下'],1,'键存在时，即使值是空字符串，也返回该值。')

add("列表中的字典：一张小表", "把一行记录扩展成多行，理解表格和程序之间的关系。",
 ["一条字典可以表示一行，多个字典放在列表里，就形成一张小表。每行使用相同字段名，才能一致地读取和处理。", "for row in rows 每轮得到一条字典；row['score'] 再从这条字典中取分数字段。这是先选行，再选字段的两步操作。", "先打印每条记录确认结构，再加条件筛选。不要直接把一个嵌套结构当成一个普通分数去比较。"],
 'rows = [{"id": "S01", "score": 50}, {"id": "S02", "score": 80}] | 列表中的每一项都是一条字典记录。\nfor row in rows: | 每轮取得一行记录。\n    if row["score"] >= 60: | 从当前行取分数，再比较。\n        print(row["id"]) | 只显示达线记录的编号。', 'S02',
 task('将 S01 的分数改成 60，让两条记录都显示。','rows = [{"id": "S01", "score": 50}, {"id": "S02", "score": 80}]\nfor row in rows:\n    if row["score"] >= 60:\n        print(row["id"])','rows = [{"id": "S01", "score": 60}, {"id": "S02", "score": 80}]\nfor row in rows:\n    if row["score"] >= 60:\n        print(row["id"])','S01\nS02','只修改第一条记录的 score。'),
 task('遍历两条记录 {label:猫,count:2} 和 {label:狗,count:3}，累加 count 并输出总数。','rows = [{"label": "猫", "count": 2}, {"label": "狗", "count": 3}]\ntotal = 0\n# 遍历并累计字段','rows = [{"label": "猫", "count": 2}, {"label": "狗", "count": 3}]\ntotal = 0\nfor row in rows:\n    total = total + row["count"]\nprint(total)','5','加的是 row["count"]，不是整条 row。'),
 'row 是一条字典，不是分数；需要再取 row["score"]。','for row in rows 后，row 是？',['整张表','当前一条记录','当前字段名'],1,'列表遍历一次取一个元素，这里的元素是字典。')

add("元组与集合：按用途选容器", "认识另外两种常见容器，并知道不该随意互换。",
 ["元组 tuple 常用圆括号表示固定的一组值，例如 (640,480) 表示宽与高。它有顺序、能索引，但不能像列表一样直接替换元素。", "集合 set 用来表达不重复的成员。set(['猫','猫','狗']) 只保留猫和狗。集合不保证你想要的展示顺序，输出时可用 sorted 排序。", "列表适合保留顺序和重复，集合适合成员检查与去重，字典适合字段映射。选择容器取决于业务含义，而不是哪种写起来最短。"],
 'size = (640, 480) | 元组保存一组固定尺寸。\nprint(size[0]) | 元组也能按索引读取。\nlabels = ["cat", "cat", "dog"] | 原始列表允许重复。\nprint(sorted(set(labels))) | 先去重，再排序成稳定的列表输出。', "640\n['cat', 'dog']",
 task('把元组宽度改成 320，其他代码保留。','size = (640, 480)\nprint(size[0])\nlabels = ["cat", "cat", "dog"]\nprint(sorted(set(labels)))','size = (320, 480)\nprint(size[0])\nlabels = ["cat", "cat", "dog"]\nprint(sorted(set(labels)))',"320\n['cat', 'dog']",'创建新元组时改值，不是给 size[0] 赋值。'),
 task('对 [3,1,3,2] 去重，排序后输出。','values = [3, 1, 3, 2]\n# set 去重，sorted 排序','values = [3, 1, 3, 2]\nprint(sorted(set(values)))','[1, 2, 3]','先处理内层 set，再处理外层 sorted。'),
 '不能用 set 保证原始顺序，也不能靠集合判断哪条重复记录该保留。','需要保留顺序和重复次数时优先用？',['列表','集合','随便哪种都一样'],0,'集合会丢掉重复和原始顺序信息。')

add("函数：定义不等于执行", "把固定步骤起一个名字，需要时再调用。",
 ["def 后写函数名和圆括号，行末是冒号。缩进中的代码是函数体。执行 def 只是建立函数，不会立刻运行函数体。", "函数名后加括号才是调用，例如 greet()。调用两次，里面的代码就分别执行两次。定义要先于调用。", "先把两行能跑通的步骤写出来，再包进函数。函数是组织代码的工具，不是为了让代码看起来更复杂。"],
 'def greet(): | 定义一个没有参数的函数。\n    print("开始检查") | 这一行等到函数被调用才执行。\ngreet() | 第一次调用。\ngreet() | 第二次调用。', '开始检查\n开始检查',
 task('只调用一次 greet，保留函数定义。','def greet():\n    print("开始检查")\ngreet()\ngreet()','def greet():\n    print("开始检查")\ngreet()','开始检查','删除一次调用，不要删掉函数体。'),
 task('定义 finish()，调用时显示『完成』；然后调用一次。','# 先定义，再调用','def finish():\n    print("完成")\nfinish()','完成','函数体的 print 要缩进，调用放回最左边。'),
 '只写 def 而没有调用，函数体不会执行。','执行 def 后，函数体什么时候运行？',['定义时立即运行','调用函数时','永远不运行'],1,'定义创建函数，调用执行函数。')

add("参数：同一套步骤处理不同值", "区分函数定义里的占位名字和调用时真正传入的值。",
 ["def show(label) 中的 label 是参数名，用来接收调用者给的值。show('猫') 会把文字猫交给 label。下一次 show('狗') 会接收另一个值。", "两个参数用逗号分隔，按位置对应。调用时数量不对会出现 TypeError。先用有意义的参数名，避免弄错顺序。", "参数让函数不依赖某个固定输入。不要在函数里面又把参数强行改成固定值，那会失去复用的意义。"],
 'def show(label): | 定义一个接收标签的函数。\n    print(f"标签：{label}") | 读取本次调用收到的值。\nshow("猫") | 第一次传入猫。\nshow("狗") | 第二次传入狗。', '标签：猫\n标签：狗',
 task('把第二次调用的参数改成鸟。','def show(label):\n    print(f"标签：{label}")\nshow("猫")\nshow("狗")','def show(label):\n    print(f"标签：{label}")\nshow("猫")\nshow("鸟")','标签：猫\n标签：鸟','改调用位置的值，不改函数里的参数名。'),
 task('定义 show_total(a,b)，在函数内输出 a+b；用 2 和 4 调用。','# 定义两个参数的函数','def show_total(a, b):\n    print(a + b)\nshow_total(2, 4)','6','定义和调用时都使用两个参数。'),
 'show() 没传必需的 label 参数会报错；检查定义和调用是否对应。','show("猫") 中的猫会交给？',['函数的参数 label','所有变量','函数名'],0,'参数用于接收本次调用给定的数据。')

add("return：把结果交回去", "区分『屏幕显示』和『返回给后面的代码』。",
 ["print 是给人看结果；return 是把值交给调用这段函数的代码。返回值可以继续参与计算、保存或输出。", "return 执行后当前函数就结束。如果函数没有 return，调用结果通常是 None，不是最后一条 print 的文字。", "先让函数只负责计算，外面决定是否打印。这能让同一个计算逻辑用于网页、文件和测试，而不强迫所有场景都输出到屏幕。"],
 'def add_two(number): | 接收一个数。\n    return number + 2 | 把计算结果交回调用处。\nresult = add_two(3) | result 接收返回值 5。\nprint(result * 2) | 返回值能继续参与运算。', '10',
 task('调用时传入 4，其余不变。','def add_two(number):\n    return number + 2\nresult = add_two(3)\nprint(result * 2)','def add_two(number):\n    return number + 2\nresult = add_two(4)\nprint(result * 2)','12','先算 4+2，再算乘 2。'),
 task('定义 is_pass(score)，返回 score >= 60 的真假；输出 is_pass(60)。','# 函数返回结果，外面打印','def is_pass(score):\n    return score >= 60\nprint(is_pass(60))','True','return 后面可以直接放比较表达式。'),
 '把 return 写成 print 后，外部再做运算可能得到 NoneType 错误。','要让函数结果用于后续计算，通常使用？',['只用 print','return','注释'],1,'return 把值交回调用者，print 仅显示。')

add("局部变量、默认参数与小测试", "理解函数内部的名字为何不会随便改动外面的变量。",
 ["函数中的参数与普通局部变量属于本次调用。外部同名变量不会因为内部赋值而自动改变。需要传出新结果时使用 return。", "参数可以有默认值，例如 bonus=2。调用时不传它就用默认值，传入则覆盖这次调用的默认选择。初学时不要把可变列表当默认参数。", "给函数准备两个调用例子：一个走默认值，一个显式传值。每次修改函数后重新运行小例子，帮助发现行为变化。"],
 'value = 10 | 这是函数外部的变量。\ndef increase(value, bonus=2): | value 是函数内部的参数，bonus 有默认值。\n    return value + bonus | 返回计算结果，不修改外部 value。\nprint(increase(3)) | 使用默认 bonus=2。\nprint(increase(3, 4)) | 本次传入 bonus=4。\nprint(value) | 外部 value 仍然是 10。', '5\n7\n10',
 task('把默认 bonus 改为 1，保留两次调用和最后输出。','value = 10\ndef increase(value, bonus=2):\n    return value + bonus\nprint(increase(3))\nprint(increase(3, 4))\nprint(value)','value = 10\ndef increase(value, bonus=1):\n    return value + bonus\nprint(increase(3))\nprint(increase(3, 4))\nprint(value)','4\n7\n10','显式传入 4 的第二次调用不会受默认值改变影响。'),
 task('定义 scale(number, factor=2)，返回 number*factor；分别输出 scale(3) 和 scale(3,4)。','# 定义函数并验证两个调用','def scale(number, factor=2):\n    return number * factor\nprint(scale(3))\nprint(scale(3, 4))','6\n12','默认参数只在没有传入该参数时使用。'),
 '不要指望函数内部给参数重新赋值会改掉外部同名变量。','increase(3,4) 会用哪个 bonus？',['默认值 2','显式传入的 4','外部 value'],1,'本次提供的实参优先于默认值。')

add("import：模块与现成工具", "理解导入与调用两步，而不是看到 import 就照抄。",
 ["模块是一组已经写好的功能。import math 导入标准库中的数学模块，math.sqrt(9) 用点号找到里面的平方根函数并调用。", "标准库随 Python 一起提供，通常不需要 pip 安装。第三方包例如 pandas 需要单独安装或由本页运行时加载。模块名和函数名不是同一个东西。", "先查工具的输入和输出约定。例如 sqrt 需要可接受的数值，传入文字不会自动变成数字。没有理解数据类型，导入再多工具也不能解决问题。"],
 'import math | 导入标准库 math。\nroot = math.sqrt(9) | 调用模块内的函数，计算 9 的平方根。\nprint(root) | sqrt 返回小数形式 3.0。', '3.0',
 task('计算 16 的平方根。','import math\nroot = math.sqrt(9)\nprint(root)','import math\nroot = math.sqrt(16)\nprint(root)','4.0','只修改函数接收的数值。'),
 task('使用 math.ceil 对 2.3 向上取整并输出。','import math\n# ceil 表示向上取整','import math\nprint(math.ceil(2.3))','3','用模块名、点号、函数名和括号组合。'),
 '直接写 sqrt(9) 不一定能找到函数；本课使用 math.sqrt(9)。','math 属于？',['标准库模块','个人文件密码','必须付费的服务'],0,'math 是随 Python 提供的标准库模块。')

add("try/except：处理可预期的错误", "识别哪种数据会失败，再给使用者可理解的结果。",
 ["try 中放可能失败的操作，except ValueError 只处理值转换不合适的情况。正常成功时，不会执行 except 分支。", "错误处理不是把所有错误都吞掉。只捕获你知道如何处理的错误，保留其他问题的定位信息。不要用一个空 except 假装程序成功了。", "本例规定非整数字符串输出『无效数字』。这是明确的数据规则；实际项目还要决定保留原值、记录原因还是拒绝本条数据。"],
 'raw = "abc" | 这不是可转换的整数字符串。\ntry: | 开始尝试可能失败的操作。\n    value = int(raw) | 此处触发 ValueError。\n    print(value) | 转换失败时不会执行到这一行。\nexcept ValueError: | 只接住值转换错误。\n    print("无效数字") | 给出可理解的结果。', '无效数字',
 task('把 raw 改为 "12"，观察成功路径。','raw = "abc"\ntry:\n    value = int(raw)\n    print(value)\nexcept ValueError:\n    print("无效数字")','raw = "12"\ntry:\n    value = int(raw)\n    print(value)\nexcept ValueError:\n    print("无效数字")','12','成功转换后直接输出，不进入 except。'),
 task('raw = "2x"，尝试 float 转换并输出；失败时输出『请检查输入』。','raw = "2x"\n# 捕获 ValueError','raw = "2x"\ntry:\n    print(float(raw))\nexcept ValueError:\n    print("请检查输入")','请检查输入','float 也可能因为值不合适而触发 ValueError。'),
 'except Exception: pass 会隐藏原因；本课要输出明确的失败提示。','int("abc") 应重点检查什么？',['变量名大小写','原始值是否为数字文字','网络是否连接'],1,'它是值转换错误，不是网络错误。')

add("assert 与边界测试", "用可重复的小检查发现错误，不只看一次输出。",
 ["assert 条件 表示开发时希望这个条件为真；为假会触发 AssertionError。断言适合学习和测试，不应替代生产系统的身份验证或用户输入校验。", "定义达线函数后，测试 59、60、61 三个值，分别覆盖边界下方、边界本身、边界上方。只测试 80 容易漏掉 > 和 >= 的错误。", "程序不报错不代表业务正确。断言是在说明『我期待什么』，失败时应检查规则或实现，而不是简单删除断言。"],
 'def is_pass(score): | 建立待测试的函数。\n    return score >= 60 | 本例约定包含 60。\nassert is_pass(59) == False | 检查边界下方。\nassert is_pass(60) == True | 检查边界本身。\nprint("边界检查通过") | 只有前面断言通过才会显示。', '边界检查通过',
 task('修复错误的 >，让两个断言都通过。','def is_pass(score):\n    return score > 60\nassert is_pass(59) == False\nassert is_pass(60) == True\nprint("边界检查通过")','def is_pass(score):\n    return score >= 60\nassert is_pass(59) == False\nassert is_pass(60) == True\nprint("边界检查通过")','边界检查通过','60 必须被包含，不要删除测试。'),
 task('定义 double(x) 返回 x*2，断言 double(0)==0 和 double(3)==6，最后输出『测试通过』。','# 定义函数，再写断言','def double(x):\n    return x * 2\nassert double(0) == 0\nassert double(3) == 6\nprint("测试通过")','测试通过','两个 assert 分别覆盖零和普通正数。'),
 '为了消除报错删掉断言，会失去验证；先检查真实规则和代码差异。','对 >=60 的规则，哪个值最容易发现误写 > 的问题？',['90','60','100'],1,'只有正好 60 能直接区分这两种比较。')

add("解释器、pip 与虚拟环境", "理解本页与本机 Python 的差别，不把安装命令写进程序。",
 ["解释器运行 .py 文件；pip 安装第三方包；虚拟环境为一个项目保存自己的解释器入口和依赖。三者不是同一个东西。本页使用浏览器里的 Python，与电脑上的 Python 分开。", "本机终端可执行 python --version 查看版本，用 python -m venv .venv 创建环境，再用该环境的 python -m pip install pandas 安装包。这些是终端命令，不能写进本页 Python 编辑区执行。", "Windows 也可直接使用 .venv\\Scripts\\python.exe，无需修改全局 PATH。先确认你运行的是哪一个解释器，再排查『明明安装了却找不到包』。本课不执行安装，只观察 sys 提供的解释器信息。"],
 'import sys | sys 是查询解释器信息的标准库。\nprint(sys.version_info.major) | 读取主版本号；本平台使用 Python 3。', '3',
 task('给主版本号加上说明，输出『Python 3』。','import sys\nprint(sys.version_info.major)','import sys\nprint("Python", sys.version_info.major)','Python 3','用 print 的两个参数显示说明和数字。'),
 task('检查解释器主版本是否等于 3，输出真假。','import sys\n# 比较 major 与 3','import sys\nprint(sys.version_info.major == 3)','True','version_info.major 是整数，可以和 3 比较。'),
 '把 pip install pandas 写进 Python 编辑区会报语法错误；安装命令在系统终端执行。','python -m pip install pandas 应写在哪里？',['Python 字符串里','系统终端','CSV 单元格'],1,'pip 安装命令由终端启动，和 Python 源代码不同。')

add("路径与写文件", "理解文件名、保存位置、编码和关闭文件，先写一个很小的结果。",
 ["文件路径告诉程序去哪里找文件。result.txt 是相对路径，相对于程序当前工作目录；它不是自动指向桌面。练习页在临时虚拟目录里运行。", "with open(..., 'w', encoding='utf-8') 打开文本文件写入，with 块结束会自动关闭。'w' 会覆盖同名文件；真实项目要先备份，避免覆盖原始数据。", "f.write 写入文字，不会自动补换行。字符串中的 \\n 表示一个换行字符。网页运行结果中的文件可下载，关闭或重置运行器后不要指望临时文件还在。"],
 'with open("result.txt", "w", encoding="utf-8") as file: | 用 UTF-8 创建或覆盖这个练习文件。\n    file.write("已检查\\n") | 写入文字及一个换行。\nprint("已保存") | 离开 with 块，文件已经关闭。', '已保存',
 task('把文件内容改为『待复核』，成功后仍显示『已保存』。','with open("result.txt", "w", encoding="utf-8") as file:\n    file.write("已检查\\n")\nprint("已保存")','with open("result.txt", "w", encoding="utf-8") as file:\n    file.write("待复核\\n")\nprint("已保存")','已保存','修改 write 的文字，不修改最后的提示。'),
 task('创建 note.txt，写入『学习记录』及换行，最后显示『记录完成』。','# 用 with 打开文件并写入','with open("note.txt", "w", encoding="utf-8") as file:\n    file.write("学习记录\\n")\nprint("记录完成")','记录完成','文件名是 note.txt，模式是 w，编码为 utf-8。'),
 '真实文件上使用 w 会覆盖旧内容；练习页使用独立临时目录，不要在本机随意覆盖原文件。','相对路径 result.txt 相对于什么？',['总是桌面','当前工作目录','浏览器收藏夹'],1,'相对路径以程序当前工作目录为起点。')
LESSONS[-1]['guided']['files']={'result.txt':'待复核\n'}
LESSONS[-1]['independent']['files']={'note.txt':'学习记录\n'}

add("读文件与行尾换行", "看清文件里的内容和显示出来的结果为何可能不同。",
 ["用 'r' 读取已有文件，read() 一次拿到全部文字。文件不存在会触发 FileNotFoundError；先检查文件名、路径和是否已经写入。", "read() 会保留文件中的换行。print 默认也补一个换行，因此直接 print(text) 有时会多出空白行。strip 可去两端空白，但不能在需要保留排版时随意使用。", "本页每次运行会重置练习目录，因此示例先创建小文件再读取，不依赖上一课有没有运行。实际处理中应保留原始文件，输出到新文件。"],
 'with open("note.txt", "w", encoding="utf-8") as file: | 为本次练习准备文件。\n    file.write("第一行\\n第二行\\n") | 写入两行文字。\nwith open("note.txt", "r", encoding="utf-8") as file: | 重新以只读方式打开。\n    text = file.read() | 读取全文，包含换行字符。\nprint(text.strip()) | 去掉末尾换行后再显示，避免重复空行。', '第一行\n第二行',
 task('将第二行内容改为『结束』，保持读取过程。','with open("note.txt", "w", encoding="utf-8") as file:\n    file.write("第一行\\n第二行\\n")\nwith open("note.txt", "r", encoding="utf-8") as file:\n    text = file.read()\nprint(text.strip())','with open("note.txt", "w", encoding="utf-8") as file:\n    file.write("第一行\\n结束\\n")\nwith open("note.txt", "r", encoding="utf-8") as file:\n    text = file.read()\nprint(text.strip())','第一行\n结束','先改写入内容，再用原读取步骤验证。'),
 task('写入 count.txt 内容为文字 12，再读出并转为整数，加 1 后输出。','# 写入、读取、转换、计算','with open("count.txt", "w", encoding="utf-8") as file:\n    file.write("12")\nwith open("count.txt", "r", encoding="utf-8") as file:\n    raw = file.read()\nprint(int(raw) + 1)','13','read 返回文字，需要 int 转换后才能加 1。'),
 'FileNotFoundError：先确认当前目录和文件名，不要立即重新安装 Python。','file.read() 读取文本后通常得到？',['字符串','自动生成的整数列表','数据库'],0,'文本读取返回字符串，后续由你决定如何解析。')

add("CSV：表头、行与字段类型", "理解逗号分隔文本如何成为可处理的记录。",
 ["CSV 用文本保存表格，常见格式是第一行字段名、后面每行一条记录。字段中如果含逗号或引号，需要正确转义，因此不要用简单 split 处理任意真实 CSV。", "csv.DictReader 根据表头创建字典记录，每个字段初始通常是字符串。要比较分数，先 int(row['score'])，不要直接把文字与数字比较。", "课程提供 scores.csv 小文件，运行时自动放入练习目录；可在资料区查看原始内容并下载。真实项目读取时明确 encoding 和 newline，并核对行数、字段和缺失值。"],
 'import csv | 导入 CSV 标准库。\nwith open("scores.csv", encoding="utf-8", newline="") as file: | 打开课程提供的模拟数据。\n    rows = list(csv.DictReader(file)) | 按表头读取，转成列表供后面遍历。\nfor row in rows: | 每轮得到一条字典。\n    if int(row["score"]) >= 60: | 先把文字分数转成整数，再判断。\n        print(row["id"]) | 输出达线记录编号。', 'S02\nS03',
 task('把筛选阈值改为 80，只输出 >=80 的编号。','import csv\nwith open("scores.csv", encoding="utf-8", newline="") as file:\n    rows = list(csv.DictReader(file))\nfor row in rows:\n    if int(row["score"]) >= 60:\n        print(row["id"])','import csv\nwith open("scores.csv", encoding="utf-8", newline="") as file:\n    rows = list(csv.DictReader(file))\nfor row in rows:\n    if int(row["score"]) >= 80:\n        print(row["id"])','S03','课程数据中 S03 的分数是 80。'),
 task('读取同一个 scores.csv，累计全部 score，输出总分。','import csv\ntotal = 0\n# 读取并累加','import csv\ntotal = 0\nwith open("scores.csv", encoding="utf-8", newline="") as file:\n    for row in csv.DictReader(file):\n        total = total + int(row["score"])\nprint(total)','190','三个分数分别为 50、60、80，先转换再相加。'),
 'CSV 看起来是数字不代表已是 int；先检查字段类型。','DictReader 用哪一行确定字段名（未显式指定时）？',['最后一行','第一行表头','随机一行'],1,'默认使用第一行作为键名，后面的行是记录。')

add("JSON：保存结构化结果", "把字典保存成可交换的文本，再读取回来。",
 ["JSON 是一种数据格式，不是 Python 代码。json.dumps 把 Python 数据变成 JSON 字符串；json.loads 把 JSON 字符串解析回 Python 数据。", "JSON 的键使用双引号，布尔值写 true/false，空值写 null；Python 对应 True/False 和 None。交给 json 模块转换，不要手工替换字符。", "中文保存时可设置 ensure_ascii=False，便于阅读。只使用可信来源并核对字段；不要用 eval 读取数据，因为 eval 会执行代码。"],
 'import json | 导入 JSON 标准库。\nreport = {"label": "猫", "count": 2} | 准备一个字典。\ntext = json.dumps(report, ensure_ascii=False) | 转成可读的 JSON 文本。\nrestored = json.loads(text) | 解析回 Python 字典。\nprint(restored["label"]) | 按字段取值。\nprint(restored["count"]) | 数字字段仍是数字。', '猫\n2',
 task('把 report 的 count 改成 4，再做同样的往返转换。','import json\nreport = {"label": "猫", "count": 2}\ntext = json.dumps(report, ensure_ascii=False)\nrestored = json.loads(text)\nprint(restored["label"])\nprint(restored["count"])','import json\nreport = {"label": "猫", "count": 4}\ntext = json.dumps(report, ensure_ascii=False)\nrestored = json.loads(text)\nprint(restored["label"])\nprint(restored["count"])','猫\n4','转换函数不变，只改输入数据。'),
 task('解析 JSON 文本 {"passed": true, "count": 3}，依次输出 passed 和 count。','import json\ntext = \'{"passed": true, "count": 3}\'\n# loads 解析，再读取字段','import json\ntext = \'{"passed": true, "count": 3}\'\nresult = json.loads(text)\nprint(result["passed"])\nprint(result["count"])','True\n3','JSON 的 true 会解析为 Python 的 True。'),
 'json.loads 需要 JSON 文本，不能直接把字典再次传进去。','读取 JSON 数据应该优先用？',['eval','json.loads','直接执行文本'],1,'json.loads 解析数据，eval 会执行表达式。')

add("NumPy：数组与形状", "在列表基础上理解数组，不直接跳到模型训练。",
 ["NumPy 是处理数值数组的第三方库，常写 import numpy as np，np 是自己给模块取的简短别名。本页首次使用会额外下载库，讲解不依赖下载。", "np.array([1,2,3]) 创建一维数组。对数组乘 2 会逐个元素计算；而 Python 列表乘 2 是重复列表，两者不能混淆。", "shape 描述每个维度的长度。一维三项数组的 shape 是 (3,)，逗号表示只有一个元素的元组。先看清行、列和形状，才能理解训练数据。"],
 'import numpy as np | 导入库，并约定使用 np 别名。\nvalues = np.array([1, 2, 3]) | 创建一维数组。\nprint((values * 2).tolist()) | 逐元素乘 2，再转成列表方便显示。\nprint(values.shape) | 查询形状，不修改数组。', '[2, 4, 6]\n(3,)',
 task('将数组每个元素乘以 3，其余保持不变。','import numpy as np\nvalues = np.array([1, 2, 3])\nprint((values * 2).tolist())\nprint(values.shape)','import numpy as np\nvalues = np.array([1, 2, 3])\nprint((values * 3).tolist())\nprint(values.shape)','[3, 6, 9]\n(3,)','只修改乘数，不修改数组本身。'),
 task('创建二维数组 [[1,2],[3,4]]，输出它的 shape，再输出第二行第一列的值。','import numpy as np\n# 创建数组，检查形状和位置','import numpy as np\nvalues = np.array([[1, 2], [3, 4]])\nprint(values.shape)\nprint(values[1, 0])','(2, 2)\n3','二维索引依次是行、列，都从 0 开始。'),
 'ModuleNotFoundError 可能是运行环境没装包；先确认解释器，再安装对应环境的依赖。','NumPy 数组乘 2 通常表示？',['逐元素乘 2','复制文件两次','仅改变形状'],0,'数值数组支持逐元素算术，与列表重复不同。')

add("pandas：从记录到 DataFrame", "把已学的列表、字典和条件筛选连接到表格工具。",
 ["DataFrame 可以理解为带列名和行索引的表格。pd.DataFrame(字典列表) 把原来的一行行记录组织成表。pandas 常取别名 pd。", "df['score'] 选择一列；df['score'] >= 60 得到每行是否满足条件的结果；df[条件] 再选出对应行。这三步可以先拆开观察。", "不要把 DataFrame 当成一个分数。先检查 shape 的行列数，再看字段名和类型。表格工具不会自动理解业务里的异常值。"],
 'import pandas as pd | 导入 pandas。\ndf = pd.DataFrame([{"id": "S01", "score": 50}, {"id": "S02", "score": 80}]) | 从两条记录创建表格。\nprint(df.shape) | 两行两列。\nselected = df[df["score"] >= 60] | 先比较一列，再按真假筛选行。\nprint(selected["id"].tolist()) | 把选中的编号列转成列表显示。', "(2, 2)\n['S02']",
 task('将筛选阈值改成 50，让两条记录都被选中。','import pandas as pd\ndf = pd.DataFrame([{"id": "S01", "score": 50}, {"id": "S02", "score": 80}])\nprint(df.shape)\nselected = df[df["score"] >= 60]\nprint(selected["id"].tolist())','import pandas as pd\ndf = pd.DataFrame([{"id": "S01", "score": 50}, {"id": "S02", "score": 80}])\nprint(df.shape)\nselected = df[df["score"] >= 50]\nprint(selected["id"].tolist())',"(2, 2)\n['S01', 'S02']",'大于等于 50 包括两条记录。'),
 task('用 pandas 读取课程 scores.csv，只输出 score 列的列表。','import pandas as pd\n# read_csv 读取，再选择一列','import pandas as pd\ndf = pd.read_csv("scores.csv")\nprint(df["score"].tolist())','[50, 60, 80]','pd.read_csv 接收文件名，df["score"] 取列。'),
 'df[0] 通常是在找名为 0 的列，不是读取第一行；按行与按列要分清。','df.shape 的两个数通常依次表示？',['列数、行数','行数、列数','最小值、最大值'],1,'DataFrame 形状按行、列排列。')

add("缺失值：先发现，再决定", "补值不是随意填零，先说明规则和影响。",
 ["缺失值表示没有有效记录，不等于真实数值 0。isna 可以标记缺失，sum 可以统计缺失数量。先报告问题，再决定如何处理。", "本课模拟质量分数据，约定只统计已有分数，因此用 dropna 删除缺失项并计算均值。这只是练习规则，不适用于所有业务。", "实际任务要记录删除了多少条、为什么删除，以及是否改变样本分布。训练模型时，补值参数只从训练集学习，不能偷看测试集。"],
 'import pandas as pd | 使用 pandas 处理缺失。\nvalues = pd.Series([60, None, 80]) | None 表示这条记录没有分数。\nprint(int(values.isna().sum())) | 统计一个缺失值。\nclean = values.dropna() | 本课规则：仅保留已有分数。\nprint(float(clean.mean())) | 两个有效分数的平均值是 70。', '1\n70.0',
 task('将有效分数改为 50 和 90，中间仍为 None，保持处理规则。','import pandas as pd\nvalues = pd.Series([60, None, 80])\nprint(int(values.isna().sum()))\nclean = values.dropna()\nprint(float(clean.mean()))','import pandas as pd\nvalues = pd.Series([50, None, 90])\nprint(int(values.isna().sum()))\nclean = values.dropna()\nprint(float(clean.mean()))','1\n70.0','先排除缺失，再对 50 与 90 求均值。'),
 task('values = pd.Series([None, 2, None, 4])，输出缺失数，再输出有效值数量。','import pandas as pd\nvalues = pd.Series([None, 2, None, 4])\n# 两个统计','import pandas as pd\nvalues = pd.Series([None, 2, None, 4])\nprint(int(values.isna().sum()))\nprint(len(values.dropna()))','2\n2','isna 统计缺失，dropna 后用 len 统计剩余。'),
 '把缺失分数填成 0 会改变均值，也改变其业务含义；先确认规则。','缺失分数是否必然等于零分？',['是','不是，需要业务规则','可以不检查'],1,'未记录与真实零分是不同状态。')

add("分组统计：从明细到汇总", "先说明按什么分组、统计什么，再写 groupby。",
 ["groupby('label') 按标签把记录放到不同组。再对 count 列求 sum，得到每个标签的数量合计。分组键和汇总字段要分别确定。", "例如 cat 有 2 与 1 两条记录，dog 有 3 一条记录；按标签汇总后 cat=3、dog=3。先手算这个小例子，再写程序。", "汇总后不要丢失原始数据。真实报告应说明统计口径，例如是在清洗前还是清洗后统计，重复记录是否已排除。"],
 'import pandas as pd | 导入表格工具。\ndf = pd.DataFrame({"label": ["cat", "dog", "cat"], "count": [2, 3, 1]}) | 三条明细记录。\nsummary = df.groupby("label")["count"].sum() | 按标签分组，对数量求和。\nprint(int(summary["cat"])) | cat 两条记录合计 3。\nprint(int(summary["dog"])) | dog 一条记录数量为 3。', '3\n3',
 task('把最后一条 cat 的 count 改为 4，再汇总。','import pandas as pd\ndf = pd.DataFrame({"label": ["cat", "dog", "cat"], "count": [2, 3, 1]})\nsummary = df.groupby("label")["count"].sum()\nprint(int(summary["cat"]))\nprint(int(summary["dog"]))','import pandas as pd\ndf = pd.DataFrame({"label": ["cat", "dog", "cat"], "count": [2, 3, 4]})\nsummary = df.groupby("label")["count"].sum()\nprint(int(summary["cat"]))\nprint(int(summary["dog"]))','6\n3','变化只影响 cat 组。'),
 task('读取 scores.csv，按 group 分组计算 score 均值；依次输出 A 组和 B 组均值。','import pandas as pd\n# 读取、分组、求均值','import pandas as pd\ndf = pd.read_csv("scores.csv")\nsummary = df.groupby("group")["score"].mean()\nprint(float(summary["A"]))\nprint(float(summary["B"]))','55.0\n80.0','A 组是 50 和 60，B 组是 80。'),
 'groupby 后的 size 是记录条数，sum 是值的总和，两者不能随意替换。','按标签汇总数量，应明确哪两件事？',['字体与颜色','分组键与汇总字段','电脑品牌与型号'],1,'先确定按什么分，再确定每组算什么。')

add("项目一：规范标签文本", "把字符串、循环与列表连接成一个小清洗流程。",
 ["这次不引入新语法，目标是把原始标签整理成一致格式。规则：去掉两端空格，英文转小写，跳过空文字。lower 返回小写的新字符串。", "先保留 raw 原始列表，创建新的 clean 列表。每轮先得到整理后的 label，再判断是否为空，最后添加。这样每一步都能解释。", "不要在不知道业务规则时随意合并类别。这里 CAT 与 cat 约定是同一标签；真实任务的大小写、同义词和未知类别应写入标注规范。"],
 'raw = [" Cat ", "", "DOG"] | 模拟不一致的原始标签。\nclean = [] | 新建结果列表，保留原始数据。\nfor text in raw: | 每轮处理一个字符串。\n    label = text.strip().lower() | 先去两端空格，再转小写。\n    if label != "": | 空字符串不进入结果。\n        clean.append(label) | 保留一个有效标签。\nprint(clean) | 输出清洗后的标签列表。', "['cat', 'dog']",
 task('在原始列表中增加 " Bird "，按原规则清理。','raw = [" Cat ", "", "DOG"]\nclean = []\nfor text in raw:\n    label = text.strip().lower()\n    if label != "":\n        clean.append(label)\nprint(clean)','raw = [" Cat ", "", "DOG", " Bird "]\nclean = []\nfor text in raw:\n    label = text.strip().lower()\n    if label != "":\n        clean.append(label)\nprint(clean)',"['cat', 'dog', 'bird']",'只扩充输入数据，处理规则保持一致。'),
 task('独立整理 [" YES ", "no", "  "]：去两端空格、转小写、去空值，输出结果。','raw = [" YES ", "no", "  "]\n# 原始数据不要修改','raw = [" YES ", "no", "  "]\nclean = []\nfor text in raw:\n    label = text.strip().lower()\n    if label != "":\n        clean.append(label)\nprint(clean)',"['yes', 'no']",'只有空格的字符串 strip 后会变成空字符串。'),
 '检查空值应放在 strip 之后，否则只包含空格的字段会被保留。','原始值 "  " 在 strip 后是什么？',['两个空格','空字符串','数字零'],1,'两端空白全部去除后，剩下空字符串。')

add("项目二：按编号去重并保留顺序", "先定义重复规则，再决定保留哪一条。",
 ["重复不一定是整行完全相同。本课按 id 判断同一条记录，重复编号只保留第一次出现的记录，并保持原顺序。", "seen 集合保存已经见过的编号，clean 列表保存完整记录。读一条数据，先查编号是否见过，没有才添加到两个容器。", "如果同一编号的内容冲突，简单保留第一条不一定合理。本课规则只是练习；真实清洗要记录冲突与处理依据，不可悄悄丢掉数据。"],
 'rows = [{"id": "S01"}, {"id": "S01"}, {"id": "S02"}] | 模拟重复编号。\nseen = set() | 空集合保存已处理编号，不能写成空字典 {}。\nclean = [] | 按顺序保存保留下来的记录。\nfor row in rows: | 依次查看一条记录。\n    if row["id"] not in seen: | 只处理首次出现的编号。\n        seen.add(row["id"]) | 在集合中登记这个编号。\n        clean.append(row) | 保存完整记录。\nprint(len(clean)) | 最后保留两条。', '2',
 task('把最后一条也改为 S01，观察保留条数。','rows = [{"id": "S01"}, {"id": "S01"}, {"id": "S02"}]\nseen = set()\nclean = []\nfor row in rows:\n    if row["id"] not in seen:\n        seen.add(row["id"])\n        clean.append(row)\nprint(len(clean))','rows = [{"id": "S01"}, {"id": "S01"}, {"id": "S01"}]\nseen = set()\nclean = []\nfor row in rows:\n    if row["id"] not in seen:\n        seen.add(row["id"])\n        clean.append(row)\nprint(len(clean))','1','所有编号相同时，只有第一次会被添加。'),
 task('对编号列表 ["B","A","B","C"] 去重，保留首次出现的顺序，输出列表。','ids = ["B", "A", "B", "C"]\n# 使用 seen 和 clean','ids = ["B", "A", "B", "C"]\nseen = set()\nclean = []\nfor value in ids:\n    if value not in seen:\n        seen.add(value)\n        clean.append(value)\nprint(clean)',"['B', 'A', 'C']",'不要用 sorted，它会改变原始顺序。'),
 '直接 set(rows) 会因字典不可哈希而出错；按明确的编号字段判断重复。','本课对重复编号保留哪条？',['最后一条','首次出现的记录','随机一条'],1,'规则明确为保留第一次，并保留原顺序。')

add("项目三：计算预测正确率", "先逐条对照真实标签和预测标签，再理解指标公式。",
 ["真实标签 truth 是参照答案，预测标签 prediction 是模型给出的结果。准确率等于对应位置判断正确的数量除以总数量；它不是考试通过概率。", "zip(truth,prediction) 把两列按位置配成一对，for actual,guess 会分别取出这一对中的两个值。这叫解包。长度不同会丢掉多余部分，所以先断言两列长度一致。", "准确率不能单独说明所有问题。类别很不平衡时，一直猜多数类也可能显得很准。学完本课再进入实验室学习混淆矩阵、精确率和召回率。"],
 'truth = ["cat", "dog", "cat", "dog"] | 四个真实标签。\nprediction = ["cat", "cat", "cat", "dog"] | 四个预测标签。\nassert len(truth) == len(prediction) | 避免标签对不齐。\ncorrect = 0 | 初始化正确计数。\nfor actual, guess in zip(truth, prediction): | 每轮取出同一位置的真实值和预测值。\n    if actual == guess: | 检查这条预测是否正确。\n        correct = correct + 1 | 正确时计数加 1。\nprint(f"{correct / len(truth):.2f}") | 三条正确，除以四条总数。', '0.75',
 task('把第二条预测改为 dog，使四条都正确。','truth = ["cat", "dog", "cat", "dog"]\nprediction = ["cat", "cat", "cat", "dog"]\nassert len(truth) == len(prediction)\ncorrect = 0\nfor actual, guess in zip(truth, prediction):\n    if actual == guess:\n        correct = correct + 1\nprint(f"{correct / len(truth):.2f}")','truth = ["cat", "dog", "cat", "dog"]\nprediction = ["cat", "dog", "cat", "dog"]\nassert len(truth) == len(prediction)\ncorrect = 0\nfor actual, guess in zip(truth, prediction):\n    if actual == guess:\n        correct = correct + 1\nprint(f"{correct / len(truth):.2f}")','1.00','只改预测列表第二项。'),
 task('truth=[1,0,1,0]，prediction=[1,1,0,0]，输出正确条数，再输出保留两位小数的准确率。','truth = [1, 0, 1, 0]\nprediction = [1, 1, 0, 0]\n# 对齐、计数、除以总数','truth = [1, 0, 1, 0]\nprediction = [1, 1, 0, 0]\nassert len(truth) == len(prediction)\ncorrect = 0\nfor actual, guess in zip(truth, prediction):\n    if actual == guess:\n        correct = correct + 1\nprint(correct)\nprint(f"{correct / len(truth):.2f}")','2\n0.50','第 1 与第 4 条预测正确；总数是 4。'),
 '不要把两个列表分别排序后再比较，那会破坏样本一一对应的关系。','准确率的分母通常是什么？',['正确条数','总样本数','类别名称长度'],1,'准确率是正确条数除以被评估的总样本数。')

add("项目四：划分数据与学习复盘", "用最后一个小任务检查自己能否解释输入、处理、输出和风险。",
 ["训练数据用于学习，测试数据用于最后检查。两者不能出现同一条样本，测试集也不应参与选择规则和调参。先在编号层面检查是否重叠。", "本课只演示确定性的切片划分：前四条训练，后两条测试。它便于理解和复现，并不代表真实任务都该按顺序切。真实数据要考虑随机分层、时间顺序、同源对象分组等要求。", "完成后回看四个项目，尝试不看答案重新写一次，并更换输入。结果对照只证明当前例子的输出匹配，不等于掌握全部 Python 或保证考试通过。下一步才进入真实数据和模型实验。"],
 'ids = ["S01", "S02", "S03", "S04", "S05", "S06"] | 每条样本有唯一编号。\ntrain = ids[:4] | 前四条作为训练示例。\ntest = ids[4:] | 剩下两条作为测试示例。\nassert set(train).isdisjoint(set(test)) | isdisjoint 检查两个集合没有共同成员。\nprint(len(train)) | 训练记录数。\nprint(len(test)) | 测试记录数。', '4\n2',
 task('改成前三条训练，后三条测试，仍检查没有交集。','ids = ["S01", "S02", "S03", "S04", "S05", "S06"]\ntrain = ids[:4]\ntest = ids[4:]\nassert set(train).isdisjoint(set(test))\nprint(len(train))\nprint(len(test))','ids = ["S01", "S02", "S03", "S04", "S05", "S06"]\ntrain = ids[:3]\ntest = ids[3:]\nassert set(train).isdisjoint(set(test))\nprint(len(train))\nprint(len(test))','3\n3','两个切片使用同一个切分位置。'),
 task('ids=[1,2,3,4,5]，前 3 条放 train，剩余放 test；断言无交集，依次输出两个列表。','ids = [1, 2, 3, 4, 5]\n# 划分并检查','ids = [1, 2, 3, 4, 5]\ntrain = ids[:3]\ntest = ids[3:]\nassert set(train).isdisjoint(set(test))\nprint(train)\nprint(test)','[1, 2, 3]\n[4, 5]','train 不含位置 3，test 从位置 3 开始。'),
 '前后两段有重叠会造成数据泄漏；真实任务还要检查同一对象的关联样本。','测试集应主要用于？',['反复调参直到分数好看','最后评估未见数据表现','复制到训练集扩大数据'],1,'测试集保留用于最后的独立评估。')
