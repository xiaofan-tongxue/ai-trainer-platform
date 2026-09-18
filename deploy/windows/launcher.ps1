param(
 [ValidateSet('Install','Start','Stop','Check','Shell','Credentials')][string]$Action='Install',
 [string]$InstallDir=(Join-Path $env:PUBLIC ('AITrainer-'+[Security.Principal.WindowsIdentity]::GetCurrent().User.Value)),
 [switch]$NoBrowser,[switch]$HideCredentials
)
$ErrorActionPreference='Stop'
$ProgressPreference='SilentlyContinue'
[Console]::OutputEncoding=[Text.UTF8Encoding]::new($false)
Add-Type -AssemblyName System.Security
Add-Type -AssemblyName System.IO.Compression.FileSystem
$root=[IO.Path]::GetFullPath($InstallDir)
if($root -match '[^\x00-\x7F]'){throw 'MySQL运行目录必须使用英文路径。默认安装会自动选择公共用户目录下的独立受限位置。'}
$stateFile=Join-Path $root 'installation.json'
$vault=Join-Path $root 'app/runtime/security'
$originalEnvironment=@{}
foreach($name in @('JAVA_HOME','PYTHONHOME','PATH','DB_URL','DB_USER','DB_PASSWORD','MYSQL_PWD','DATA_ENCRYPTION_KEY','SETUP_ADMIN_PASSWORD','AITRAINER_FRESH_INSTALL','APP_ENV','PUBLIC_ORIGIN','TRUSTED_PROXY_IP','DEEPSEEK_API_KEY')){$originalEnvironment[$name]=[Environment]::GetEnvironmentVariable($name,'Process')}
function Set-PrivateDirectory([string]$path){
 New-Item -ItemType Directory -Path $path -Force|Out-Null
 $current=Get-Acl -LiteralPath $path
 $allowed=@([Security.Principal.WindowsIdentity]::GetCurrent().User.Value,'S-1-5-18')
 $rules=@($current.GetAccessRules($true,$true,[Security.Principal.SecurityIdentifier]))
 if($current.AreAccessRulesProtected -and $rules.Count -ge 2 -and @($rules|Where-Object {$_.IdentityReference.Value -notin $allowed}).Count -eq 0){return}
 $acl=New-Object Security.AccessControl.DirectorySecurity
 $acl.SetAccessRuleProtection($true,$false)
 foreach($sid in @([Security.Principal.WindowsIdentity]::GetCurrent().User,[Security.Principal.SecurityIdentifier]::new('S-1-5-18'))){$acl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new($sid,'FullControl','ContainerInherit,ObjectInherit','None','Allow'))}
 Set-Acl -LiteralPath $path -AclObject $acl
}
function Random-Secret {
 $bytes=New-Object byte[] 32;$rng=[Security.Cryptography.RandomNumberGenerator]::Create();$rng.GetBytes($bytes);$rng.Dispose()
 return ([Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+','-').Replace('/','_')+'9a')
}
function Save-Secret([string]$name,[string]$value){
 $file=Join-Path $vault ($name+'.dpapi')
 $sealed=[Security.Cryptography.ProtectedData]::Protect([Text.Encoding]::UTF8.GetBytes($value),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
 [IO.File]::WriteAllBytes($file,$sealed)
}
function Read-Secret([string]$name){return [Text.Encoding]::UTF8.GetString([Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes((Join-Path $vault ($name+'.dpapi'))),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser))}
function Write-Text([string]$file,[string]$value){[IO.File]::WriteAllText($file,$value,[Text.UTF8Encoding]::new($false))}
function Free-Port([int]$first){
 for($candidate=$first;$candidate -lt ($first+100);$candidate++){
  $listener=[Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,$candidate)
  try{$listener.Start();return $candidate}catch{}finally{$listener.Stop()}
 };throw '没有可用端口'
}
function Expand-Runtime([string]$archive,[string]$target,[string]$expected){
 if(Test-Path (Join-Path $target $expected)){return}
 $staging=$target+'.extract'
 if(Test-Path $staging){throw "检测到未完成解压，请保留现场并检查：$staging"}
 New-Item -ItemType Directory -Force -Path $staging|Out-Null
 [IO.Compression.ZipFile]::ExtractToDirectory($archive,$staging)
  $base=Get-ChildItem $staging -Directory|Where-Object {Test-Path (Join-Path $_.FullName $expected)}|Select-Object -First 1
  if(-not $base){throw '运行环境压缩包结构不匹配'}
  if(-not [IO.Path]::GetFullPath($target).StartsWith($root+'\',[StringComparison]::OrdinalIgnoreCase) -or -not $base.FullName.StartsWith([IO.Path]::GetFullPath($staging)+'\',[StringComparison]::OrdinalIgnoreCase)){throw '运行环境目标目录越界'}
 Move-Item -LiteralPath $base.FullName -Destination $target
 Remove-Item -LiteralPath $staging
}
function Apply-Environment {
 $env:JAVA_HOME=Join-Path $root 'java';$env:PYTHONHOME=Join-Path $root 'python'
 $env:PATH="$root\java\bin;$root\python;$root\python\Scripts;$root\mysql\bin;"+$originalEnvironment['PATH']
 $env:DB_URL="jdbc:mysql://127.0.0.1:$($state.dbPort)/ai_trainer_platform?useUnicode=true&characterEncoding=utf8&sslMode=DISABLED&allowPublicKeyRetrieval=true&connectionTimeZone=Asia/Shanghai"
 foreach($name in @('DB_USER','DB_PASSWORD','DATA_ENCRYPTION_KEY','APP_ENV','PUBLIC_ORIGIN','TRUSTED_PROXY_IP','DEEPSEEK_API_KEY','MYSQL_PWD')){[Environment]::SetEnvironmentVariable($name,$null,'Process')}
}
function Owned-Process([string]$kind){
 $file=Join-Path $root ($kind+'.pid')
 if(-not(Test-Path $file)){return $null}
 $value=Get-Content $file -Raw
 if($value -notmatch '^\s*\d+\s*$'){throw '进程记录损坏'}
 $process=Get-CimInstance Win32_Process -Filter "ProcessId=$([int]$value)"
 if(-not $process){return $null}
 $expected=if($kind -eq 'database'){Join-Path $root 'mysql/bin/mysqld.exe'}else{Join-Path $root 'java/bin/java.exe'}
 if($process.ExecutablePath -ne $expected){throw 'PID已被其他进程占用，拒绝操作'}
 if($kind -eq 'application' -and $process.CommandLine -notmatch 'com\.aitrainer\.Main'){throw '应用进程身份不匹配'}
 return $process
}
function Start-Database {
 if(Owned-Process 'database'){return}
 $args='--defaults-file="'+(Join-Path $root 'mysql.ini')+'"'
 $init=Join-Path $vault 'mysql-bootstrap.sql'
 if(Test-Path $init){$args+=' --init-file="'+$init+'"'}
 $process=Start-Process -FilePath (Join-Path $root 'mysql/bin/mysqld.exe') -ArgumentList $args -WindowStyle Hidden -WorkingDirectory $root -RedirectStandardOutput (Join-Path $root 'database.out.log') -RedirectStandardError (Join-Path $root 'database.err.log') -PassThru
 $process.Id|Set-Content (Join-Path $root 'database.pid')
 $ready=$false;$env:MYSQL_PWD=Read-Secret 'db-admin-password'
 try{
  for($i=0;$i -lt 60;$i++){
   if($process.HasExited){throw '数据库退出；请查看database.err.log。数据目录保留。'}
   $previousAction=$ErrorActionPreference
   try{$ErrorActionPreference='Continue';& "$root/mysql/bin/mysql.exe" --protocol=TCP '--host=127.0.0.1' "--port=$($state.dbPort)" -uroot --connect-timeout=2 --batch --skip-column-names -e 'SELECT 1' 2>$null|Out-Null;$queryCode=$LASTEXITCODE}finally{$ErrorActionPreference=$previousAction}
   if($queryCode -eq 0){$ready=$true;break};Start-Sleep -Milliseconds 500
  }
 }finally{$env:MYSQL_PWD=$null}
 if(-not $ready){throw '数据库启动超时，查看日志后重试'}
 if(Test-Path $init){Remove-Item -LiteralPath $init}
}
function Import-Sql([string]$file){
 $env:MYSQL_PWD=Read-Secret 'db-admin-password'
 try{
  $info=[Diagnostics.ProcessStartInfo]::new();$info.FileName=Join-Path $root 'mysql/bin/mysql.exe'
  $info.Arguments="--protocol=TCP -h127.0.0.1 --port=$($state.dbPort) -uroot --default-character-set=utf8mb4"
  $info.UseShellExecute=$false;$info.CreateNoWindow=$true;$info.RedirectStandardInput=$true
  $process=[Diagnostics.Process]::Start($info)
  $sqlBytes=[Text.Encoding]::UTF8.GetBytes([IO.File]::ReadAllText($file,[Text.Encoding]::UTF8));$process.StandardInput.BaseStream.Write($sqlBytes,0,$sqlBytes.Length);$process.StandardInput.Close();$process.WaitForExit()
  if($process.ExitCode -ne 0){throw '数据导入失败，原目录保留，不自动覆盖重建'}
 }finally{$env:MYSQL_PWD=$null}
}
function Start-Application {
 Start-Database
 if(-not(Owned-Process 'application')){
  $javaArgs='-Dfile.encoding=UTF-8 -Dport='+$state.httpPort+' -cp "build/classes;lib/connector.jar" com.aitrainer.Main'
  $p=Start-Process -FilePath (Join-Path $root 'java/bin/java.exe') -ArgumentList $javaArgs -WorkingDirectory (Join-Path $root 'app') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $root 'application.out.log') -RedirectStandardError (Join-Path $root 'application.err.log') -PassThru
  $p.Id|Set-Content (Join-Path $root 'application.pid')
 }
 $url="http://127.0.0.1:$($state.httpPort)/login"
 $ready=$false
 for($i=0;$i -lt 60;$i++){try{$response=Invoke-WebRequest $url -UseBasicParsing -TimeoutSec 2;if($response.StatusCode -eq 200){$ready=$true;break}}catch{};Start-Sleep -Milliseconds 500}
 if(-not $ready){throw '网站启动失败，查看application.err.log'}
 Write-Host "网站已就绪：$url" -ForegroundColor Green
 if(-not $NoBrowser){Start-Process $url}
}
try{
 if(-not [Environment]::Is64BitProcess){throw '请使用64位Windows PowerShell运行'}
 if($Action -eq 'Install'){
  $payload=Join-Path $PSScriptRoot 'payload'
  $manifest=Get-Content (Join-Path $PSScriptRoot 'manifest.json') -Raw|ConvertFrom-Json
  Write-Host '[1/6] 校验离线安装文件...'
  foreach($file in $manifest.files){$actual=(Get-FileHash (Join-Path $payload $file.name) -Algorithm SHA256).Hash;if($actual -ne $file.sha256){throw ('安装文件损坏：'+$file.name)}}
  if(Test-Path $stateFile){$state=Get-Content $stateFile -Raw|ConvertFrom-Json}else{
   if(Test-Path $root){throw '目标目录已存在但无安装标记，禁止覆盖'}
   Set-PrivateDirectory $root
   $state=[pscustomobject]@{version=1;httpPort=(Free-Port 19001);dbPort=(Free-Port 13306);stage='created';installedBy=[Security.Principal.WindowsIdentity]::GetCurrent().User.Value}
   Write-Text $stateFile ($state|ConvertTo-Json)
  }
  if($state.installedBy -ne [Security.Principal.WindowsIdentity]::GetCurrent().User.Value){throw '必须使用首次安装的Windows账号'}
  Write-Host '[2/6] 安装Java与独立MySQL环境...'
  Expand-Runtime (Join-Path $payload 'java.zip') (Join-Path $root 'java') 'bin/java.exe'
  Expand-Runtime (Join-Path $payload 'mysql.zip') (Join-Path $root 'mysql') 'bin/mysqld.exe'
  & "$root/mysql/bin/mysqld.exe" --version|Out-Null
  if($LASTEXITCODE -ne 0){
   Write-Host '需要微软VC++运行库，Windows将显示安装确认。'
   $vc=Start-Process (Join-Path $payload 'vc-redist.exe') -Verb RunAs -ArgumentList '/install /quiet /norestart' -Wait -PassThru
   if($vc.ExitCode -notin 0,3010,1638){throw 'VC++运行库安装失败'}
  }
  Write-Host '[3/6] 安装Python与pip...'
  Expand-Runtime (Join-Path $payload 'python.zip') (Join-Path $root 'python') 'python.exe'
  if(-not(Test-Path "$root/app")){[IO.Compression.ZipFile]::ExtractToDirectory((Join-Path $payload 'app.zip'),(Join-Path $root 'app'))}
  Set-PrivateDirectory $vault
  if($state.stage -ne 'created'){foreach($required in @('data-key','db-admin-password','initial-admin')){if(-not(Test-Path "$vault/$required.dpapi")){throw "加密凭据缺失：$required；请恢复原密钥，禁止自动生成替换"}}}
  if(-not(Test-Path "$vault/data-key.dpapi")){$bytes=New-Object byte[] 32;$rng=[Security.Cryptography.RandomNumberGenerator]::Create();$rng.GetBytes($bytes);$rng.Dispose();Save-Secret 'data-key' ([Convert]::ToBase64String($bytes))}
  if(-not(Test-Path "$vault/db-admin-password.dpapi")){Save-Secret 'db-admin-password' (Random-Secret)}
  if(-not(Test-Path "$vault/initial-admin.dpapi")){Save-Secret 'initial-admin' (Random-Secret)}
  Apply-Environment
  Write-Host '[4/6] 初始化独立数据库...'
  if($state.stage -eq 'created'){
   if(Test-Path "$root/mysql-data"){throw '发现未完成数据库初始化，禁止自动删除；请查看日志'}
   $base=$root.Replace('\','/')
   Write-Text "$root/mysql.ini" "[mysqld]`nbasedir=$base/mysql`ndatadir=$base/mysql-data`nport=$($state.dbPort)`nbind-address=127.0.0.1`nmysqlx=0`nlocal-infile=0`nsecure-file-priv=NULL`ncharacter-set-server=utf8mb4`ncollation-server=utf8mb4_unicode_ci`n"
   & "$root/mysql/bin/mysqld.exe" "--defaults-file=$root/mysql.ini" --initialize-insecure --console
   if($LASTEXITCODE -ne 0){throw 'MySQL初始化失败；保留目录检查日志'}
   Write-Text "$vault/mysql-bootstrap.sql" ("ALTER USER 'root'@'localhost' IDENTIFIED BY '"+(Read-Secret 'db-admin-password')+"';")
   $state.stage='database-ready';Write-Text $stateFile ($state|ConvertTo-Json)
  }
  Start-Database
  if($state.stage -eq 'database-ready'){
   Import-Sql "$root/app/db/schema.sql"
   $state.stage='importing';Write-Text $stateFile ($state|ConvertTo-Json)
   Import-Sql "$root/app/db/seed.sql"
   $state.stage='seeded';Write-Text $stateFile ($state|ConvertTo-Json)
  }
  if($state.stage -eq 'importing'){throw '上次种子导入中断，保留现场；不要再次覆盖导入'}
  Write-Host '[5/6] 配置加密、管理员及最小权限...'
  if($state.stage -eq 'seeded'){
   $env:DB_USER='root';$env:DB_PASSWORD=Read-Secret 'db-admin-password';$env:SETUP_ADMIN_PASSWORD=Read-Secret 'initial-admin';$env:AITRAINER_FRESH_INSTALL='YES'
   Push-Location "$root/app"
   try{
    & "$root/java/bin/java.exe" '-Dfile.encoding=UTF-8' -cp 'build/classes;lib/connector.jar' com.aitrainer.security.PortableSetup
    if($LASTEXITCODE -ne 0){throw '安全初始化失败'}
    if(-not(Test-Path "$vault/db-user.dpapi")){
     $credentialJson=& "$root/java/bin/java.exe" -cp 'build/classes;lib/connector.jar' com.aitrainer.security.ProvisionDatabase
     if($LASTEXITCODE -ne 0){throw '运行账号创建失败'}
     $credentials=$credentialJson|ConvertFrom-Json
     Save-Secret 'db-password' $credentials.password;Save-Secret 'db-user' $credentials.username
     $credentials=$null;$credentialJson=$null
    }
   }finally{Pop-Location;$env:SETUP_ADMIN_PASSWORD=$null;$env:AITRAINER_FRESH_INSTALL=$null;Apply-Environment}
   $state.stage='complete';Write-Text $stateFile ($state|ConvertTo-Json)
  }
  Write-Host '[6/6] 检查并启动学习平台...'
  Write-Host ('安装目录：'+$root)
  & "$root/java/bin/java.exe" -version
  & "$root/python/python.exe" -c 'import sqlite3,ssl; print(1)'
  if($LASTEXITCODE -ne 0){throw 'Python检查失败'}
  & "$root/python/python.exe" -m pip --version
  if($LASTEXITCODE -ne 0){throw 'pip检查失败'}
  Start-Application
  Copy-Item -LiteralPath $PSCommandPath -Destination (Join-Path $root 'control.ps1') -Force
  $desktop=[Environment]::GetFolderPath('Desktop')
  if(-not $NoBrowser -and $desktop -and -not(Test-Path (Join-Path $desktop 'AI训练师学习平台.lnk'))){
   try{$shell=New-Object -ComObject WScript.Shell;$shortcut=$shell.CreateShortcut((Join-Path $desktop 'AI训练师学习平台.lnk'));$shortcut.TargetPath=Join-Path $PSHOME 'powershell.exe';$shortcut.Arguments='-NoLogo -NoProfile -ExecutionPolicy Bypass -File "'+$root+'\control.ps1" -Action Start -InstallDir "'+$root+'"';$shortcut.WorkingDirectory=$root;$shortcut.Save()}catch{Write-Host '桌面快捷方式创建失败，可使用启动平台.bat。'}
  }
  if(-not $HideCredentials){Write-Host ('初始管理员：admin  初始密码：'+(Read-Secret 'initial-admin'));Write-Host '首次登录必须修改密码。DeepSeek密钥在管理后台设置。'}
 }else{
  if(-not(Test-Path $stateFile)){throw '请先双击一键安装'}
  $state=Get-Content $stateFile -Raw|ConvertFrom-Json
  if($state.installedBy -ne [Security.Principal.WindowsIdentity]::GetCurrent().User.Value){throw 'Windows账号与安装身份不一致'}
  Apply-Environment
  switch($Action){
   'Start'{if($state.stage -ne 'complete'){throw '请重新运行安装入口完成初始化'};Start-Application}
   'Stop'{
    $app=Owned-Process 'application';if($app){Stop-Process -Id $app.ProcessId}
    if(Owned-Process 'database'){$env:MYSQL_PWD=Read-Secret 'db-admin-password';try{& "$root/mysql/bin/mysqladmin.exe" --protocol=TCP '--host=127.0.0.1' "--port=$($state.dbPort)" -uroot --connect-timeout=3 shutdown;if($LASTEXITCODE -ne 0){throw '数据库正常停止失败'}}finally{$env:MYSQL_PWD=$null}}
    Write-Host '本部署的应用和数据库已停止，数据保留。'
   }
   'Check'{& "$root/java/bin/java.exe" -version;& "$root/python/python.exe" --version;& "$root/python/python.exe" -m pip --version;& "$root/mysql/bin/mysqld.exe" --version;Write-Host ('安装阶段：'+$state.stage);Write-Host ('网站端口：'+$state.httpPort+'；数据库端口：'+$state.dbPort);Write-Host ('应用运行：'+[bool](Owned-Process 'application')+'；数据库运行：'+[bool](Owned-Process 'database'))}
   'Credentials'{Write-Host ('账号：admin；初始密码：'+(Read-Secret 'initial-admin'));Write-Host '仅显示首次安装密码；已修改后请使用新密码。'}
   'Shell'{Write-Host '此终端已配置Java、Python和MySQL路径；数据库密码不注入终端。';Start-Process cmd.exe -WorkingDirectory $root -Wait}
  }
 }
}catch{Write-Host ('操作失败：'+$_.Exception.Message) -ForegroundColor Red;exit 1}
finally{foreach($entry in $originalEnvironment.GetEnumerator()){[Environment]::SetEnvironmentVariable($entry.Key,$entry.Value,'Process')}}
