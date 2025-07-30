@echo off
echo ========================================
echo Astral Player Automated Test Suite
echo ========================================
echo.

REM Check if device is connected
adb devices | findstr /r "device$" >nul
if errorlevel 1 (
    echo ERROR: No Android device connected!
    echo Please connect a device or start an emulator.
    exit /b 1
)

echo Building and running tests...
echo.

REM Clean build
echo [1/4] Cleaning previous builds...
call gradlew.bat clean

REM Build the app
echo.
echo [2/4] Building debug APK...
call gradlew.bat assembleDebug

REM Build test APK
echo.
echo [3/4] Building test APK...
call gradlew.bat assembleDebugAndroidTest

REM Run tests
echo.
echo [4/4] Running automated tests...
call gradlew.bat connectedDebugAndroidTest

REM Generate report
echo.
echo ========================================
echo Test Results:
echo ========================================
type app\build\reports\androidTests\connected\index.html | findstr /i "passed failed"

echo.
echo Full test report available at:
echo app\build\reports\androidTests\connected\index.html
echo.
echo Test execution completed!
pause