@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
cd /d "%~dp0"

set "APK=app\build\outputs\apk\debug\app-debug.apk"

echo ================================================================
echo   简易记账 - 安装到手机（需要 USB 调试已开启）
echo ================================================================
echo.

if not exist "%APK%" (
    echo [错误] 还没构建出 APK：%APK%
    echo        请先双击 build-apk.bat。
    echo.
    pause
    exit /b 1
)

rem ---- 找一个可用的 adb：优先 PATH，其次常见 SDK 目录 ----
set "ADB=adb"
where adb >nul 2>nul
if errorlevel 1 (
    set "ADB="
    for %%D in (
        "%LOCALAPPDATA%\Android\Sdk"
        "C:\Android\Sdk"
        "%USERPROFILE%\AppData\Local\Android\Sdk"
        "C:\Program Files (x86)\Android\android-sdk"
    ) do (
        if exist "%%~D\platform-tools\adb.exe" set "ADB=%%~D\platform-tools\adb.exe"
    )
)

if not defined ADB (
    echo [错误] 找不到 adb.exe。
    echo        它随 Android SDK 一起安装，一般在：
    echo        %%LOCALAPPDATA%%\Android\Sdk\platform-tools\adb.exe
    echo        也可以直接用 Android Studio 顶部的设备列表里点 Run 按钮安装。
    echo.
    pause
    exit /b 1
)

echo 使用 adb: %ADB%
echo.
echo 已连接的设备：
"%ADB%" devices
echo.

echo 正在安装 %APK% ...
"%ADB%" install -r "%APK%"
if errorlevel 1 (
    echo.
    echo ================================================================
    echo [失败] 安装未成功。常见原因：
    echo   - 手机没开 USB 调试，或没在手机上点「允许」
    echo   - 手机已装同包名但签名不同的版本 -^> 先在手机上卸载「简易记账」
    echo   - 数据线只供电不传数据 -^> 换一根线 / 换一个 USB 口
    echo ================================================================
    echo.
    pause
    exit /b 1
)

echo.
echo [成功] 已安装到手机，桌面找到「简易记账」即可打开。
echo.
pause
