param([Parameter(Mandatory=$true)][string]$InstanceDir)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Security
$project=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$root=[IO.Path]::GetFullPath($InstanceDir)
$launcher=Join-Path $project 'dist/AITrainer-Windows-OneClick/launcher.ps1'
$state=Get-Content "$root/installation.json" -Raw|ConvertFrom-Json
$base="http://127.0.0.1:$($state.httpPort)"
$vault="$root/app/runtime/security"
$checks=@()
function Assert-Check([bool]$condition,[string]$name){if(-not $condition){throw $name};$script:checks+=@{name=$name;passed=$true};Write-Output "PASS $name"}
function Login([string]$password){$body=@{username='admin';password=$password}|ConvertTo-Json -Compress;return (Invoke-RestMethod "$base/api/auth/login" -Method Post -ContentType 'application/json' -Body $body).data}
$original=[Text.Encoding]::UTF8.GetString([Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes("$vault/initial-admin.dpapi"),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser))
$login=Login $original
Assert-Check ($login.user.mustChangePassword -eq $true) 'unique initial administrator requires password change'
$updated=$original+'N9'
[IO.File]::WriteAllBytes("$vault/deployment-test-password.dpapi",[Security.Cryptography.ProtectedData]::Protect([Text.Encoding]::UTF8.GetBytes($updated),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser))
$headers=@{Authorization='Bearer '+$login.token}
$null=Invoke-RestMethod "$base/api/auth/password" -Method Post -ContentType 'application/json' -Headers $headers -Body (@{oldPassword=$original;newPassword=$updated}|ConvertTo-Json -Compress)
$login=Login $updated
Assert-Check ($login.user.mustChangePassword -eq $false) 'new administrator password activates full access'
$headers=@{Authorization='Bearer '+$login.token}
$stats=(Invoke-RestMethod "$base/api/admin/stats" -Headers $headers).data
Assert-Check ($stats.questions -eq 900 -and $stats.practical -eq 40 -and $stats.users -eq 1) 'fresh seed has 900 questions, 40 tasks, only one random-password administrator'
$keyHash=(Get-FileHash "$vault/data-key.dpapi" -Algorithm SHA256).Hash
& powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File $launcher -Action Install -InstallDir $root -NoBrowser -HideCredentials
Assert-Check ($LASTEXITCODE -eq 0) 'repeat installation succeeds'
Assert-Check ((Get-FileHash "$vault/data-key.dpapi" -Algorithm SHA256).Hash -eq $keyHash) 'repeat installation preserves the data key'
$login=Login $updated
Assert-Check ($login.user.mustChangePassword -eq $false) 'repeat installation preserves changed credentials'
& powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File $launcher -Action Stop -InstallDir $root -NoBrowser
Assert-Check ($LASTEXITCODE -eq 0) 'graceful stop succeeds'
& powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File $launcher -Action Start -InstallDir $root -NoBrowser
Assert-Check ($LASTEXITCODE -eq 0) 'restart and HTTP readiness succeed'
$login=Login $updated
Assert-Check ($login.user.mustChangePassword -eq $false) 'database credentials survive restart'
@{testedAt=(Get-Date).ToString('o');httpPort=$state.httpPort;databasePort=$state.dbPort;checks=$checks}|ConvertTo-Json -Depth 5|Set-Content -Encoding UTF8 "$project/docs/deployment/evidence/lifecycle.json"
$original=$null;$updated=$null;$login=$null;$headers=$null
