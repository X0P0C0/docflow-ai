@echo off
setlocal

echo ============================================================
echo  DocFlow AI - Starting All Services
echo ============================================================
echo.

rem Set all paths
set "JAVA_HOME=D:\develop\java\jdk-17"
set "PATH=%JAVA_HOME%\bin;D:\develop\java\maven\apache-maven-3.9.0-bin\apache-maven-3.9.0\bin;%APPDATA%\fnm\node-versions\v22.22.3\installation;C:\Windows\system32;C:\Windows"

rem Start Backend
echo [1/2] Starting Backend on port 8081...
cd /d "%~dp0..\backend"
start "DocFlow Backend" cmd /k "mvn spring-boot:run"

rem Wait for backend
echo Waiting for backend to start...
timeout /t 20 /nobreak > nul

rem Start Frontend
echo [2/2] Starting Frontend on port 8900...
cd /d "%~dp0..\frontend"
start "DocFlow Frontend" cmd /k "call pnpm dev"

echo.
echo ============================================================
echo  Services starting...
echo  Backend:  http://localhost:8081
echo  Frontend: http://localhost:8900
echo ============================================================
echo.
pause
