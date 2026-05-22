@echo off
setlocal EnableExtensions
call "%~dp0env.bat"

echo ============================================================
echo  DocFlow AI - Stopping All Services
echo ============================================================
echo.

rem ---- Stop by port using PowerShell (much more reliable than netstat) ----
for %%P in (%DOCFLOW_BACKEND_PORT% %DOCFLOW_FRONTEND_PORT%) do (
    echo [INFO]  Stopping process on port %%P ...
    powershell -NoProfile -Command ^
        "$pids = (Get-NetTCPConnection -LocalPort %%P -ErrorAction SilentlyContinue).OwningProcess | Sort-Object -Unique;" ^
        "if ($pids) { $pids | ForEach-Object { Write-Host \"         Killing PID $_\" ; Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue } }" ^
        "else { Write-Host '         No process found on this port.' }"
)

rem ---- Close launcher windows ----
echo.
echo [INFO]  Closing launcher windows...
taskkill /f /t /fi "WINDOWTITLE eq DocFlow AI - Backend*" >nul 2>nul
taskkill /f /t /fi "WINDOWTITLE eq DocFlow AI - Frontend*" >nul 2>nul

echo.
echo [INFO]  All services stopped.
