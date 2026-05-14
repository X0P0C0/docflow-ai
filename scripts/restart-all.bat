@echo off
setlocal EnableExtensions

set "PROJECT_ROOT=D:\java\project\docflow-ai"

echo [DocFlow AI] Restarting services...
call "%PROJECT_ROOT%\scripts\stop-all.bat"

echo [DocFlow AI] Waiting for ports to be released...
timeout /t 2 /nobreak >nul

call "%PROJECT_ROOT%\scripts\start-all.bat"
