@echo off
setlocal

echo ============================================================
echo  DocFlow AI - Backend
echo ============================================================
echo.

rem Set Java and Maven paths
set "JAVA_HOME=D:\develop\java\jdk-17"
set "PATH=%JAVA_HOME%\bin;D:\develop\java\maven\apache-maven-3.9.0-bin\apache-maven-3.9.0\bin;C:\Windows\system32;C:\Windows"

rem Go to backend directory
cd /d "%~dp0..\backend"

rem Show versions
java -version
echo.
mvn --version
echo.

rem Run Spring Boot
echo Starting Spring Boot...
mvn spring-boot:run

pause
