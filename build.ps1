$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

$JDK = @(
  "$env:ProgramFiles\Java\jdk-24\bin\javac.exe",
  "${env:ProgramFiles(x86)}\Java\jdk-24\bin\javac.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $JDK) { $JDK = "javac" }

$LocalJar = Join-Path $Root "lib\mysql-connector-java-5.1.37-bin.jar"
$Connector = @(
  $LocalJar,
  "${env:ProgramFiles(x86)}\MySQL\Connector.J 5.1\mysql-connector-java-5.1.37-bin.jar",
  "$env:ProgramFiles\MySQL\Connector.J 5.1\mysql-connector-java-5.1.37-bin.jar"
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $Connector) { throw "未找到 MySQL Connector/J 5.1.37 驱动" }

$Out = Join-Path $Root "build\classes"
New-Item -ItemType Directory -Force -Path $Out | Out-Null

$Sources = Get-ChildItem -Path (Join-Path $Root "src\main\java") -Recurse -Filter *.java | ForEach-Object { $_.FullName }
Write-Host "[build] 使用 javac: $JDK"
Write-Host "[build] 编译 $($Sources.Count) 个源文件 (target=Java 8)..."

$CompilerVersion = (& $JDK -version 2>&1 | Out-String)
if ($CompilerVersion -match 'javac 1\.8') {
    & $JDK -source 8 -target 8 -encoding UTF-8 -cp $Connector -d $Out $Sources
} else {
    & $JDK --release 8 -encoding UTF-8 -cp $Connector -d $Out $Sources
}
if ($LASTEXITCODE -ne 0) { throw "编译失败" }

Write-Host "[build] 编译完成 -> build\classes" -ForegroundColor Green
