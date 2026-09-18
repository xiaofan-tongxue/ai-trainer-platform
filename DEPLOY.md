# 部署与安全升级

新电脑使用[Windows一键部署包](dist/AITrainer-Windows-OneClick.zip)，解压后运行“一键安装.bat”。包内包含运行环境，初始化独立数据库及随机管理员密码，自动完成安全迁移和环境配置。见[操作指南](deploy/windows/README.md)。新部署使用MySQL8.4及Connector/J8.4；下文现有电脑的MySQL5.7实例保持原状。

当前电脑已完成敏感字段加密迁移和最小权限数据库配置。使用初始化密钥库时的Windows账号启动；运行 `build.ps1`、`run.ps1`，或使用 `start.bat`。默认访问 http://localhost:19001/login，旧账号首次登录须更新密码。

**不要重新导入seed.sql，不要覆盖data-key.dpapi，不要用旧版代码直接连接加密后的数据库。** 安全版本启动不自动执行DDL；异机复制DPAPI文件也不保证可恢复。

- [安全部署、迁移、生产配置与恢复手册](docs/security/06-deployment.md)
- [安全评估与等保支撑材料](docs/security/README.md)
- [生产Nginx配置模板](deploy/nginx-security.conf)
- [学习功能升级说明](docs/UPGRADE.md)

`test.ps1 --integration`验证业务；`security-test.ps1`在隔离数据库验证安全迁移与恢复。测试使用临时记录并清理，不调用付费AI接口。环境变量示例文件仅作说明，程序不自动读取.env。

当前服务仅本机HTTP，尚未部署生产TLS/WAF。MySQL5.7及Connector/J5.1.37仍待独立环境升级验证。历史备份已生成两份验证通过的加密副本，原件删除待确认。其余上线门槛与责任划分见安全报告。
