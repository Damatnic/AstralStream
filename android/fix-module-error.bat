@echo off
echo ====================================
echo Fixing Module Loading Error
echo ====================================
echo.

echo [1/6] Stopping all Gradle processes...
call gradlew --stop
taskkill /f /im java.exe 2>nul
timeout /t 3 >nul

echo.
echo [2/6] Clearing ALL caches...
rmdir /s /q .gradle 2>nul
rmdir /s /q app\.gradle 2>nul
rmdir /s /q app\build 2>nul
rmdir /s /q build 2>nul
rmdir /s /q "%USERPROFILE%\.gradle\caches\modules-2" 2>nul

echo.
echo [3/6] Removing problematic gradle.properties entries temporarily...
move gradle.properties gradle.properties.backup 2>nul
echo org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8 > gradle.properties
echo android.useAndroidX=true >> gradle.properties
echo android.enableJetifier=false >> gradle.properties
echo kotlin.code.style=official >> gradle.properties
echo kapt.incremental.apt=false >> gradle.properties
echo kapt.use.worker.api=false >> gradle.properties

echo.
echo [4/6] Downloading dependencies fresh...
call gradlew :app:dependencies --refresh-dependencies

echo.
echo [5/6] Running build with detailed logging...
call gradlew :app:assembleDebug --no-daemon --no-build-cache --refresh-dependencies --info

echo.
echo [6/6] Restoring original gradle.properties...
del gradle.properties
move gradle.properties.backup gradle.properties 2>nul

echo.
echo ====================================
echo Build completed with exit code: %ERRORLEVEL%
echo ====================================
pause