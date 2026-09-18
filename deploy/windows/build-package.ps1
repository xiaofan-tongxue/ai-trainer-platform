param([switch]$SkipArchive)
$ErrorActionPreference='Stop'
$project=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
Set-Location $project
Add-Type -AssemblyName System.IO.Compression.FileSystem
$cache=Join-Path $project 'build/oneclick-cache'
$out=Join-Path $project 'dist/AITrainer-Windows-OneClick'
$app=Join-Path $project 'build/oneclick-app'
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
# Include only fresh public seed content, never old account credentials or runtime data.
$seed=[IO.File]::ReadAllText("$project/db/seed.sql")
$seed=[regex]::Replace($seed,'(?s)-- 默认账号:.*?-- 理论知识章节','-- 理论知识章节')
if($seed -match '(?i)INSERT\s+INTO\s+users'){throw 'Default account removal failed'}
[IO.File]::WriteAllText("$app/db/seed.sql",$seed,[Text.UTF8Encoding]::new($false))
# Backup files in the historical web directory must not be distributed.
$unwanted=Get-ChildItem "$app/webapp" -Recurse -File|Where-Object {$_.Name -match '\.(bak|map)$'}
foreach($file in $unwanted){if(-not $file.FullName.StartsWith([IO.Path]::GetFullPath($app)+'\')){throw 'Staging boundary failed'};Remove-Item -LiteralPath $file.FullName}
$sources=Get-ChildItem "$project/src/main/java" -Recurse -Filter '*.java'|ForEach-Object FullName
& 'C:\Program Files\Java\jdk-24\bin\javac.exe' --release 8 -encoding UTF-8 -cp "$app/lib/connector.jar" -d "$app/build/classes" $sources
if($LASTEXITCODE -ne 0){throw 'Build failed'}
$appZip="$out/payload/app.zip"
if(Test-Path $appZip){Remove-Item -LiteralPath $appZip}
[IO.Compression.ZipFile]::CreateFromDirectory($app,$appZip,[IO.Compression.CompressionLevel]::Optimal,$false)
$urls=@{'java.zip'=$javaSource.link;'mysql.zip'='https://cdn.mysql.com/Downloads/MySQL-8.4/mysql-8.4.11-winx64.zip';'python.zip'='Runtime extracted from Authenticode-verified https://www.python.org/ftp/python/3.13.15/python-3.13.15-amd64.exe; pip entry points made relative';'vc-redist.exe'='https://aka.ms/vc14/vc_redist.x64.exe';'app.zip'='local project source; JDBC from https://cdn.mysql.com/Downloads/Connector-J/mysql-connector-j-8.4.0.zip'}
$files=Get-ChildItem "$out/payload" -File|ForEach-Object {@{name=$_.Name;bytes=$_.Length;sha256=(Get-FileHash $_.FullName -Algorithm SHA256).Hash;source=$urls[$_.Name]}}
@{createdAt=(Get-Date).ToString('o');platform='Windows x64';java=$javaSource.name;mysql='8.4.11';python='3.13.15';connector='8.4.0';files=@($files)}|ConvertTo-Json -Depth 5|Set-Content -Encoding UTF8 "$out/manifest.json"
if(-not $SkipArchive){
 $archive=Join-Path $project 'dist/AITrainer-Windows-OneClick.zip'
 if(Test-Path $archive){Remove-Item -LiteralPath $archive}
 [IO.Compression.ZipFile]::CreateFromDirectory($out,$archive,[IO.Compression.CompressionLevel]::Fastest,$true)
 (Get-FileHash $archive -Algorithm SHA256).Hash|Set-Content "$archive.sha256"
}
Write-Output "One-click package prepared: $out"
