# Windows 一键部署包

适用Windows10/11 64位。将整个ZIP解压到可写目录，建议路径简短、磁盘至少空余8GB。双击 **一键安装.bat**，等待校验、环境安装、建库、加密初始化及启动完成。通常不需要管理员权限；系统缺少VC++运行库时会显示Windows管理员确认。

包内包含Java21 JDK、MySQL8.4 LTS独立实例、Connector/J8.4、Python3.13及pip、课程/题库/素材和预编译应用。运行环境下载来源及SHA-256在manifest.json登记。首次安装不需要下载Java、数据库或Python。网页内Python练习的Pyodide首次加载和DeepSeek仍需要互联网。

安装目录自动选择公共用户目录中的 `AITrainer-当前用户SID`（通常在C:\Users\Public下），目录权限限制为安装用户和SYSTEM；这样避免MySQL中文路径问题。解压位置可以使用中文。配置只对本程序进程及“环境终端”生效，不修改全局PATH，不覆盖系统已有MySQL/Python。网站优先19001端口，数据库优先13306，冲突时自动选择后续可用端口，实际地址会显示并自动打开。

- **一键安装.bat**：首次安装；完整安装再次执行会检查并启动，不重新导入数据。
- **启动平台.bat**：重启电脑后使用；启动独立数据库和网站。
- **停止平台.bat**：停止本实例网站，正常关闭数据库，保留数据。
- **环境检查.bat**：显示运行环境、端口及安装阶段。
- **查看初始账号.bat**：读取本Windows账号加密保管的初始管理员密码。改密后初始密码不再有效。
- **环境终端.bat**：在配置好java、javac、python、pip、mysql路径的终端中工作。

第一次安装会随机生成管理员初始密码，账号为admin，首次登录必须改密。数据库密码和数据密钥自动生成并以DPAPI保存。DeepSeek API Key需要你在管理后台填写一次；该凭据不能自动购买或生成。

此包用于全新部署，包含900理论题、40实操任务和完整学习资料，不包含原电脑的学员账号、答卷、API Key或数据库文件。不要把现有runtime/security直接复制到另一电脑充当迁移；已有学员数据迁移需要匹配的数据和密钥恢复流程。

Python运行文件来自已验证数字签名的官方完整安装程序，部署时独立解压，包含标准库、SSL、SQLite、Tk和pip，未自动安装机器学习第三方包。需扩展时在环境终端运行 `python -m pip install 包名`，下载依赖需要联网。无需卸载现有Python；环境终端自动配置本实例的解释器和工具路径。

下文“instance”表示上述实际安装目录，启动界面会显示该路径。安装状态记录于instance/installation.json。失败不会删除数据目录或覆盖已有数据库；查看database.err.log/application.err.log，修正后重新运行入口。若种子导入阶段中断，会保留现场并要求人工核查，避免重复导入污染数据。不要删除标记、修改端口或覆盖密钥强行重装。

停止平台后可备份整个instance至加密存储；因DPAPI绑定安装Windows身份，异机恢复必须先验证恢复方案。不要移动运行中的数据库目录。部署包为本机回环HTTP用途；公网发布还需TLS、域名、边界防护及安全报告中尚未关闭的项目。

官方来源：[Temurin](https://adoptium.net/temurin/releases/?version=21)、[MySQL8.4](https://dev.mysql.com/downloads/mysql/8.4.html)、[Connector/J](https://dev.mysql.com/downloads/connector/j/)、[Python Windows](https://www.python.org/downloads/windows/)、[微软VC++运行库](https://learn.microsoft.com/en-us/cpp/windows/latest-supported-vc-redist)。第三方许可保留在原始运行环境档案/安装程序中，使用与再分发应遵守各组件许可。
