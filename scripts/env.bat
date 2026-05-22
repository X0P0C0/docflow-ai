@echo off
rem ============================================================
rem DocFlow AI - Shared Environment Configuration
rem Source this file from other scripts: call "%~dp0env.bat"
rem ============================================================

rem ---- Project root (derived from script location) ----
set "DOCFLOW_ROOT=%~dp0.."

rem ---- Java ----
if not defined JAVA_HOME set "JAVA_HOME=D:\develop\java\jdk-17"
set "PATH=%JAVA_HOME%\bin;%PATH%"

rem ---- Maven (prefer PATH, fallback to fixed location) ----
where mvn.cmd >nul 2>nul
if errorlevel 1 (
    if exist "D:\develop\java\maven\apache-maven-3.9.0-bin\apache-maven-3.9.0\bin\mvn.cmd" (
        set "PATH=D:\develop\java\maven\apache-maven-3.9.0-bin\apache-maven-3.9.0\bin;%PATH%"
    )
)

rem ---- Node.js version ----
set "DOCFLOW_NODE_VERSION=20"

rem ---- fnm (prefer PATH, fallback to fixed location) ----
where fnm.exe >nul 2>nul
if errorlevel 1 (
    if exist "D:\develop\fnm\fnm.exe" (
        set "PATH=D:\develop\fnm;%PATH%"
    )
)

rem ---- Service ports ----
set "DOCFLOW_BACKEND_PORT=8081"
set "DOCFLOW_FRONTEND_PORT=5173"
