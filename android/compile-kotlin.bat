@echo off
echo Starting Kotlin compilation...
call gradlew.bat :app:compileDebugKotlin
echo.
echo Build completed with exit code: %ERRORLEVEL%
if %ERRORLEVEL% NEQ 0 (
    echo Build FAILED
    exit /b %ERRORLEVEL%
) else (
    echo Build SUCCESSFUL
)