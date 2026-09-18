$ErrorActionPreference='Stop'
$scriptDir=$PSScriptRoot
$mysql='C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe'
if(-not(Test-Path $mysql)){$mysql='mysql'}
if(-not $env:MYSQL_ROOT_PASSWORD){throw '首次初始化需要 MYSQL_ROOT_PASSWORD 环境变量；已有库禁止重新初始化'}
$previous=$env:MYSQL_PWD
try {
 $env:MYSQL_PWD=$env:MYSQL_ROOT_PASSWORD
 $count=& $mysql -uroot --batch --skip-column-names -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='ai_trainer_platform'"
 if($LASTEXITCODE -ne 0){throw '数据库连接检查失败'}
 if([int]$count -ne 0){throw '数据库已有表；请使用迁移流程，禁止重复导入'}
 foreach($name in @('schema.sql','seed.sql')){
  & $mysql -uroot --default-character-set=utf8mb4 -e "source $($scriptDir.Replace('\','/'))/$name"
  if($LASTEXITCODE -ne 0){throw "初始化在 $name 失败；保留现场并检查，禁止忽略错误"}
 }
 Write-Output '空库初始化完成。必须继续安全迁移、设置独立运行账号，旧种子账号强制改密后方可使用。'
} finally {$env:MYSQL_PWD=$previous}
