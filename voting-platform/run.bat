@echo off
REM SecureVote 2026 Startup Script for Windows

echo.
echo ================================
echo SecureVote 2026 Platform
echo ================================
echo.

REM Check if Maven is installed
where mvn >nul 2>nul
if errorlevel 1 (
    echo ERROR: Maven is not installed or not in PATH
    echo Please install Maven from: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

REM Build project
echo [1/3] Building project...
call mvn clean install -q
if errorlevel 1 (
    echo ERROR: Build failed
    pause
    exit /b 1
)
echo Build successful!

echo.
echo [2/3] Starting VotingSocketServer...
echo Press Ctrl+C to stop server
echo.
start cmd /k "cd %cd% && java -cp target/classes org.example.client.core.VotingSocketServer"

echo.
echo [3/3] Waiting for server to start (3 seconds)...
timeout /t 3 /nobreak

echo.
echo Starting JavaFX Application...
echo.
call mvn javafx:run

pause
