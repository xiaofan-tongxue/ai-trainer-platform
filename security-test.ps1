$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
& .\build.ps1
$java='C:\Program Files\Java\jdk-24\bin\java.exe'
$javac='C:\Program Files\Java\jdk-24\bin\javac.exe'
$cp='build/classes;lib/mysql-connector-java-5.1.37-bin.jar'
& $javac --release 8 -encoding UTF-8 -cp $cp -d build/classes src/test/java/com/aitrainer/service/SecurityTest.java
if($LASTEXITCODE -ne 0){throw 'Security test compilation failed'}
$previousUser=$env:DB_USER;$previousPassword=$env:DB_PASSWORD
try {
    $maintenance=Join-Path $PSScriptRoot 'runtime/security/db-admin-password.dpapi'
    if(Test-Path $maintenance){
        Add-Type -AssemblyName System.Security
        $env:DB_USER='root'
        $env:DB_PASSWORD=[Text.Encoding]::UTF8.GetString([Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($maintenance),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser))
    }
    & $java '-Dfile.encoding=UTF-8' -cp $cp com.aitrainer.service.SecurityTest
    if($LASTEXITCODE -ne 0){throw 'Security tests failed'}
} finally {$env:DB_USER=$previousUser;$env:DB_PASSWORD=$previousPassword}
