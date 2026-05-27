@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "APP_HOME=%%~fI"
set "JAR_PATH=%APP_HOME%\backend\target\ai-love-daily-1.0.0.jar"
set "LOG_DIR=%APP_HOME%\.run-logs"
set "PID_FILE=%LOG_DIR%\backend.pid"
set "ENV_FILE=%SCRIPT_DIR%.env.bat"

if exist "%ENV_FILE%" call "%ENV_FILE%"

if not defined SPRING_PROFILE set "SPRING_PROFILE=prod"
if not defined JAVA_OPTS set "JAVA_OPTS=-Xms256m -Xmx512m"
if not defined FILE_UPLOAD_PATH set "FILE_UPLOAD_PATH=%APP_HOME%\data\uploads"

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
if not exist "%FILE_UPLOAD_PATH%" mkdir "%FILE_UPLOAD_PATH%"

if not exist "%JAR_PATH%" (
  echo Jar not found: %JAR_PATH%
  exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$proc = Start-Process -FilePath 'java' -ArgumentList @('%JAVA_OPTS%'.Split(' ') + @('-jar','%JAR_PATH%','--spring.profiles.active=%SPRING_PROFILE%')) -WorkingDirectory '%APP_HOME%' -RedirectStandardOutput '%LOG_DIR%\backend.out.log' -RedirectStandardError '%LOG_DIR%\backend.err.log' -PassThru; Set-Content -Path '%PID_FILE%' -Value $proc.Id; Write-Output ('Backend started. PID=' + $proc.Id)"

if errorlevel 1 exit /b 1

echo Logs: %LOG_DIR%\backend.out.log
