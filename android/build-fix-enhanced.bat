@echo off
echo ====================================
echo Enhanced Build Fix Script
echo ====================================
echo.

echo [1/5] Stopping all Gradle daemons...
call gradlew --stop
timeout /t 2 >nul

echo.
echo [2/5] Cleaning Gradle cache and build directories...
rmdir /s /q .gradle\configuration-cache 2>nul
rmdir /s /q .gradle\file-system.probe 2>nul
rmdir /s /q app\build 2>nul
rmdir /s /q build 2>nul

echo.
echo [3/5] Deleting Kotlin build cache...
rmdir /s /q app\build\kotlin 2>nul
rmdir /s /q app\build\tmp\kapt3 2>nul

echo.
echo [4/5] Invalidating caches...
del /q .gradle\*.lock 2>nul
del /q .gradle\*.bin 2>nul

echo.
echo [5/5] Running clean build with fresh daemon...
call gradlew clean --no-daemon
echo.
echo Building project...
call gradlew :app:assembleDebug --no-daemon --no-build-cache --refresh-dependencies --stacktrace

echo.
echo ====================================
echo Build completed with exit code: %ERRORLEVEL%
echo ====================================
pause