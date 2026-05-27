@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "APP_HOME=%%~fI"
set "PID_FILE=%APP_HOME%\.run-logs\backend.pid"

if not exist "%PID_FILE%" (
  echo PID file not found: %PID_FILE%
  exit /b 1
)

set /p TARGET_PID=<"%PID_FILE%"
if "%TARGET_PID%"=="" (
  echo PID file is empty.
  exit /b 1
)

taskkill /PID %TARGET_PID% /F
if errorlevel 1 exit /b 1

del /Q "%PID_FILE%"
echo Backend stopped. PID=%TARGET_PID%
