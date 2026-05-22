@echo off
setlocal EnableExtensions
call "%~dp0env.bat"

echo.
echo ============================================================
echo  DocFlow AI - Starting All Services
echo ============================================================
echo.

rem ---- Preflight: Java ----
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERROR] JDK not found at %JAVA_HOME%
    echo         Run scripts\setup.bat first, or update scripts\env.bat
    exit /b 1
)

rem ---- Preflight: Maven ----
where mvn.cmd >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Maven not found. Run scripts\setup.bat first.
    exit /b 1
)

rem ---- Preflight: Node.js + fnm ----
where fnm.exe >nul 2>nul
if errorlevel 1 (
    echo [ERROR] fnm not found. Run scripts\setup.bat first.
    exit /b 1
)

fnm use %DOCFLOW_NODE_VERSION% >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Node %DOCFLOW_NODE_VERSION% is not installed.
    echo         Run: scripts\setup.bat
    exit /b 1
)

rem ---- Preflight: Frontend dependencies ----
if not exist "%DOCFLOW_ROOT%\frontend\node_modules" (
    echo [WARN]  node_modules not found.
    echo         Running one-time install...
    call npm install --prefix "%DOCFLOW_ROOT%\frontend"
    if errorlevel 1 (
        echo [ERROR] npm install failed. Check your network and try again.
        exit /b 1
    )
)

rem ---- Start services ----
echo [INFO]  Starting backend on port %DOCFLOW_BACKEND_PORT% ...
start "DocFlow AI - Backend" "%DOCFLOW_ROOT%\scripts\start-backend.bat"

echo [INFO]  Starting frontend on port %DOCFLOW_FRONTEND_PORT% ...
start "DocFlow AI - Frontend" "%DOCFLOW_ROOT%\scripts\start-frontend.bat"

echo.
echo ------------------------------------------------------------
echo  Backend  : http://127.0.0.1:%DOCFLOW_BACKEND_PORT%
echo  Swagger  : http://127.0.0.1:%DOCFLOW_BACKEND_PORT%/swagger-ui.html
echo  Frontend : http://127.0.0.1:%DOCFLOW_FRONTEND_PORT%
echo ------------------------------------------------------------
echo.
echo  Backend takes ~15-30 seconds on first launch (Maven download).
echo  Frontend is ready in ~3-5 seconds.
echo.
echo  Run scripts\stop-all.bat to shut everything down.
echo.
