@echo off
title Omar Dev - Discord Activity Admin Bot
color 0b
echo ========================================================
echo   👑 OMAR DEV - DISCORD ACTIVITY STATUS ADMIN BOT 👑
echo ========================================================
echo.
echo [1/2] Checking Python environment...
python --version
if %errorlevel% neq 0 (
    echo [ERROR] Python is not installed or not in PATH!
    pause
    exit /b
)

echo [2/2] Starting Discord Admin Bot...
echo.
python bot_admin.py
pause
