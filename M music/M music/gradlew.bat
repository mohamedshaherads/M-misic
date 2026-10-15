@echo off
setlocal
set GRADLE_VERSION=8.9
set BASE=%USERPROFILE%\.gradle\manual-gradle
set GRADLE_HOME=%BASE%\gradle-%GRADLE_VERSION%
if exist "%GRADLE_HOME%\bin\gradle.bat" goto run
if not exist "%BASE%" mkdir "%BASE%"
set ZIP=%BASE%\gradle-%GRADLE_VERSION%-bin.zip
if not exist "%ZIP%" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ZIP%'"
  if errorlevel 1 exit /b 1
)
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ZIP%' '%BASE%'"
if errorlevel 1 exit /b 1
:run
call "%GRADLE_HOME%\bin\gradle.bat" %*
endlocal
