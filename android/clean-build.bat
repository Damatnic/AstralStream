@echo off
echo Starting clean build process for Astral Stream...

echo Stopping Gradle daemon...
call gradlew --stop

echo Cleaning project...
call gradlew clean

echo Clear Gradle cache? (Y/N)
set /p response=
if /i "%response%"=="y" (
    echo Clearing Gradle cache...
    rmdir /s /q %USERPROFILE%\.gradle\caches
    rmdir /s /q .gradle
    rmdir /s /q app\build
)

echo Clearing local caches...
rmdir /s /q .idea
del /q *.iml
del /q app\*.iml

echo Syncing project...
call gradlew --refresh-dependencies

echo Building debug APK...
call gradlew assembleDebug --stacktrace

if %errorlevel% equ 0 (
    echo Build successful! APK location:
    dir /s /b app\build\outputs\apk\debug\*.apk
) else (
    echo Build failed. Check the error messages above.
    echo Try running with --info flag for more details:
    echo    gradlew assembleDebug --info --stacktrace
)

pause