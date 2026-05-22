@echo off
setlocal EnableExtensions
call "%~dp0env.bat"

echo ============================================================
echo  DocFlow AI - Frontend
echo ============================================================
echo  Port    : %DOCFLOW_FRONTEND_PORT%
echo  Node    : %DOCFLOW_NODE_VERSION%
echo ============================================================
echo.

rem Ensure correct Node version
fnm use %DOCFLOW_NODE_VERSION%
if errorlevel 1 (
    echo [ERROR] Node %DOCFLOW_NODE_VERSION% not found.
    echo         Run: scripts\setup.bat
    pause
    exit /b 1
)

cd /d "%DOCFLOW_ROOT%\frontend"
npm run dev -- --host 127.0.0.1 --port %DOCFLOW_FRONTEND_PORT%
