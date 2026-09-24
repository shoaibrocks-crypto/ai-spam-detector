# setup_env.ps1 - Setup Python and dependencies for AI Spam Detector

$ErrorActionPreference = "Stop"
Write-Host "=== AI Spam Detector Environment Setup ===" -ForegroundColor Cyan

# 1. Check if Python is already available in PATH or common locations
$pythonExe = $null

if (Get-Command python -ErrorAction SilentlyContinue) {
    $pythonExe = "python"
} elseif (Get-Command py -ErrorAction SilentlyContinue) {
    $pythonExe = "py"
} else {
    $commonPaths = @(
        "$env:LOCALAPPDATA\Programs\Python\Python311\python.exe",
        "$env:LOCALAPPDATA\Programs\Python\Python312\python.exe",
        "$env:LOCALAPPDATA\Programs\Python\Python310\python.exe",
        "C:\Program Files\Python311\python.exe",
        "C:\Program Files\Python312\python.exe",
        "C:\Python311\python.exe"
    )
    foreach ($p in $commonPaths) {
        if (Test-Path $p) {
            $pythonExe = $p
            break
        }
    }
}

if (-not $pythonExe) {
    Write-Host "Python not found. Downloading Python 3.11.9 installer..." -ForegroundColor Yellow
    $installerUrl = "https://www.python.org/ftp/python/3.11.9/python-3.11.9-amd64.exe"
    $installerPath = "$env:TEMP\python-3.11.9-installer.exe"
    
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $installerUrl -OutFile $installerPath -UseBasicParsing
    
    Write-Host "Installing Python 3.11 for current user..." -ForegroundColor Yellow
    $process = Start-Process -FilePath $installerPath -ArgumentList "/quiet", "InstallAllUsers=0", "PrependPath=1", "Include_pip=1", "Include_test=0" -Wait -PassThru
    
    Write-Host "Installer finished with exit code $($process.ExitCode)" -ForegroundColor Green
    
    # Refresh PATH for current process
    $userPath = [Environment]::GetEnvironmentVariable("Path", [EnvironmentVariableTarget]::User)
    $machinePath = [Environment]::GetEnvironmentVariable("Path", [EnvironmentVariableTarget]::Machine)
    $env:PATH = "$userPath;$machinePath;$env:LOCALAPPDATA\Programs\Python\Python311;$env:LOCALAPPDATA\Programs\Python\Python311\Scripts"
    
    $pythonExe = "$env:LOCALAPPDATA\Programs\Python\Python311\python.exe"
    if (-not (Test-Path $pythonExe)) {
        if (Get-Command python -ErrorAction SilentlyContinue) {
            $pythonExe = "python"
        }
    }
}

Write-Host "Using Python: $pythonExe" -ForegroundColor Green
& $pythonExe --version

# 2. Upgrade pip and install requirements
Write-Host "Installing required packages (flask, scikit-learn, numpy, requests)..." -ForegroundColor Yellow
& $pythonExe -m pip install --upgrade pip
& $pythonExe -m pip install flask scikit-learn numpy requests

Write-Host "=== Setup completed successfully! ===" -ForegroundColor Green
