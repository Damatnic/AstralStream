@echo off
echo ====================================
echo Deep Clean and Rebuild Script
echo ====================================
echo.

echo [1/7] Killing all Java and Kotlin processes...
taskkill /f /im java.exe 2>nul
taskkill /f /im kotlin-compiler-daemon.exe 2>nul
taskkill /f /im gradle.exe 2>nul
timeout /t 3 >nul

echo.
echo [2/7] Removing ALL Gradle directories...
rmdir /s /q .gradle 2>nul
rmdir /s /q "%USERPROFILE%\.gradle\daemon" 2>nul
rmdir /s /q "%USERPROFILE%\.gradle\caches\transforms-3" 2>nul
rmdir /s /q "%USERPROFILE%\.gradle\caches\modules-2" 2>nul

echo.
echo [3/7] Removing ALL build directories...
rmdir /s /q build 2>nul
rmdir /s /q app\build 2>nul
rmdir /s /q app\.gradle 2>nul

echo.
echo [4/7] Removing kapt generated files...
rmdir /s /q app\build\generated 2>nul
rmdir /s /q app\build\tmp 2>nul

echo.
echo [5/7] Resetting Gradle wrapper...
call gradlew --stop
del /q gradlew.bat 2>nul
del /q gradle\wrapper\gradle-wrapper.jar 2>nul

echo.
echo [6/7] Regenerating Gradle wrapper...
call gradle wrapper --gradle-version=8.14

echo.
echo [7/7] Building from scratch...
call gradlew clean
call gradlew :app:assembleDebug --no-daemon --no-build-cache --refresh-dependencies --info

echo.
echo ====================================
echo Build completed with exit code: %ERRORLEVEL%
echo ====================================
pause