@echo off
REM AstralStream Elite Upgrade Agent Installation Script for Windows
REM This script sets up the complete environment for upgrading your video player

setlocal enabledelayedexpansion

echo ========================================
echo AstralStream Elite Upgrade Agent Setup
echo ========================================
echo.

REM Check if Python is installed
echo [STEP] Checking Python installation...
python --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python is not installed or not in PATH
    echo Please install Python 3.8 or higher from https://python.org
    echo Make sure to check "Add Python to PATH" during installation
    pause
    exit /b 1
)

REM Get Python version
for /f "tokens=2" %%i in ('python --version 2^>^&1') do set PYTHON_VERSION=%%i
echo [INFO] Python %PYTHON_VERSION% found

REM Check if pip is installed
echo.
echo [STEP] Checking pip installation...
pip --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] pip is not installed
    echo Please reinstall Python with pip included
    pause
    exit /b 1
)
echo [INFO] pip found

REM Create virtual environment
echo.
echo [STEP] Creating virtual environment...
if not exist "venv" (
    python -m venv venv
    echo [INFO] Virtual environment created
) else (
    echo [INFO] Virtual environment already exists
)

REM Activate virtual environment
echo [INFO] Activating virtual environment...
call venv\Scripts\activate.bat

REM Upgrade pip
echo [INFO] Upgrading pip...
python -m pip install --upgrade pip

REM Install dependencies
echo.
echo [STEP] Installing dependencies...
if exist "requirements.txt" (
    pip install -r requirements.txt
    echo [INFO] Dependencies installed from requirements.txt
) else (
    echo [WARNING] requirements.txt not found, installing essential packages...
    pip install pyyaml pathlib2 click colorama tqdm jinja2
)

REM Install the package
echo.
echo [STEP] Installing AstralStream Elite Agent...
if exist "setup.py" (
    pip install -e .
    echo [INFO] Package installed in development mode
) else (
    echo [ERROR] setup.py not found
    pause
    exit /b 1
)

REM Verify installation
echo.
echo [STEP] Verifying installation...
astral-upgrade --help >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Installation failed - command not found
    pause
    exit /b 1
) else (
    echo [INFO] Installation successful! Command 'astral-upgrade' is available
)

REM Create example configuration
echo.
echo [STEP] Creating example configuration...
if not exist "example-config.yaml" (
    if exist "config.yaml" (
        copy config.yaml example-config.yaml >nul
    ) else (
        echo # Example AstralStream Elite Agent Configuration > example-config.yaml
        echo project: >> example-config.yaml
        echo   name: "AstralStream" >> example-config.yaml
        echo   version: "2.0.0-elite" >> example-config.yaml
        echo   package_name: "com.astralplayer" >> example-config.yaml
        echo. >> example-config.yaml
        echo quality_targets: >> example-config.yaml
        echo   code_coverage: 85 >> example-config.yaml
        echo   performance_score: 95 >> example-config.yaml
        echo   security_score: 100 >> example-config.yaml
    )
    echo [INFO] Example configuration created
) else (
    echo [INFO] Example configuration already exists
)

REM Show usage instructions
echo.
echo [STEP] Usage Instructions
echo.
echo To upgrade your AstralStream project:
echo.
echo 1. Navigate to your project directory:
echo    cd C:\path\to\your\astralstream\project
echo.
echo 2. Activate the virtual environment:
echo    call C:\path\to\elite-agent\venv\Scripts\activate.bat
echo.
echo 3. Run the upgrade agent:
echo    astral-upgrade --project-path .
echo.
echo 4. Optional flags:
echo    --dry-run          Preview changes without modifying files
echo    --config FILE      Use custom configuration file
echo    --skip-backup      Skip backup creation (not recommended)
echo.
echo 5. For help:
echo    astral-upgrade --help
echo.

echo ========================================
echo [SUCCESS] Installation Complete!
echo ========================================
echo.
echo Next steps:
echo 1. Navigate to your AstralStream project directory
echo 2. Activate the virtual environment:
echo    call %CD%\venv\Scripts\activate.bat
echo 3. Run: astral-upgrade --project-path .
echo.
echo Press any key to continue...
pause >nul