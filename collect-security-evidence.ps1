$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
$destination=Join-Path $PSScriptRoot 'docs/security/evidence'
New-Item -ItemType Directory -Force -Path $destination | Out-Null
$cp='build/classes;lib/mysql-connector-java-5.1.37-bin.jar'
$audit=& 'C:\Program Files\Java\jdk-24\bin\java.exe' -cp $cp com.aitrainer.security.SecurityAudit
if($LASTEXITCODE -ne 0){throw '审计链验证失败'}
$http=Get-Content (Join-Path $destination 'http-security-results.json') -Raw | ConvertFrom-Json
if(@($http.results|Where-Object {-not $_.passed}).Count){throw 'HTTP证据包含失败项'}
$backup=Get-Content 'runtime/security/legacy-backup-verification.json' -Raw | ConvertFrom-Json
$listeners=@(Get-NetTCPConnection -State Listen -LocalPort 19001 | Select-Object LocalAddress,LocalPort)
$report=@{
 collectedAt=(Get-Date).ToString('o');scope='Local single-instance application; production controls not assessed'
 http=@{testedAt=$http.testedAt;target=$http.target;passed=@($http.results).Count}
 auditChain=@($audit);auditExternalAnchorEstablished=$false;listeners=$listeners
 legacyBackupCopies=@($backup|Select-Object source,encrypted,sha256Base64,verified,originalRetained)
 pending=@('Historical plaintext cleanup approval','Production deployment and MLPS level','Database/driver upgrade','MFA and role separation','Strict CSP and isolated code execution','Independent archive and disaster recovery','Applicable commercial cryptography assessment')
}
$report|ConvertTo-Json -Depth 7|Set-Content -Encoding UTF8 (Join-Path $destination 'delivery-verification.json')
$jar=Get-Item 'lib/mysql-connector-java-5.1.37-bin.jar'
@{
 recordedAt=(Get-Date).ToString('o');coverage='Direct runtime components only; no claim of complete transitive CVE matching'
 components=@(
  @{name='MySQL Connector/J';version='5.1.37';path='lib/'+$jar.Name;sha256=(Get-FileHash $jar.FullName -Algorithm SHA256).Hash;status='Legacy 5.1 branch; upgrade pending';reference='https://dev.mysql.com/blog-archive/support-eol-for-mysql-connector-j-5-1/'},
  @{name='MySQL Server';version='5.7';status='Existing host service; supported-version migration pending'},
  @{name='Java';version='JDK 24 host, Java 8 compilation target';status='Deployment runtime patch and support policy to verify'},
  @{name='Pyodide';version='0.24.1';url='https://cdn.jsdelivr.net/pyodide/v0.24.1/full/pyodide.js';status='CDN loader; isolation, integrity and transitive WASM/package audit pending'}
 )
}|ConvertTo-Json -Depth 6|Set-Content -Encoding UTF8 (Join-Path $destination 'components.json')
$sourceFiles=Get-ChildItem src/main/java,webapp -Recurse -File | Where-Object {$_.Extension -in '.java','.js','.html','.css'}
$sourceFiles|ForEach-Object {[pscustomobject]@{path=$_.FullName.Substring($PSScriptRoot.Length+1).Replace('\','/');sha256=(Get-FileHash $_.FullName -Algorithm SHA256).Hash}}|ConvertTo-Json -Depth 3|Set-Content -Encoding UTF8 (Join-Path $destination 'source-manifest.json')
Write-Output ('证据已收集：HTTP '+@($http.results).Count+' 项、审计链、组件及源码摘要。未导出密钥或日志正文。')
