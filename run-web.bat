@echo off
setlocal

echo [1/2] Compiling Java classes...
call compile.bat

echo [2/2] Launching Expiry Alert System Web Server...
start "" "http://localhost:8080"

where java >nul 2>nul
if %ERRORLEVEL% equ 0 (
    java -cp bin web.WebServer
) else if exist "%USERPROFILE%\.jdk\jdk-17.0.20.1+1\bin\java.exe" (
    "%USERPROFILE%\.jdk\jdk-17.0.20.1+1\bin\java.exe" -cp bin web.WebServer
) else (
    echo [Error] Java not found! Please ensure JDK 17+ is installed.
    pause
)
