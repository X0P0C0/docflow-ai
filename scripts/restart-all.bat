@echo off
setlocal EnableExtensions
call "%~dp0env.bat"

echo ============================================================
echo  DocFlow AI - Restarting All Services
echo ============================================================
echo.

call "%~dp0stop-all.bat"

echo.
echo Waiting 2 seconds for ports to release...
timeout /t 2 /nobreak >nul

call "%~dp0start-all.bat"
