@echo off
echo Stopping Gradle daemons...
call gradlew --stop

echo.
echo Cleaning build directories...
rmdir /s /q app\build 2>nul

echo.
echo Building project...
call gradlew :app:assembleDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --no-daemon --stacktrace

echo.
echo Build completed with exit code: %ERRORLEVEL%
pause