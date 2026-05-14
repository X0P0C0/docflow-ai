@echo off
setlocal EnableExtensions

set "PROJECT_ROOT=D:\java\project\docflow-ai"
set "JAVA_HOME=D:\develop\java\jdk-17"
set "MAVEN_HOME=D:\develop\java\maven\apache-maven-3.9.0-bin\apache-maven-3.9.0"

cd /d "%PROJECT_ROOT%\backend"
set "JAVA_HOME=%JAVA_HOME%"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

mvn spring-boot:run
