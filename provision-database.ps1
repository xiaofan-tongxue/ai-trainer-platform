$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
Add-Type -AssemblyName System.Security
$vault=Join-Path $PSScriptRoot 'runtime/security'
if(Test-Path (Join-Path $vault 'db-user.dpapi')){throw '运行账号已配置；为避免重复创建，已停止'}
$cp='build/classes;lib/mysql-connector-java-5.1.37-bin.jar'
$raw=& 'C:\Program Files\Java\jdk-24\bin\java.exe' -cp $cp com.aitrainer.security.ProvisionDatabase
if($LASTEXITCODE -ne 0){throw '最小权限数据库账号创建失败'}
$credentials=$raw | ConvertFrom-Json
Copy-Item -LiteralPath (Join-Path $vault 'db-password.dpapi') -Destination (Join-Path $vault 'db-admin-password.dpapi')
foreach($item in @(@('db-user',$credentials.username),@('db-password',$credentials.password))){
 $bytes=[Text.Encoding]::UTF8.GetBytes($item[1])
 $sealed=[Security.Cryptography.ProtectedData]::Protect($bytes,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
 $plain=[Security.Cryptography.ProtectedData]::Unprotect($sealed,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
 if([Convert]::ToBase64String($bytes) -ne [Convert]::ToBase64String($plain)){throw '凭据往返校验失败'}
 [IO.File]::WriteAllBytes((Join-Path $vault ($item[0]+'.dpapi')),$sealed)
}
$credentials=$null;$raw=$null
Write-Output '最小权限数据库账号已创建，DDL拒绝验证通过，凭据已DPAPI加密保存。重启服务后生效。'
