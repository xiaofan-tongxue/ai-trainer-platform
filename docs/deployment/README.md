# 一键部署验收记录

日期：2026-09-15。对象：Windows64位全新部署包。现有MySQL5.7数据库未迁移或覆盖；测试创建了两套独立MySQL8.4实例。

交付位置：`dist/AITrainer-Windows-OneClick.zip`。使用说明见 [Windows部署指南](../../deploy/windows/README.md)。部署包包含运行环境和教学内容，排除原学员、真实API Key、DPAPI密钥库及数据库文件。

| 项目 | 结果 |
|---|---|
| 运行环境 | Temurin JDK21、MySQL8.4.11、Connector/J8.4.0、Python3.13.15及pip26.2.1 |
| Windows自带PowerShell5.1 | 实际安装执行通过，不要求另装PowerShell7 |
| 空目录完整安装 | 第二套实例一次完整执行通过，自动初始化独立数据库与加密配置 |
| 端口占用 | 第二套实例自动选择网站19002、数据库13307，第一套19001/13306保持独立 |
| 业务回归 | 74/74通过，含课程、考试、答案保存、交卷与素材下载 |
| HTTP安全回归 | 62/62通过，含鉴权、CSRF、越权、加密字段与账户锁定 |
| 初始账号 | 随机管理员密码；首次登录强制更新；无默认demo账号 |
| 初始化内容 | 900理论题、40实操任务，管理员统计接口实际核验 |
| 重复安装 | 已改密码、数据主密钥保持不变，不重新导入种子 |
| 停止/重启 | 应用停止、MySQL正常关闭、重启就绪与改密后登录验证通过 |
| Python | 随包解压的完整运行文件，SSL/SQLite导入及pip版本检查通过 |
| 前端 | 12个脚本语法与课程资源链接检查通过 |

验证中发现并修复：MySQL中文运行目录不兼容（默认自动采用英文安装目录）；PowerShell原生IP参数解析；PowerShell5.1输入流API差异；重复设置目录ACL；Python命令引用；Connector/J8的DATETIME返回LocalDateTime，统一转换为既有时间格式。

原始证据：[业务输出](evidence/PlatformTest.txt)、[HTTP输出](evidence/SecurityHttpTest.txt)、[HTTP结果](evidence/http-security-results.json)、[环境记录](evidence/verification.json)、[安装生命周期记录](evidence/lifecycle.json)。测试临时账号与业务记录已清理；验收实例停止后保留以供检查，未混入发布包。

范围：已在当前Windows主机隔离目录验证，并非真实空白虚拟机验证。系统自带PowerShell、.NET、Windows基础组件仍是前提；缺少VC++运行库时安装器提供已签名运行库并触发系统确认，该缺组件分支未在卸载本机运行库的环境下模拟。浏览器Pyodide首次加载及DeepSeek服务需要网络；未宣称整站所有功能完全离线。生产公网部署仍需TLS、边界防护、管理员MFA等整改。
