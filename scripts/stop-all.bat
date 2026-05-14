@echo off
setlocal EnableExtensions

set "BACKEND_PORT=8081"
set "FRONTEND_PORT=5173"

echo [DocFlow AI] Stopping backend on port %BACKEND_PORT%...
call :kill_port %BACKEND_PORT%

echo [DocFlow AI] Stopping frontend on port %FRONTEND_PORT%...
call :kill_port %FRONTEND_PORT%

echo [DocFlow AI] Closing launcher windows if they are still open...
taskkill /f /t /fi "WINDOWTITLE eq DocFlow AI Backend*" >nul 2>nul
taskkill /f /t /fi "WINDOWTITLE eq DocFlow AI Frontend*" >nul 2>nul

echo [DocFlow AI] Stop commands completed.
echo [DocFlow AI] If a service was not running, it was skipped.
exit /b 0

:kill_port
set "TARGET_PORT=%~1"
set "FOUND_PID="

for /f "tokens=5" %%I in ('netstat -ano ^| findstr /r /c:":%TARGET_PORT% .*LISTENING"') do (
  set "FOUND_PID=%%I"
  echo [DocFlow AI] Terminating PID %%I on port %TARGET_PORT%...
  taskkill /f /t /pid %%I >nul 2>nul
)

if not defined FOUND_PID (
  echo [DocFlow AI] No listening process found on port %TARGET_PORT%.
)

exit /b 0
