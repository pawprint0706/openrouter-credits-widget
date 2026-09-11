@echo off
setlocal
cd /d "%~dp0"

echo Building debug APK and running debug unit tests...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1"
set "BUILD_EXIT=%ERRORLEVEL%"

echo.
if "%BUILD_EXIT%"=="0" (
    echo Debug build completed successfully.
    echo APK: %~dp0app\build\outputs\apk\debug\app-debug.apk
) else (
    echo Debug build failed with exit code %BUILD_EXIT%.
)

echo.
pause
exit /b %BUILD_EXIT%
