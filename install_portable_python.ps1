# install_portable_python.ps1
$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$envDir = Join-Path $scriptDir ".python_env"
$zipPath = Join-Path $scriptDir "python_embed.zip"
$getPipPath = Join-Path $scriptDir "get-pip.py"

Write-Host "Setting up portable Python 3.11 in $envDir..." -ForegroundColor Cyan

if (-not (Test-Path $envDir)) {
    New-Item -ItemType Directory -Path $envDir -Force | Out-Null
}

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

# 1. Download zip if python.exe not present
$pyExe = Join-Path $envDir "python.exe"
if (-not (Test-Path $pyExe)) {
    Write-Host "Downloading Python 3.11.9 embeddable..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri "https://www.python.org/ftp/python/3.11.9/python-3.11.9-embed-amd64.zip" -OutFile $zipPath -UseBasicParsing
    
    Write-Host "Extracting..." -ForegroundColor Yellow
    Expand-Archive -Path $zipPath -DestinationPath $envDir -Force
    Remove-Item $zipPath -Force
    
    # 2. Enable site-packages in ._pth file
    $pthFile = Get-ChildItem -Path $envDir -Filter "*._pth" | Select-Object -First 1
    if ($pthFile) {
        $content = Get-Content $pthFile.FullName
        $newContent = $content | ForEach-Object {
            if ($_ -eq "#import site") { "import site" } else { $_ }
        }
        Set-Content -Path $pthFile.FullName -Value $newContent
    }
}

Write-Host "Verifying Python executable..." -ForegroundColor Green
& $pyExe -c "import sys; print('Python executable running:', sys.version)"

# 3. Install pip
$scriptsDir = Join-Path $envDir "Scripts"
$pipExe = Join-Path $scriptsDir "pip.exe"
if (-not (Test-Path $pipExe)) {
    Write-Host "Downloading get-pip.py..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri "https://bootstrap.pypa.io/get-pip.py" -OutFile $getPipPath -UseBasicParsing
    
    Write-Host "Installing pip..." -ForegroundColor Yellow
    & $pyExe $getPipPath --no-warn-script-location
    if (Test-Path $getPipPath) { Remove-Item $getPipPath -Force }
}

# 4. Install required packages
Write-Host "Installing dependencies (flask, requests)..." -ForegroundColor Yellow
& $pyExe -m pip install flask requests --no-warn-script-location

Write-Host "=== Portable Python Setup Complete! ===" -ForegroundColor Green
Write-Host "Executable: $pyExe"
