$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
Add-Type -AssemblyName System.Security
$vault=Join-Path $PSScriptRoot 'runtime/security'
New-Item -ItemType Directory -Path $vault -Force | Out-Null
$acl=New-Object Security.AccessControl.DirectorySecurity
$acl.SetAccessRuleProtection($true,$false)
foreach($sid in @([Security.Principal.WindowsIdentity]::GetCurrent().User,[Security.Principal.SecurityIdentifier]::new('S-1-5-18'))){
 $rule=New-Object Security.AccessControl.FileSystemAccessRule($sid,'FullControl','ContainerInherit,ObjectInherit','None','Allow')
 $acl.AddAccessRule($rule)
}
Set-Acl -LiteralPath $vault -AclObject $acl
function Save-Protected([string]$name,[string]$value){
 $target=Join-Path $vault ($name+'.dpapi')
 if(Test-Path -LiteralPath $target){return}
 $bytes=[Text.Encoding]::UTF8.GetBytes($value)
 $sealed=[Security.Cryptography.ProtectedData]::Protect($bytes,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
 $check=[Security.Cryptography.ProtectedData]::Unprotect($sealed,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
 if([Convert]::ToBase64String($check) -ne [Convert]::ToBase64String($bytes)){throw 'Secret round-trip failed'}
 [IO.File]::WriteAllBytes($target,$sealed)
}
if(-not(Test-Path (Join-Path $vault 'data-key.dpapi'))){
 $keyBytes=New-Object byte[] 32
 $rng=[Security.Cryptography.RandomNumberGenerator]::Create();$rng.GetBytes($keyBytes);$rng.Dispose()
 Save-Protected 'data-key' ([Convert]::ToBase64String($keyBytes))
}
if(-not(Test-Path (Join-Path $vault 'db-password.dpapi'))){
 $dbSecret=$env:DB_PASSWORD
 if(-not $dbSecret){
  $legacy=[IO.File]::ReadAllText((Join-Path $PSScriptRoot 'src/main/java/com/aitrainer/config/Config.java'))
  $match=[regex]::Match($legacy,'env\("DB_PASSWORD",\s*"([^"\r\n]+)"\)')
  if($match.Success){$dbSecret=$match.Groups[1].Value}
 }
 if(-not $dbSecret){throw '请先设置 DB_PASSWORD 环境变量'}
 Save-Protected 'db-password' $dbSecret
 $dbSecret=$null
}
$backup=Join-Path $vault ('application-before-security-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.zip')
Compress-Archive -Path @('src','webapp','db','data','*.ps1','*.bat','README.md','DEPLOY.md') -DestinationPath $backup
$bytes=[IO.File]::ReadAllBytes($backup)
$sealed=[Security.Cryptography.ProtectedData]::Protect($bytes,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
$checked=[Security.Cryptography.ProtectedData]::Unprotect($sealed,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
if([Convert]::ToBase64String($bytes) -ne [Convert]::ToBase64String($checked)){throw 'Backup verification failed'}
[IO.File]::WriteAllBytes(($backup+'.dpapi'),$sealed)
$resolved=[IO.Path]::GetFullPath($backup)
if(-not $resolved.StartsWith(([IO.Path]::GetFullPath($vault)+'\'))){throw 'Backup outside vault'}
Remove-Item -LiteralPath $backup
Write-Output 'DPAPI密钥库及加密应用备份已创建，未输出任何密钥。请使用同一Windows账号启动。'
