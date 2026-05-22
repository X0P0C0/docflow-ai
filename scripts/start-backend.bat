@echo off
setlocal EnableExtensions
call "%~dp0env.bat"

echo ============================================================
echo  DocFlow AI - Backend
echo ============================================================
echo  Port    : %DOCFLOW_BACKEND_PORT%
echo  Java    : %JAVA_HOME%
echo ============================================================
echo.

cd /d "%DOCFLOW_ROOT%\backend"
mvn spring-boot:run
if errorlevel 1 (
    echo.
    echo [ERROR] Backend failed to start. Check the output above for details.
    pause
)
