param([switch]$SkipArchive)
$ErrorActionPreference='Stop'
$project=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
Set-Location $project
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Read-DataVersion([string]$path){
 $head=Get-Content -LiteralPath $path -TotalCount 8 -Encoding UTF8
 if(($head -join "`n") -notmatch '"version"\s*:\s*"([^"]+)"'){throw "资料缺少 version：$path"}
 return $Matches[1]
}
function Assert-StagedApp([string]$staged){
 $root=[IO.Path]::GetFullPath($staged)
 $files=@(Get-ChildItem -LiteralPath $root -Recurse -Force -File)
 foreach($file in $files){
  $relative=$file.FullName.Substring($root.Length).TrimStart('\','/')
  if($relative -match '(?i)(^|[\\/])runtime([\\/]|$)' -or $file.Extension -match '(?i)^\.(dpapi|enc|pem|key|pfx|p12|bak|log)$' -or $file.Name -match '(?i)^\.(env|env\.)'){throw "安装包禁止包含：$relative"}
  if($file.Length -ge 40){
   $headerBytes=[IO.File]::ReadAllBytes($file.FullName)[0..39]
   if([Text.Encoding]::ASCII.GetString($headerBytes).StartsWith('version https://git-lfs.github.com/spec/v1')){throw "Git LFS 指针未展开：$relative"}
  }
 }
 $seedText=[IO.File]::ReadAllText((Join-Path $root 'db/seed.sql'))
 if($seedText -match '(?i)INSERT\s+INTO\s+users'){throw '种子仍含用户账号插入'}
 $textExt=@('.sql','.json','.js','.java','.html','.css','.md','.txt','.ps1','.xml','.properties','.py','.ipynb','.csv')
 foreach($file in $files){
  if($textExt -notcontains $file.Extension.ToLower()){continue}
  if($file.Length -gt 8MB){throw "文本文件过大，停止打包：$($file.Name)"}
  $text=[Text.Encoding]::UTF8.GetString([IO.File]::ReadAllBytes($file.FullName))
  if($text -match 'sk-[A-Za-z0-9_-]{20,}'){throw "疑似 API Key：$($file.FullName.Substring($root.Length))"}
  if($text -match '(?i)C:\\Users\\(?!Public\b|Default\b|<)[A-Za-z0-9_.-]+'){throw "疑似个人目录：$($file.FullName.Substring($root.Length))"}
 }
}
$cache=Join-Path $project 'build/oneclick-cache'
$out=Join-Path $project 'dist/AITrainer-Windows-OneClick'
$app=Join-Path $project 'build/oneclick-app'
foreach($stale in @($out,$app)){if(Test-Path -LiteralPath $stale){Remove-Item -LiteralPath $stale -Recurse -Force}}
New-Item -ItemType Directory -Force -Path "$out/payload","$app/build/classes","$app/lib"|Out-Null
$utf8=[Text.UTF8Encoding]::new($true)
foreach($file in @('launcher.ps1','README.md')){[IO.File]::WriteAllText((Join-Path $out $file),[IO.File]::ReadAllText((Join-Path $PSScriptRoot $file)),$utf8)}
$entries=@{'一键安装'='Install';'启动平台'='Start';'停止平台'='Stop';'环境检查'='Check';'环境终端'='Shell';'查看初始账号'='Credentials'}
foreach($entry in $entries.GetEnumerator()){
 $batch="@echo off`r`nchcp 65001 >nul`r`ncd /d `"%~dp0`"`r`npowershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File `"%~dp0launcher.ps1`" -Action $($entry.Value)`r`nif errorlevel 1 echo 操作未完成，请查看上方提示及实例日志。`r`npause`r`n"
 [IO.File]::WriteAllText((Join-Path $out ($entry.Key+'.bat')),$batch,[Text.UTF8Encoding]::new($false))
}
foreach($name in @('java.zip','mysql.zip','python.zip','vc-redist.exe')){Copy-Item -LiteralPath (Join-Path $cache $name) -Destination "$out/payload/$name" -Force}
if(Test-Path "$out/payload/python-installer.exe"){Remove-Item -LiteralPath "$out/payload/python-installer.exe"}
$javaSource=Get-Content "$cache/java-source.json" -Raw|ConvertFrom-Json
if((Get-FileHash "$cache/java.zip" -Algorithm SHA256).Hash -ne $javaSource.checksum){throw 'Java发行方摘要不匹配'}
if((Get-FileHash "$cache/mysql.zip" -Algorithm MD5).Hash -ne '2E833921898A9A030EA6BFE81BD811BC'){throw 'MySQL官方下载摘要不匹配'}
foreach($name in @('python-installer.exe','vc-redist.exe')){if((Get-AuthenticodeSignature "$cache/$name").Status -ne 'Valid'){throw "安装程序数字签名无效：$name"}}
$connector=$cache+'/connector-extracted'
if(-not(Test-Path $connector)){[IO.Compression.ZipFile]::ExtractToDirectory("$cache/connector.zip",$connector)}
$jar=Get-ChildItem $connector -Recurse -Filter 'mysql-connector-j-8.4.0.jar'|Select-Object -First 1
if(-not $jar){throw 'Connector/J8.4 missing'}
Copy-Item -LiteralPath $jar.FullName -Destination "$app/lib/connector.jar" -Force
foreach($directory in @('webapp','data','materials','db','src')){Copy-Item -LiteralPath (Join-Path $project $directory) -Destination $app -Recurse -Force}
Copy-Item -LiteralPath (Join-Path $project 'requirements-labs.txt') -Destination $app -Force
# Include only fresh public seed content, never old account credentials or runtime data.
$seed=[IO.File]::ReadAllText("$project/db/seed.sql")
$seed=[regex]::Replace($seed,'(?s)-- 默认账号:.*?-- 理论知识章节','-- 理论知识章节')
if($seed -match '(?i)INSERT\s+INTO\s+users'){throw 'Default account removal failed'}
[IO.File]::WriteAllText("$app/db/seed.sql",$seed,[Text.UTF8Encoding]::new($false))
# Backup files in the historical web directory must not be distributed.
$unwanted=Get-ChildItem "$app/webapp" -Recurse -File|Where-Object {$_.Name -match '\.(bak|map)$'}
foreach($file in $unwanted){if(-not $file.FullName.StartsWith([IO.Path]::GetFullPath($app)+'\')){throw 'Staging boundary failed'};Remove-Item -LiteralPath $file.FullName}
Assert-StagedApp $app
$sources=Get-ChildItem "$project/src/main/java" -Recurse -Filter '*.java'|ForEach-Object FullName
$javac=@(
 "$env:ProgramFiles\Java\jdk-24\bin\javac.exe",
 "${env:ProgramFiles(x86)}\Java\jdk-24\bin\javac.exe"
)|Where-Object {Test-Path -LiteralPath $_}|Select-Object -First 1
if(-not $javac){$javac='javac'}
$compilerVersion=(& $javac -version 2>&1|Out-String)
if($compilerVersion -match 'javac 1\.8'){
 & $javac -source 8 -target 8 -encoding UTF-8 -cp "$app/lib/connector.jar" -d "$app/build/classes" $sources
}else{
 & $javac --release 8 -encoding UTF-8 -cp "$app/lib/connector.jar" -d "$app/build/classes" $sources
}
if($LASTEXITCODE -ne 0){throw 'Build failed'}
Assert-StagedApp $app
$appZip="$out/payload/app.zip"
if(Test-Path $appZip){Remove-Item -LiteralPath $appZip}
[IO.Compression.ZipFile]::CreateFromDirectory($app,$appZip,[IO.Compression.CompressionLevel]::Optimal,$false)
$payloadNames=@(Get-ChildItem "$out/payload" -File|ForEach-Object Name|Sort-Object)
$expected=@('app.zip','java.zip','mysql.zip','python.zip','vc-redist.exe')
if(Compare-Object $payloadNames $expected){throw ('payload 文件集合与预期不一致：'+$($payloadNames -join ', '))}
$commit=(& git -C $project rev-parse HEAD).Trim()
if($LASTEXITCODE -ne 0 -or $commit -notmatch '^[0-9a-f]{40}$'){throw '无法读取源码提交号'}
$urls=@{'java.zip'=$javaSource.link;'mysql.zip'='https://cdn.mysql.com/Downloads/MySQL-8.4/mysql-8.4.11-winx64.zip';'python.zip'='Runtime extracted from Authenticode-verified https://www.python.org/ftp/python/3.13.15/python-3.13.15-amd64.exe; pip entry points made relative';'vc-redist.exe'='https://aka.ms/vc14/vc_redist.x64.exe';'app.zip'='local project source; JDBC from https://cdn.mysql.com/Downloads/Connector-J/mysql-connector-j-8.4.0.zip'}
$files=Get-ChildItem "$out/payload" -File|ForEach-Object {@{name=$_.Name;bytes=$_.Length;sha256=(Get-FileHash $_.FullName -Algorithm SHA256).Hash;source=$urls[$_.Name]}}
@{createdAt=(Get-Date).ToString('o');platform='Windows x64';sourceCommit=$commit;questionExplanations=(Read-DataVersion (Join-Path $project 'data/question-explanations.json'));practicalGuidance=(Read-DataVersion (Join-Path $project 'data/practical-guidance.json'));java=$javaSource.name;mysql='8.4.11';python='3.13.15';connector='8.4.0';freshInstallOnly=$true;files=@($files)}|ConvertTo-Json -Depth 5|Set-Content -Encoding UTF8 "$out/manifest.json"
if(-not $SkipArchive){
 $archive=Join-Path $project 'dist/AITrainer-Windows-OneClick.zip'
 if(Test-Path $archive){Remove-Item -LiteralPath $archive}
 [IO.Compression.ZipFile]::CreateFromDirectory($out,$archive,[IO.Compression.CompressionLevel]::Fastest,$true)
 $hash=(Get-FileHash $archive -Algorithm SHA256).Hash
 Set-Content -Encoding ascii "$archive.sha256" $hash
 Write-Output "SHA256 $hash"
}
Write-Output "One-click package prepared: $out"
Write-Output "source $commit"
