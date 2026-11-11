# Windows 一键包构建手册

本文说明如何在 Windows 上，用现有 `deploy/windows/build-package.ps1` 从当前 main 打出下一版 `AITrainer-Windows-OneClick.zip`。本仓库的 Linux 工作区不能完成安装程序数字签名校验、`javac` 和离线运行环境缓存，因此这里没有生成该 zip，也没有创建 tag 或 GitHub Release。

上一份已发布的包是 [v1.1.1](https://github.com/xiaofan-tongxue/ai-trainer-platform/releases/tag/v1.1.1)，由 commit `5aa8d60` 在 2026-09-22 构建。当前 main 已包含合并的 PR #1，merge commit 为 `98c1d8c65536763d1f665de0667d88681cf98da8`。发行说明草案见 [v1.1.1 之后](../release-notes-draft-after-v1.1.1.md)。

## 构建前

使用 Windows 10/11 64 位和系统自带的 Windows PowerShell 5.1。磁盘建议至少再留 8 GB 给安装实例，构建目录另计。安装 Git 与 Git LFS。JDK 24 放在 `C:\Program Files\Java\jdk-24\`，或者保证 `javac` 在 PATH 中。脚本与 `build.ps1` 使用同一套查找顺序，编译目标仍是 Java 8 字节码，类路径只用 Connector/J 8.4。

```powershell
git clone https://github.com/xiaofan-tongxue/ai-trainer-platform.git
cd ai-trainer-platform
git checkout main
git pull
git lfs install
git lfs pull
git rev-parse HEAD
```

`git rev-parse HEAD` 应为 `98c1d8c65536763d1f665de0667d88681cf98da8`，或该提交之后的 main。提交号若不同，发行说明里的基准要改成实际值。

五个 `.onnx` 必须是 Git LFS 展开后的模型文件。下面两个文件的 SHA-256 都应为 `af16a04a6ec48ac494065d4439fe9dea590d337b9ca6dc328160ccf04a217b9c`。这是 `docs/2026-10-26-content-quality.md` 记录的摘要。摘要不一致就停止，不要为了打包通过而改题，也不要解除 3.2.4 的隔离。

```powershell
Get-FileHash -Algorithm SHA256 materials\3.2.1\resnet.onnx, materials\3.2.4\flower-detection.onnx
python tools\audit-readiness.py
```

`audit-readiness.py` 应没有 `errors`。警告中应有 `300 AI-assisted explanations; 151 unresolved items excluded from assessment`，以及 `2 practical tasks excluded`。

## 运行环境缓存

缓存不进 git，目录是 `build/oneclick-cache/`。不要把开发机已安装的 MySQL 数据目录、`runtime/`、已有学习平台实例或学员库拷进缓存。`python.zip` 必须是从已验签的官方安装包独立解压出来的运行文件，并已把 pip 入口改成相对路径。

| 文件 | 脚本怎样核对 |
|---|---|
| `java.zip` | SHA-256 必须等于 `java-source.json` 的 `checksum`。该 JSON 还要有 `name` 和 `link` |
| `java-source.json` | 见上一行 |
| `mysql.zip` | 官方 `mysql-8.4.11-winx64.zip`。MD5 必须等于 `2E833921898A9A030EA6BFE81BD811BC` |
| `connector.zip` | 官方 Connector/J 8.4.0 压缩包，内含 `mysql-connector-j-8.4.0.jar` |
| `python-installer.exe` | `https://www.python.org/ftp/python/3.13.15/python-3.13.15-amd64.exe`，Authenticode 状态为 Valid。此文件不进入最终 zip |
| `python.zip` | 由上一行安装包解压得到。脚本不重新解压安装包，只复用这份缓存 |
| `vc-redist.exe` | `https://aka.ms/vc14/vc_redist.x64.exe`，Authenticode 状态为 Valid，并复制进 payload |

来源页面仍以 [一键部署指南](../../deploy/windows/README.md) 列出的 Temurin、MySQL 8.4、Connector/J、Python Windows 和 VC++ 运行库为准。

## 构建命令

在仓库根目录执行：

```powershell
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File .\deploy\windows\build-package.ps1
```

脚本会清空并重建 `build/oneclick-app` 和 `dist/AITrainer-Windows-OneClick`。它只删除这两个构建目录，不删除 `build/oneclick-cache`，也不接触 `C:\Users\Public\AITrainer-*` 里的已安装实例。接着它会：

- 从 `db/seed.sql` 去掉默认账号插入，再写入待打包的 app。仓库里的 `db/seed.sql` 仍保留那段插入，供源码对照；进入 zip 的副本必须没有 `INSERT INTO users`。
- 拒绝 `runtime`、`.dpapi`、`.enc`、私钥扩展名、`.env`、用户插入、`sk-` 后接 20 位以上令牌字符的文本、`C:\Users\` 下除 Public 和 Default 以外的个人目录，以及未展开的 Git LFS 指针。
- 编译 `src/main/java`。
- 写出 `manifest.json`。其中 `sourceCommit` 是 `git rev-parse HEAD`，`questionExplanations` 来自 `data/question-explanations.json`，`practicalGuidance` 来自 `data/practical-guidance.json`，每个 payload 文件带字节数和 SHA-256。

只准备目录、不打最外层 zip 时：

```powershell
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File .\deploy\windows\build-package.ps1 -SkipArchive
```

## 期望产物

目录 `dist/AITrainer-Windows-OneClick/` 包含：

- `一键安装.bat`、`启动平台.bat`、`停止平台.bat`、`环境检查.bat`、`环境终端.bat`、`查看初始账号.bat`
- `launcher.ps1`
- `README.md`
- `manifest.json`
- `payload/app.zip`
- `payload/java.zip`
- `payload/mysql.zip`
- `payload/python.zip`
- `payload/vc-redist.exe`

最外层还有：

- `dist/AITrainer-Windows-OneClick.zip`
- `dist/AITrainer-Windows-OneClick.zip.sha256`（一行 SHA-256，ASCII）

控制台打印一行 `SHA256` 加摘要，以及一行 `source` 加 40 位提交号。

`manifest.json` 中 `questionExplanations` 为 `2026-10-26`，`practicalGuidance` 为 `2026-11-11`，`mysql` 为 `8.4.11`，`python` 为 `3.13.15`，`connector` 为 `8.4.0`，`freshInstallOnly` 为 true，`files` 恰好 5 项。用下面的命令复核摘要：

```powershell
Get-FileHash -Algorithm SHA256 dist\AITrainer-Windows-OneClick\payload\*
Get-Content dist\AITrainer-Windows-OneClick\manifest.json -Raw
Get-FileHash -Algorithm SHA256 dist\AITrainer-Windows-OneClick.zip
Get-Content dist\AITrainer-Windows-OneClick.zip.sha256
```

最外层 zip 的 SHA-256 必须与 `.sha256` 文件和工作台输出一致。v1.1.1 的 zip 大约 798 MB，那是旧包的体积。本次以新文件的长度和摘要为准，不要把 798 MB 写成预期值。

最外层 zip 的条目以 `AITrainer-Windows-OneClick/` 为前缀，因为脚本打包时包含该目录名。

## zip 里不得出现的内容

- `runtime/`，任何 `.dpapi`（包括 `data-key.dpapi`），`.enc`，私钥，`.env`
- 打包后 `db/seed.sql` 中的 `INSERT INTO users`
- DeepSeek API Key，或文本里的 `sk-` 长令牌
- 个人 Windows 目录。安装说明中的 `C:\Users\Public` 可以保留
- 本机已安装实例、学员账号、答卷、日志和备份
- `python-installer.exe`。它只留在缓存中供验签
- Git LFS 指针文本，开头是 `version https://git-lfs.github.com/spec/v1`
- `.git`、`build/oneclick-cache`，以及把 `dist` 再套进 zip

抽查：

```powershell
tar -tf dist\AITrainer-Windows-OneClick.zip | Select-String -Pattern 'runtime|\.dpapi|python-installer|oneclick-cache|\.env|data-key'
```

这条没有匹配才继续。再解出 `payload/app.zip`，确认其中的 `db/seed.sql` 没有 `INSERT INTO users`，并且存在 `data/question-explanations.json`、`data/practical-guidance.json`、`data/bridge-course.json`、`requirements-labs.txt`、`build/classes` 和 `lib/connector.jar`。`practical-guidance.json` 里 2.1.2 与 3.2.4 的 `examEligible` 仍为 false。

## 全新安装

把 zip 解压到可写目录，双击 `一键安装.bat`。默认实例在 `C:\Users\Public\AITrainer-<当前用户SID>`。首次安装导入去掉默认账号后的种子，并新建 `data-key.dpapi`。同一 Windows 账号要安装第二套全新实例时，用 PowerShell 调用 `launcher.ps1 -Action Install -InstallDir <另一段英文路径>`。不要把 `-InstallDir` 指到已有实例。

## 已有安装

已有实例再次执行新包里的 `一键安装.bat` 时，`launcher.ps1` 看到 `installation.json` 且阶段为 `complete` 之后：不重新导入 `seed.sql`，不覆盖已有 `data-key.dpapi`，并且因为 `app` 目录已存在，不会解压新的 `app.zip`。再次双击安装不会把 2026-10-26 和 2026-11-11 的资料写进旧实例。

替换已有实例的程序和资料时，先双击「停止平台」，再执行：

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$root = 'C:\Users\Public\AITrainer-S-1-5-21-换成实际SID'
$packageApp = 'D:\解压后的包\payload\app.zip'
$key = Join-Path $root 'app\runtime\security\data-key.dpapi'
$before = (Get-FileHash -Algorithm SHA256 -LiteralPath $key).Hash
$runtimeBackup = Join-Path $env:TEMP ('AITrainer-runtime-' + [guid]::NewGuid().ToString('n'))
Copy-Item -LiteralPath (Join-Path $root 'app\runtime') -Destination $runtimeBackup -Recurse
Rename-Item -LiteralPath (Join-Path $root 'app') -NewName 'app.previous'
$staging = Join-Path $env:TEMP ('AITrainer-app-' + [guid]::NewGuid().ToString('n'))
New-Item -ItemType Directory -Force -Path $staging | Out-Null
[IO.Compression.ZipFile]::ExtractToDirectory($packageApp, $staging)
if (Test-Path -LiteralPath (Join-Path $staging 'runtime')) { throw '新 app.zip 含 runtime，停止' }
Move-Item -LiteralPath $staging -Destination (Join-Path $root 'app')
New-Item -ItemType Directory -Force -Path (Join-Path $root 'app\runtime') | Out-Null
Copy-Item -LiteralPath (Join-Path $runtimeBackup '*') -Destination (Join-Path $root 'app\runtime') -Recurse -Force
$after = (Get-FileHash -Algorithm SHA256 -LiteralPath $key).Hash
if ($before -ne $after) { throw 'data-key.dpapi 已变化，停止并恢复 app.previous' }
```

然后用原来的「启动平台」启动。不要执行 `schema.sql` 或 `seed.sql`。确认仍能用改过的管理员密码登录，原有作答还在。确认后再删除 `app.previous`。`app.previous` 里还有旧的 `runtime`，删除前先确认新位置的密钥哈希未变，并且不要把这份目录打进发布 zip。

`installation.json` 的 `version` 是安装器状态结构编号，当前为 1，不是产品发行号。

## 安装后核对

在全新临时实例上核对，不要对着已有学员库运行。

```powershell
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File .\deploy\windows\test-lifecycle.ps1 -InstanceDir 'C:\Users\Public\AITrainer-<测试SID>'
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File .\deploy\windows\test-package.ps1 -InstanceDir 'C:\Users\Public\AITrainer-<测试SID>'
python tools\audit-readiness.py
```

`test-lifecycle.ps1` 要求全新库为 900 道理论题、40 道实操、1 个随机密码管理员，并检查重复安装不改变 `data-key.dpapi`。

`test-package.ps1` 从测试进程最后一行读取计数。通过时，两行应分别是 `ALL <数字> CHECKS PASSED` 和 `ALL <数字> HTTP SECURITY CHECKS PASSED; fixtures removed`。它会重写 `docs/deployment/evidence/verification.json`。只有这次 Windows 运行通过之后，才把新证据提交进仓库。[2026-09-15 验收记录](README.md) 里的 74 和 62 属于 v1.1.1，不要在未运行测试时改掉。

内容上，151 道理论题以及实操 2.1.2、3.2.4 仍不进入模拟。2.1.5、2.2.3 可以进入，标记分别为 `2026-11-11-age-interval` 和 `2026-11-11-age-interval-classification`。

## 发布

不要把 zip 提交进 git。`.gitignore` 已忽略 `*.zip` 和 `dist/`。摘要核对完成后，由维护者另行决定 v1.1.1 之后的 tag，并发布 GitHub Release。本手册不创建 tag，也不指定最终版本号。

题库按公开备考练习使用，不写成四川省科教兴川的历次真题。课程学完、自测完成或 AI 评分，都不表示正式考试合格。
