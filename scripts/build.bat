@echo off
rem 构建包装脚本：自动使用项目内 JDK，无需系统安装 Java
rem 用法: scripts\build.bat <gradle 任务>   例: scripts\build.bat :desktop:build
setlocal
set "JAVA_HOME=%~dp0..\tools\jdk-17.0.20.1+1"
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [错误] 未找到项目内 JDK，请确认 tools\jdk-17.0.20.1+1 存在
    exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0.."
call gradlew.bat %*
endlocal
