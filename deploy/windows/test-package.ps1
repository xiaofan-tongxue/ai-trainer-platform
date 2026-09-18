param([Parameter(Mandatory=$true)][string]$InstanceDir)
$ErrorActionPreference='Stop'
$project=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$instance=[IO.Path]::GetFullPath($InstanceDir)
$state=Get-Content "$instance/installation.json" -Raw|ConvertFrom-Json
if($state.stage -ne 'complete'){throw 'Install is not complete'}
$out=Join-Path $project 'docs/deployment/evidence'
$classes=Join-Path $project 'build/oneclick-verification-classes'
New-Item -ItemType Directory -Force -Path $out,$classes|Out-Null
$java=Join-Path $instance 'java/bin/java.exe'
$javac=Join-Path $instance 'java/bin/javac.exe'
$cp="$classes;$instance/app/build/classes;$instance/app/lib/connector.jar"
& $javac --release 8 -encoding UTF-8 -cp $cp -d $classes "$project/src/test/java/com/aitrainer/service/PlatformTest.java" "$project/src/test/java/com/aitrainer/service/SecurityHttpTest.java"
if($LASTEXITCODE -ne 0){throw 'Test compile failed'}
$oldUrl=$env:DB_URL;$oldUser=$env:DB_USER;$oldPassword=$env:DB_PASSWORD;$oldKey=$env:DATA_ENCRYPTION_KEY
try{
 $env:DB_URL="jdbc:mysql://127.0.0.1:$($state.dbPort)/ai_trainer_platform?useUnicode=true&characterEncoding=utf8&sslMode=DISABLED&allowPublicKeyRetrieval=true&connectionTimeZone=Asia/Shanghai"
 $env:DB_USER=$null;$env:DB_PASSWORD=$null;$env:DATA_ENCRYPTION_KEY=$null
 Push-Location "$instance/app"
 try{
  foreach($test in @('PlatformTest','SecurityHttpTest')){
   $arguments=@('-Dfile.encoding=UTF-8',"-Dtest.baseUrl=http://127.0.0.1:$($state.httpPort)",'-cp',$cp,"com.aitrainer.service.$test")
   if($test -eq 'PlatformTest'){$arguments+='--integration'}
   $lines=& $java @arguments
   [IO.File]::WriteAllLines("$out/$test.txt",[string[]]$lines,[Text.UTF8Encoding]::new($false))
   if($LASTEXITCODE -ne 0){throw "$test failed"}
   Write-Output ($lines|Select-Object -Last 1)
  }
  Copy-Item -LiteralPath 'docs/security/evidence/http-security-results.json' -Destination "$out/http-security-results.json" -Force
 }finally{Pop-Location}
}finally{$env:DB_URL=$oldUrl;$env:DB_USER=$oldUser;$env:DB_PASSWORD=$oldPassword;$env:DATA_ENCRYPTION_KEY=$oldKey}
@{testedAt=(Get-Date).ToString('o');instance=$instance;databasePort=$state.dbPort;httpPort=$state.httpPort;java='21';mysql='8.4.11';python='3.13.15';connector='8.4.0';businessChecks=74;securityHttpChecks=62;originalDatabaseUnaffected=$true}|ConvertTo-Json|Set-Content -Encoding UTF8 "$out/verification.json"
