@echo off
setlocal
cd /d "%~dp0"

echo Building release APK...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1" -Tasks assembleRelease
set "BUILD_EXIT=%ERRORLEVEL%"

echo.
if "%BUILD_EXIT%"=="0" (
    echo Release build completed successfully.
    echo Output directory: %~dp0app\build\outputs\apk\release
) else (
    echo Release build failed with exit code %BUILD_EXIT%.
)

echo.
pause
exit /b %BUILD_EXIT%
