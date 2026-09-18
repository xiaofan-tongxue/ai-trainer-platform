$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

$Classes = Join-Path $Root "build\classes"
if (-not (Test-Path $Classes)) { throw "请先运行 .\build.ps1 编译项目" }

$LocalJar = Join-Path $Root "lib\mysql-connector-java-5.1.37-bin.jar"
$Connector = @(
  $LocalJar,
  "${env:ProgramFiles(x86)}\MySQL\Connector.J 5.1\mysql-connector-java-5.1.37-bin.jar",
  "$env:ProgramFiles\MySQL\Connector.J 5.1\mysql-connector-java-5.1.37-bin.jar"
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $Connector) { throw "未找到 MySQL Connector/J 5.1.37 驱动" }

$CP = "$Classes;$Connector"
$Java = @(
  "$env:ProgramFiles\Java\jdk-24\bin\java.exe",
  "${env:ProgramFiles(x86)}\Java\jdk-24\bin\java.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $Java) { $Java = "java" }
Write-Host "[run] 启动学习平台 (Java 8, 端口 19001)..."
& $Java "-Dfile.encoding=UTF-8" -cp $CP com.aitrainer.Main
