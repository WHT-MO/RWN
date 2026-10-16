@echo off
rem 将本地《铁锈战争》安装目录中的运行 jar 复制到 lib/，供构建使用
rem 游戏 jar 为版权物，禁止提交到 git 仓库
setlocal
set GAME_DIR=D:\steam\steamapps\common\Rusted Warfare
if not exist "%GAME_DIR%\game-lib.jar" (
    echo [错误] 未找到游戏目录: %GAME_DIR%
    echo 请编辑本脚本修改 GAME_DIR 为你的 Rusted Warfare 安装路径
    exit /b 1
)
if not exist lib mkdir lib
copy /y "%GAME_DIR%\game-lib.jar" lib\game-lib.jar
echo 完成。lib\game-lib.jar 已就绪。
endlocal
