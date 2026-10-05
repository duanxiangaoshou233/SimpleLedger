@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo ================================================================
echo   简易记账 - 一键构建 debug APK
echo ================================================================
echo.

if not exist "gradlew.bat" (
    echo [错误] 找不到 gradlew.bat
    echo        说明 Gradle Wrapper 还没补齐。
    echo        请看 docs\BUILD_APK.md 的第 2 节（浏览器下载 或 借 Android Studio 生成）。
    echo.
    pause
    exit /b 1
)

if not exist "local.properties" (
    echo [提示] 缺少 local.properties（Android SDK 路径）。
    echo        Android Studio 首次 Sync 会自动生成；若手动构建，请写入一行：
    echo        sdk.dir=C:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk
    echo.
)

echo [1/2] 正在构建（首次会下载依赖，请耐心等待）...
echo.
call gradlew.bat assembleDebug
if errorlevel 1 (
    echo.
    echo ================================================================
    echo [失败] 构建未通过。请把上面第一处 "FAILURE" 附近的报错发给开发者。
    echo        常见原因见 docs\BUILD_APK.md 第 6 节。
    echo ================================================================
    echo.
    pause
    exit /b 1
)

echo.
echo ================================================================
echo [成功] APK 已生成：
echo   %cd%\app\build\outputs\apk\debug\app-debug.apk
echo.
echo 安装到手机：
echo   1) USB 直连 + 开启 USB 调试  -^> 双击 install-apk.bat
echo   2) 或把上面的 apk 拷到手机任意目录，用文件管理器点开安装
echo      （首次需允许该来源「安装未知应用」）
echo ================================================================
echo.
pause
