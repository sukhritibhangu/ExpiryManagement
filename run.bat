@echo off
setlocal

:: Try system java first, fallback to user-installed JDK 17
where java >nul 2>nul
if %ERRORLEVEL% equ 0 (
    java -cp bin app.Main
) else if exist "%USERPROFILE%\.jdk\jdk-17.0.20.1+1\bin\java.exe" (
    "%USERPROFILE%\.jdk\jdk-17.0.20.1+1\bin\java.exe" -cp bin app.Main
) else (
    echo [Error] Java not found! Please ensure JDK 17+ is installed and on your PATH.
    pause
)
