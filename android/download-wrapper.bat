@echo off
echo Downloading gradle-wrapper.jar...
cd gradle\wrapper
powershell -Command "Invoke-WebRequest -Uri 'https://github.com/gradle/gradle/raw/v9.0.0-M1/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle-wrapper.jar'"
echo Done!