@echo off
setlocal EnableExtensions

set "PROJECT_ROOT=D:\java\project\docflow-ai"
set "JAVA_HOME=D:\develop\java\jdk-17"
set "MAVEN_HOME=D:\develop\java\maven\apache-maven-3.9.0-bin\apache-maven-3.9.0"
set "NODE_VERSION=20"
set "FRONTEND_DIR=%PROJECT_ROOT%\frontend"
set "NODE_MARKER=%FRONTEND_DIR%\.node-runtime-version"
set "FNM_EXE=D:\develop\fnm\fnm.exe"

if not exist "%FRONTEND_DIR%" (
  echo [DocFlow AI] Project not found: %PROJECT_ROOT%
  exit /b 1
)

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo [DocFlow AI] JDK 17 not found: %JAVA_HOME%
  exit /b 1
)

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  echo [DocFlow AI] Maven not found: %MAVEN_HOME%
  exit /b 1
)

if not exist "%FNM_EXE%" (
  where fnm >nul 2>nul
  if not errorlevel 1 (
    for /f "delims=" %%i in ('where fnm') do set "FNM_EXE=%%i"
  )
)

if not exist "%FNM_EXE%" (
  echo [DocFlow AI] fnm not found.
  echo [DocFlow AI] Checked: D:\develop\fnm\fnm.exe and PATH.
  exit /b 1
)

echo [DocFlow AI] Using fnm: %FNM_EXE%
echo [DocFlow AI] Ensuring Node %NODE_VERSION% is available...
call "%FNM_EXE%" install %NODE_VERSION%
if errorlevel 1 exit /b 1

for /f "delims=" %%i in ('"%FNM_EXE%" env --shell cmd') do call %%i
call "%FNM_EXE%" use %NODE_VERSION%
if errorlevel 1 exit /b 1

for /f %%i in ('node -v') do set "ACTIVE_NODE=%%i"
echo [DocFlow AI] Active Node version: %ACTIVE_NODE%

set "NEED_REINSTALL=0"
if not exist "%FRONTEND_DIR%\node_modules" set "NEED_REINSTALL=1"
if not exist "%FRONTEND_DIR%\package-lock.json" set "NEED_REINSTALL=1"
if not exist "%NODE_MARKER%" set "NEED_REINSTALL=1"
if exist "%NODE_MARKER%" (
  set /p INSTALLED_NODE=<"%NODE_MARKER%"
  if /I not "%INSTALLED_NODE%"=="%NODE_VERSION%" set "NEED_REINSTALL=1"
)

if "%NEED_REINSTALL%"=="1" (
  echo [DocFlow AI] Refreshing frontend dependencies for Node %NODE_VERSION%...
  echo [DocFlow AI] This is expected the first time you switch this project to Node %NODE_VERSION%.
  echo [DocFlow AI] Closing possible old frontend processes...
  taskkill /f /im esbuild.exe >nul 2>nul
  taskkill /f /im node.exe >nul 2>nul
  timeout /t 1 /nobreak >nul
  if exist "%FRONTEND_DIR%\node_modules" rmdir /s /q "%FRONTEND_DIR%\node_modules"
  if exist "%FRONTEND_DIR%\package-lock.json" del /f /q "%FRONTEND_DIR%\package-lock.json"
  cd /d "%FRONTEND_DIR%"
  call npm install
  if errorlevel 1 exit /b 1
  > "%NODE_MARKER%" echo %NODE_VERSION%
) else (
  echo [DocFlow AI] Frontend dependencies already match Node %NODE_VERSION%.
)

echo [DocFlow AI] Launching backend and frontend...
echo [DocFlow AI] Backend: http://127.0.0.1:8081
echo [DocFlow AI] Frontend: http://127.0.0.1:5173
echo.

start "DocFlow AI Backend" "%PROJECT_ROOT%\scripts\start-backend.bat"
timeout /t 3 /nobreak >nul
start "DocFlow AI Frontend" "%PROJECT_ROOT%\scripts\start-frontend.bat"

echo [DocFlow AI] Startup commands sent.
echo [DocFlow AI] This launcher can be closed.
