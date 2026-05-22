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

rem Use fnm exec for reliable Node binary resolution
fnm exec --using=%DOCFLOW_NODE_VERSION% node -v >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Node %DOCFLOW_NODE_VERSION% not found.
    echo         Run: scripts\setup.bat
    pause
    exit /b 1
)

cd /d "%DOCFLOW_ROOT%\frontend"
fnm exec --using=%DOCFLOW_NODE_VERSION% npm.cmd run dev -- --host 127.0.0.1 --port %DOCFLOW_FRONTEND_PORT%
if errorlevel 1 (
    echo.
    echo [ERROR] Frontend failed to start. Check the output above for details.
    pause
)
