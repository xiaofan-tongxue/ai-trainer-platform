$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
if(Get-NetTCPConnection -State Listen -LocalPort 19001 -ErrorAction SilentlyContinue){throw '请先停止应用并完成备份，迁移期间禁止写入'}
$cp='build/classes;lib/mysql-connector-java-5.1.37-bin.jar'
if(-not(Test-Path 'build/classes/com/aitrainer/security/SecurityMigration.class')){throw '请先运行 build.ps1'}
$previousUser=$env:DB_USER;$previousPassword=$env:DB_PASSWORD
try{
 $maintenance=Join-Path $PSScriptRoot 'runtime/security/db-admin-password.dpapi'
 if(Test-Path $maintenance){
  Add-Type -AssemblyName System.Security
  $env:DB_USER='root'
  $env:DB_PASSWORD=[Text.Encoding]::UTF8.GetString([Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($maintenance),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser))
 }
 & 'C:\Program Files\Java\jdk-24\bin\java.exe' -Dfile.encoding=UTF-8 -cp $cp com.aitrainer.security.SecurityMigration
 if($LASTEXITCODE -ne 0){throw '迁移未完成；保留密钥和现场，按手册检查'}
}finally{$env:DB_USER=$previousUser;$env:DB_PASSWORD=$previousPassword}
