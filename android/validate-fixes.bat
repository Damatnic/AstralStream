@echo off
echo 🔧 Validating fixes...

set ERRORS=0

REM Check for TODO() implementations in non-test files
echo Checking for TODO() implementations...
for /f %%i in ('findstr /r "TODO()" app\src\main\java\*.kt 2^>nul ^| find /c /v ""') do set TODO_COUNT=%%i
if %TODO_COUNT% gtr 0 (
    echo ❌ Found %TODO_COUNT% TODO() implementations
    set /a ERRORS=%ERRORS%+1
) else (
    echo ✅ No TODO() implementations found
)

REM Check if key files exist
echo Checking key files...
set "FILES=GestureCustomizationScreen.kt AnalyticsDashboardScreen.kt CommunityScreen.kt"
for %%f in (%FILES%) do (
    dir /s /b app\src\main\java\%%f >nul 2>&1
    if errorlevel 1 (
        echo ❌ %%f missing
        set /a ERRORS=%ERRORS%+1
    ) else (
        echo ✅ %%f exists
    )
)

REM Check cloud providers are fixed
echo Checking cloud providers...
findstr /r "connect.*TODO()" app\src\main\java\com\astralplayer\nextplayer\cloud\CloudDataClasses.kt >nul 2>&1
if not errorlevel 1 (
    echo ❌ Cloud providers still have TODO() implementations
    set /a ERRORS=%ERRORS%+1
) else (
    echo ✅ Cloud providers implemented
)

if %ERRORS% equ 0 (
    echo 🎉 All major fixes validated!
) else (
    echo ❌ %ERRORS% issues remain
)

pause