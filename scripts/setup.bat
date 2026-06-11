@echo off
setlocal EnableExtensions EnableDelayedExpansion
call "%~dp0env.bat"

echo.
echo ============================================================
echo  DocFlow AI - One-Time Setup
echo ============================================================
echo.

rem ---- Check Java ----
echo [1/4] Checking Java...
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERROR] JDK not found at %JAVA_HOME%
    pause
    exit /b 1
)
"%JAVA_HOME%\bin\java.exe" -version 2>&1 | findstr /r "version"
echo         [OK]

rem ---- Check Maven ----
echo [2/4] Checking Maven...
where mvn.cmd >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Maven not found.
    pause
    exit /b 1
)
mvn.cmd --version 2>&1 | findstr /r "Apache Maven"
echo         [OK]

rem ---- Install Node ----
echo [3/4] Installing Node %DOCFLOW_NODE_VERSION%...
where fnm.exe >nul 2>nul
if errorlevel 1 (
    echo [ERROR] fnm not found.
    pause
    exit /b 1
)
fnm install %DOCFLOW_NODE_VERSION%

rem Find installed Node directory
set "NODE_DIR=%APPDATA%\fnm\node-versions\v%DOCFLOW_NODE_VERSION%.*"
for /d %%d in ("!NODE_DIR!") do set "FOUND=%%d\installation"
if not defined FOUND (
    echo [ERROR] Could not find Node installation.
    pause
    exit /b 1
)
echo         Node: !FOUND!\node.exe

rem ---- Install pnpm + deps ----
echo [4/4] Installing frontend dependencies...
call "!FOUND!\npm.cmd" install -g pnpm
cd /d "%DOCFLOW_ROOT%\frontend"
call "!FOUND!\pnpm.cmd" install
if errorlevel 1 (
    echo [ERROR] pnpm install failed.
    pause
    exit /b 1
)

cd /d "%DOCFLOW_ROOT%"
echo.
echo ============================================================
echo  Setup complete! Run: scripts\start-all.bat
echo ============================================================
echo.
pause
