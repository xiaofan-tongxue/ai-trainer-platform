$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
Add-Type -AssemblyName System.Security
Add-Type -AssemblyName System.IO.Compression.FileSystem
$project=[IO.Path]::GetFullPath($PSScriptRoot)
$workspace=[IO.Directory]::GetParent($project).FullName
$vault=Join-Path $project 'runtime/security'
if(-not(Test-Path (Join-Path $vault 'data-key.dpapi'))){throw '请先初始化受限密钥库'}
$targets=@((Join-Path $project 'backups/learning-upgrade-20260911-093832'),(Join-Path $workspace 'ai-trainer-platform.zip'))
$records=@()
foreach($target in $targets){
 if(-not(Test-Path -LiteralPath $target)){continue}
 $resolved=(Resolve-Path -LiteralPath $target).Path
 if($resolved -ne [IO.Path]::GetFullPath($target) -or -not $resolved.StartsWith($workspace+'\',[StringComparison]::OrdinalIgnoreCase)){throw '目标路径验证失败'}
 $item=Get-Item -LiteralPath $resolved
 if($item.Attributes -band [IO.FileAttributes]::ReparsePoint){throw '禁止处理重解析点'}
 $archive=$resolved;$temporary=$false
 if($item.PSIsContainer){
  $linked=Get-ChildItem -LiteralPath $resolved -Recurse -Force | Where-Object { $_.Attributes -band [IO.FileAttributes]::ReparsePoint }
  if($linked){throw '备份包含重解析点，已停止'}
  $archive=Join-Path $vault ('legacy-staging-'+[Guid]::NewGuid().ToString('N')+'.zip')
  [IO.Compression.ZipFile]::CreateFromDirectory($resolved,$archive,[IO.Compression.CompressionLevel]::Optimal,$true)
  $temporary=$true
 }
 $destination=Join-Path $vault ($item.Name+'.protected.dpapi')
 if(Test-Path -LiteralPath $destination){throw '加密副本已存在，需先核对，禁止覆盖'}
 $bytes=[IO.File]::ReadAllBytes($archive)
 $sha=[Security.Cryptography.SHA256]::Create()
 $digest=[Convert]::ToBase64String($sha.ComputeHash($bytes))
 $sealed=[Security.Cryptography.ProtectedData]::Protect($bytes,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
 [IO.File]::WriteAllBytes($destination,$sealed)
 $bytes=$null;$sealed=$null
 $verified=[Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($destination),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
 if([Convert]::ToBase64String($sha.ComputeHash($verified)) -ne $digest){throw '解密回读摘要不一致，保留原件'}
 $sha.Dispose();$verified=$null
 # Keep every original. Removal of legacy plaintext requires separate user approval.
 if($temporary -and [IO.Path]::GetFullPath($archive).StartsWith($vault+'\')){Remove-Item -LiteralPath $archive}
 $records+=@{source=$resolved;encrypted=$destination;sha256Base64=$digest;verified=$true;originalRetained=$true;time=(Get-Date).ToString('o')}
 [GC]::Collect()
}
if($records.Count){$records|ConvertTo-Json -Depth 4|Set-Content -Encoding UTF8 (Join-Path $vault 'legacy-backup-verification.json')}
Write-Output ('历史备份加密副本已验证：'+$records.Count+' 项；全部原件保留，明文清理待单独确认。')
