@echo off
REM ========================================
REM   启动所有后端服务
REM   Windows 启动脚本
REM ========================================

cd /d %~dp0

echo.
echo ========================================
echo   Starting All Backend Services
echo ========================================
echo.

echo [1/3] Starting Auth Service (Port 8083)...
start cmd /k "cd services\auth\auth-api && mvn spring-boot:run"
timeout /t 10 /nobreak > nul

echo [2/3] Starting Knowledge Mentor Service (Port 8080)...
start cmd /k "cd services\knowledge-mentor\km-api && mvn spring-boot:run"
timeout /t 10 /nobreak > nul

echo [3/3] Starting Code Intelligence Service (Port 8081)...
start cmd /k "cd services\code-intelligence\ci-api && mvn spring-boot:run"

echo.
echo ========================================
echo   All services starting...
echo   Check each window for status
echo ========================================
echo.
echo Service URLs:
echo   - Auth Service:        http://localhost:8083
echo   - Knowledge Mentor:    http://localhost:8080
echo   - Code Intelligence:    http://localhost:8081
echo.
pause
