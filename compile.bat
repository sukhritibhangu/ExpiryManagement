@echo off
setlocal

:: Try system javac first, fallback to user-installed JDK 17
where javac >nul 2>nul
if %ERRORLEVEL% equ 0 (
    javac -d bin src\model\*.java src\service\*.java src\storage\*.java src\util\*.java src\app\*.java src\web\*.java
    echo [Success] Compilation complete! Output placed in bin\ directory.
) else if exist "%USERPROFILE%\.jdk\jdk-17.0.20.1+1\bin\javac.exe" (
    "%USERPROFILE%\.jdk\jdk-17.0.20.1+1\bin\javac.exe" -d bin src\model\*.java src\service\*.java src\storage\*.java src\util\*.java src\app\*.java src\web\*.java
    echo [Success] Compilation complete using JDK 17! Output placed in bin\ directory.
) else (
    echo [Error] javac not found! Please ensure JDK 17+ is installed and on your PATH.
    pause
)
