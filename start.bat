@echo off
title omar dev - Discord Rich Presence Manager
color 0B

echo ===================================================
echo  omar dev - Discord Rich Presence Manager
echo ===================================================
echo.
echo [1/2] Checking Python environment...

set "PY_CMD=python"
where python >nul 2>nul
if %errorlevel% neq 0 (
    where py >nul 2>nul
    if %errorlevel% neq 0 (
        color 0C
        echo [X] Error: Python is not installed or not in system PATH!
        echo Please download and install Python from https://www.python.org/
        echo Make sure to check "Add Python to PATH" during installation.
        echo.
        pause
        exit /b 1
    ) else (
        set "PY_CMD=py"
    )
)

echo [OK] Found Python command: %PY_CMD%
echo.
echo [2/2] Checking and installing required dependencies...
%PY_CMD% -m pip install -r requirements.txt --quiet --disable-pip-version-check

echo.
echo [OK] Environment ready! Launching Omar Dev GUI...
echo ===================================================
%PY_CMD% gui_app.py
if %errorlevel% neq 0 (
    echo.
    echo [X] Application exited with error code %errorlevel%!
    pause
)
