[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

Write-Host "=============================================="
Write-Host "  人工智能训练师（三级）学习平台 - 一键启动"
Write-Host "=============================================="

if (Test-Path "$Root\build\classes\com\aitrainer\Main.class") {
  Write-Host "[1/2] 检测到已编译产物，跳过编译..."
} else {
  Write-Host "[1/2] 首次运行，正在编译（需要 JDK 8 或更高）..."
  & "$Root\build.ps1"
  if ($LASTEXITCODE -ne 0) {
    Write-Host "编译失败，请安装 JDK 8 或更高版本后重试" -ForegroundColor Red
    Read-Host "按回车退出"
    exit 1
  }
}

Write-Host "[2/2] 启动服务（端口 19001）..."
Write-Host "浏览器访问: http://localhost:19001/login"
Write-Host "旧账号首次登录需更新密码；服务默认仅监听本机。"
Write-Host ""
& "$Root\run.ps1"
