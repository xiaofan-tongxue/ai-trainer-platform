# 安全部署、迁移与验收手册

## 1. 当前电脑

当前数据库已完成field-v1加密迁移，运行凭据已切换独立DML账号，密钥由初始化时的Windows账号通过DPAPI读取。不要重新初始化数据库、重新生成主密钥或还原旧代码覆盖新版。运行 `build.ps1` 后用 `run.ps1` 或 `start.bat` 启动；服务已有实例时无需重复启动。

访问 `http://localhost:19001/login`。已有账号需在首次登录后更新密码。上线前由运营确认管理员新密码、停用不需要的演示账号；不要继续分发旧默认凭据。密码变更、角色调整、禁用和服务重启使相关会话失效。

默认只监听127.0.0.1:19001。当前浏览器到本机服务为HTTP，不具备公网TLS保护。浏览器草稿改为当前标签页会话保存，退出或关闭后可能丢失，重要代码应主动保存到自己的受控文件中。

## 2. 新空库或未加固实例迁移

适用前提：已取得维护窗口、停止应用写入、完成完整数据库备份，并确认恢复凭据/密钥。当前项目无需重复执行。`db/init.ps1`只允许空库初始化，已有表会中止。

```powershell
# 仅新空数据库：由安全终端/密码管理器注入 MYSQL_ROOT_PASSWORD 后执行
.\db\init.ps1

# 所有未加固实例：维护身份通过受控方式注入 DB_PASSWORD
.\security-init.ps1
.\build.ps1
.\migrate-security.ps1
.\provision-database.ps1
.\run.ps1
```

security-init创建受限DPAPI密钥库和加密应用备份；迁移工具先调用业务表迁移，再转换敏感字段并核验备份。迁移脚本要求应用停止；运行账号不会自动执行DDL。provision-database创建随机账号，仅授予目标数据库SELECT/INSERT/UPDATE/DELETE，实际验证建表被拒绝；凭据不输出。运行账号配置已存在时不会重复创建。迁移后禁止再次导入原seed.sql。

迁移DDL不是整体事务，中断后保留现场与密钥，检查报错并用同一版本幂等重试。跨主机恢复必须先解决DPAPI恢复身份或正式导入KMS密钥，不能只复制runtime/security目录就假定可读。

## 3. 配置

`.env.example`仅为说明，应用不自动加载.env。机密通过受控服务配置或密钥管理器注入；生产不在命令行参数中传密码，避免进程列表泄露。

| 配置 | 值/用途 |
|---|---|
| APP_ENV | `production`启用生产检查及Secure Cookie/HSTS |
| PUBLIC_ORIGIN | 精确HTTPS源，例如`https://learn.example.org`，不带末尾斜杠；应与Host/Origin一致 |
| TRUSTED_PROXY_IP | 反向代理实际连接源IP，单一明确地址；同机模板为127.0.0.1 |
| DB_URL | JDBC地址；跨主机连接必须配置TLS和证书验证 |
| DB_USER/DB_PASSWORD | 环境变量优先；本机缺省从DPAPI运行凭据读取 |
| DATA_ENCRYPTION_KEY | Base64编码32字节主密钥；缺省读取data-key.dpapi，不能随意替换 |
| DEEPSEEK_API_KEY/MODEL/API_URL | 服务端提供方配置；已有管理后台Key密文保存 |
| -Dport / -Dbind.address | 默认19001/127.0.0.1；后端仅对可信代理开放 |
| -Dsecurity.dir | 密钥与加密审计目录，默认runtime/security |

维护凭据目前保存在db-admin-password.dpapi，和运行密钥仍在同一Windows身份下，生产应迁至独立运维身份或离线受控保管。不要把runtime、build日志、备份、.env或.dpapi/.enc文件放入静态根目录和发布包。

## 4. 生产边界部署

参考 [Nginx模板](../../deploy/nginx-security.conf)。模板需在网关http上下文中引入，替换域名、证书和日志路径；示例后端为同机127.0.0.1:19001。实际网关配置先用`nginx -t`验证，证书加载、TLS协议和限流以生产实测为准，本次未安装或启动Nginx。

上线步骤：

1. 关闭公网访问作为默认状态，部署有效证书和TLS1.2/1.3；设置PUBLIC_ORIGIN和production模式。
2. 使后端19001只接受网关路径；3306不向互联网暴露；远程DB使用独立网络和验证证书的TLS连接。
3. 网关覆盖X-Real-IP，不信任客户端伪造值；应用TRUSTED_PROXY_IP仅填写真实代理地址。多跳代理需单独设计信任链。
4. 部署WAF、主机防护、集中日志与告警；网关日志目录位于受保护加密存储，明确六个月留存；模板本身不等于WAF产品已部署。
5. 管理入口接入MFA与受限来源；管理员、系统维护和审计身份分离。完成旧账号、组件升级及其他高优先级整改。
6. 配置异地备份，验证全部数据库、素材和密钥的恢复。完成生产资产范围的授权扫描与业务回归后开放业务。

## 5. 验证命令与证据

```powershell
# 服务运行时：业务回归，临时用户自动清理
.\test.ps1 --integration

# 独立临时MySQL库：加密迁移与恢复，使用维护凭据
.\security-test.ps1

# test.ps1 已编译测试类，服务运行时执行本地安全HTTP复测
& 'C:\Program Files\Java\jdk-24\bin\java.exe' -cp 'build/classes;lib/mysql-connector-java-5.1.37-bin.jar' com.aitrainer.service.SecurityHttpTest

# 加密审计链检查：输出文件名和末端认证值，不打印日志内容或密钥
& 'C:\Program Files\Java\jdk-24\bin\java.exe' -cp 'build/classes;lib/mysql-connector-java-5.1.37-bin.jar' com.aitrainer.security.SecurityAudit
```

HTTP测试固定为本机HTTP开发入口，不能直接证明生产Secure Cookie、TLS、WAF、真实代理和Origin部署正确。生产测试应使用独立测试账号和实际HTTPS域名，逐项验证：未认证401、越权403、缺CSRF拒绝、方法405、限流429、错误无堆栈、公开配置不含密钥、Cookie具备Secure/HttpOnly/SameSite、数据库DDL拒绝、日志可归档和数据可恢复。

## 6. 历史备份与回退

`protect-legacy-backups.ps1`只创建并验证加密副本，保留原件；同名加密副本已存在时停止，防止覆盖。两份历史副本验证记录位于runtime/security/legacy-backup-verification.json。当前原件清理等待明确确认。

回退前停止写入并备份当前状态；在隔离库将匹配版本的逻辑备份解密恢复，验证用户/答卷/权限后再安排切换。当前迁移备份覆盖13张用户与学习表，整站回退还需完整公共题库、素材和原库结构备份。禁止仅还原旧源代码连接已加密的新库。

密钥不可读时核对Windows身份和密钥路径，不要“重置生成新key”；迁移标记缺失时停止启动并检查维护日志；审计写失败优先检查磁盘容量和ACL；生产421检查PUBLIC_ORIGIN/Host，403检查Origin/CSRF及真实代理配置。不能为排障关闭身份校验或把运行账号改回root。
