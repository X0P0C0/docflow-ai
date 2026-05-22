@echo off
setlocal EnableExtensions
call "%~dp0env.bat"

echo.
echo ============================================================
echo  DocFlow AI - One-Time Setup
echo ============================================================
echo.
echo This script installs project dependencies.
echo Run this once after cloning the repo or when package.json changes.
echo.

rem ---- Check Java ----
echo [1/3] Checking Java...
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERROR] JDK not found at %JAVA_HOME%
    echo         Please install JDK 17 and update JAVA_HOME in scripts\env.bat
    pause
    exit /b 1
)
for /f "tokens=*" %%v in ('"%JAVA_HOME%\bin\java.exe" -version 2^>^&1') do (
    if not defined JAVA_VER set "JAVA_VER=%%v"
)
echo         %JAVA_VER%
echo         [OK]

rem ---- Check Maven ----
echo [2/3] Checking Maven...
where mvn.cmd >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Maven not found in PATH or env.bat fallback paths.
    echo         Please install Maven 3.9+ and update scripts\env.bat
    pause
    exit /b 1
)
for /f "tokens=*" %%v in ('mvn.cmd --version 2^>^&1 ^| findstr /r "Apache Maven"') do (
    if not defined MVN_VER set "MVN_VER=%%v"
)
echo         %MVN_VER%
echo         [OK]

rem ---- Check Node.js ----
echo [3/3] Checking Node.js + installing frontend dependencies...
where fnm.exe >nul 2>nul
if errorlevel 1 (
    echo [ERROR] fnm not found.
    echo         Please install fnm and update scripts\env.bat
    pause
    exit /b 1
)

rem Ensure correct Node version is installed (one-time download if needed)
fnm install %DOCFLOW_NODE_VERSION%
if errorlevel 1 (
    echo [ERROR] Failed to install Node %DOCFLOW_NODE_VERSION% via fnm
    pause
    exit /b 1
)

fnm use %DOCFLOW_NODE_VERSION%
if errorlevel 1 (
    echo [ERROR] Failed to switch to Node %DOCFLOW_NODE_VERSION%
    pause
    exit /b 1
)

for /f "tokens=*" %%v in ('node -v') do echo         Node version: %%v

echo         Installing frontend dependencies...
cd /d "%DOCFLOW_ROOT%\frontend"
call npm install
if errorlevel 1 (
    echo [ERROR] npm install failed
    pause
    exit /b 1
)

cd /d "%DOCFLOW_ROOT%"
echo.
echo ============================================================
echo  Setup complete. You can now run: scripts\start-all.bat
echo ============================================================
echo.
echo  Press any key to close this window...
pause >nul
