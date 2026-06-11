@echo off
setlocal

echo ============================================================
echo  DocFlow AI - Frontend
echo ============================================================
echo.

rem Set Node.js v22 path directly
set "PATH=%APPDATA%\fnm\node-versions\v22.22.3\installation;C:\Windows\system32;C:\Windows"

rem Go to frontend directory
cd /d "%~dp0..\frontend"

rem Show node version
node --version
echo.

rem Run pnpm dev
call pnpm dev

pause
