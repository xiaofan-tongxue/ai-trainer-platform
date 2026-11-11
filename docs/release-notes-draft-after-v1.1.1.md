# 发行说明草案：v1.1.1 之后的下一版

状态：草案。本文不创建 git tag，不发布 GitHub Release，也不附带安装包。版本号由维护者在 Windows 上按 [构建手册](deployment/windows-build-runbook.md) 打出 zip、核对 SHA-256 之后决定。

## 基准

| 项 | 值 |
|---|---|
| 上一已发布版本 | [v1.1.1](https://github.com/xiaofan-tongxue/ai-trainer-platform/releases/tag/v1.1.1) |
| v1.1.1 源码 | `5aa8d60`，发布于 2026-09-22 |
| 本草案描述的源码 | main，merge commit `98c1d8c65536763d1f665de0667d88681cf98da8`（已合并 PR #1） |
| 题库解析叠加 | `data/question-explanations.json` 的 `version` 为 `2026-10-26` |
| 实操指引叠加 | `data/practical-guidance.json` 的 `version` 为 `2026-11-11` |
| 技能成绩排除规则 | `heuristic-v5-practical-conflicts` |

v1.1.1 的 zip 约 798 MB，不含 2026-10-26 的课程、解析和隔离规则，也不含 2026-11-11 的实操修订。旧安装包不会随源码自动更新。

## 相对 v1.1.1 的学习内容

- 课程保持 24 节概览、20 节衔接课、48 节 Python 小课。Python 学习标记按账号保存，可从上次位置继续。
- 原 900 道理论题里，种子 `analysis` 为空的 300 道已由 `data/question-explanations.json` 叠加解析。解析按题型、题干、选项和原答案的指纹匹配。原答案保留。
- 这 300 份解析的生成方式记为 `DeepSeek assisted; schema checked; selected editorial review; original key retained`。151 道 `examEligible` 为 false，`reviewStatus` 为「待复核，暂停评估」，只作辨析，不进入新模拟卷，不形成新的错题判定，不计入领域掌握度。另外 149 道多选题仍可抽入模拟，`reviewStatus` 为「AI 辅助解析，待专家复核」。
- 种子中原有解析的 600 道题（编号 1–600）仍是可抽题。这些解析都很短，尚未逐题专家复核。范围见 [理论解析人工复核清单](2026-11-11-theory-explanation-review.md)。
- 40 项实操都有分步指引。2026-11-11 只恢复了题面、笔记本和参考脚本已经一致的两题：2.1.5（`2026-11-11-age-interval`）和 2.2.3（`2026-11-11-age-interval-classification`）。2.1.2 与 3.2.4 继续排除在技能模拟之外。没有本次修订标记的 2.1.5、2.2.3 旧卷不计入能力评估。
- 浏览器 Python 练习在主 CDN 失败时会尝试备用 CDN。单位网络若拦截外部 CDN，仍需改用手机热点，或在环境终端运行已下载的代码。
- `requirements-labs.txt` 随应用目录提供。包内 Python 含 pip，不含全部机器学习库。安装这些库需要联网。参考脚本此前在 Python 3.10 上验证，尚未对包内 Python 3.13 的全部依赖组合复验。

可抽理论题为 749 道：600 道短解析加 149 道仍可抽的 AI 多选。这是抽题资格，不是官方审定题量。

## 安装包运行环境

沿用现有打包脚本，不更换运行环境组合：

- Temurin JDK 21，编译产物为 Java 8 字节码
- MySQL 8.4.11 独立实例，优先端口 13306
- Connector/J 8.4.0
- Python 3.13.15 与 pip
- 微软 VC++ 运行库，仅在系统缺少时由安装器调起

网站优先 19001。首次安装生成随机管理员密码，账号为 admin，首次登录必须改密。DeepSeek API Key 由管理员在后台填写，不放进安装包。

## 已有数据

全新安装才会导入去掉默认账号后的种子，并生成新的 `data-key.dpapi`。

已有安装禁止重新导入 `seed.sql`，禁止覆盖 `data-key.dpapi`。新包的「一键安装」发现完整实例后只检查并启动，不替换已存在的 `app` 目录。要把新程序和资料放进旧实例，按构建手册备份 `app\runtime`、替换 `app`、放回原来的 `runtime`，并核对密钥哈希。不要把旧电脑的 `runtime`、学员库或 API Key 打进发布 zip。

## 使用边界

题库是公开备考练习，不是四川省科教兴川的历次真题。通过可能性仍是未经考后样本校准的规则估计。课程学完、自测完成、脚本运行成功或 AI 评分，都不表示正式考试合格。公网发布仍需 TLS、边界防护和管理员多因素认证。本草案不宣称已经通过等级保护测评。

## 发布前仍要在 Windows 上完成的事

1. `git lfs pull`，并核对 `materials/3.2.1/resnet.onnx` 与 `materials/3.2.4/flower-detection.onnx` 的 SHA-256。
2. 准备 `build/oneclick-cache` 中的 Java、MySQL、Connector/J、Python 和 VC++ 文件。缓存来自官方安装包，不使用本机业务库。
3. 运行 `deploy/windows/build-package.ps1`，得到 `dist/AITrainer-Windows-OneClick.zip` 和同名 `.sha256`。
4. 按构建手册抽查 zip：没有 runtime、没有 DPAPI、没有默认账号插入、没有 API Key、没有 LFS 指针。
5. 在空目录做全新安装，运行 `test-lifecycle.ps1` 和 `test-package.ps1`。新的检查计数以这两次输出为准。
6. 核对 151 道理论题、2.1.2 和 3.2.4 仍不进入模拟。
7. 由维护者创建 tag 并上传 Release。zip 不进入 git 仓库。
