$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
& .\build.ps1
$Javac = 'C:\Program Files\Java\jdk-24\bin\javac.exe'
$Java = 'C:\Program Files\Java\jdk-24\bin\java.exe'
if (-not (Test-Path $Javac)) { $Javac = 'javac'; $Java = 'java' }
$cp = 'build/classes;lib/mysql-connector-java-5.1.37-bin.jar'
$sources = Get-ChildItem src/test/java -Recurse -Filter '*.java' | ForEach-Object FullName
$CompilerVersion = (& $Javac -version 2>&1 | Out-String)
if ($CompilerVersion -match 'javac 1\.8') {
    & $Javac -source 8 -target 8 -encoding UTF-8 -cp $cp -d build/classes $sources
} else {
    & $Javac --release 8 -encoding UTF-8 -cp $cp -d build/classes $sources
}
if ($LASTEXITCODE -ne 0) { throw '测试编译失败' }
& $Java '-Dfile.encoding=UTF-8' -cp $cp com.aitrainer.service.PlatformTest @args
if ($LASTEXITCODE -ne 0) { throw '测试失败' }
