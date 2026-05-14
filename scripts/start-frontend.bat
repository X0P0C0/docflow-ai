@echo off
setlocal EnableExtensions

set "PROJECT_ROOT=D:\java\project\docflow-ai"
set "FRONTEND_DIR=%PROJECT_ROOT%\frontend"
set "NODE_VERSION=20"
set "FNM_EXE=D:\develop\fnm\fnm.exe"

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

for /f "delims=" %%i in ('"%FNM_EXE%" env --shell cmd') do call %%i
call "%FNM_EXE%" use %NODE_VERSION%
if errorlevel 1 exit /b 1

cd /d "%FRONTEND_DIR%"
call npm.cmd run dev -- --host 127.0.0.1 --port 5173
