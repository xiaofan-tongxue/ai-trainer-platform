$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

# Repeated clicks on start.bat must reuse the already running local instance.
$LoginUrl = "http://127.0.0.1:19001/login"
try {
  $ExistingPage = Invoke-WebRequest -Uri $LoginUrl -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
  if ($ExistingPage.Content -match '<title>登录 - 人工智能训练师学习平台</title>') {
    Write-Host "[run] 学习平台已经在运行，无需重复启动。" -ForegroundColor Green
    Write-Host "浏览器访问: $LoginUrl"
    return
  }
} catch {
  # No HTTP response (or another program owns this port); check the TCP port below.
}

$PortOccupied = $false
$Probe = New-Object System.Net.Sockets.TcpClient
try {
  $Connect = $Probe.BeginConnect("127.0.0.1", 19001, $null, $null)
  if ($Connect.AsyncWaitHandle.WaitOne(500)) {
    $Probe.EndConnect($Connect)
    $PortOccupied = $true
  }
} catch {
  $PortOccupied = $false
} finally {
  $Probe.Close()
}
if ($PortOccupied) {
  Write-Host "[run] 无法启动：19001 端口已被其他程序占用。请关闭占用端口的程序后重试。" -ForegroundColor Red
  exit 1
}

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
