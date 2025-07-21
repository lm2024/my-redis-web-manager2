@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM Redis Web GUI 完整启动脚本 (Windows版本)
REM 作者: Redis Web GUI Team
REM 版本: 1.0.0

echo ================================
echo   Redis Web GUI 完整启动脚本
echo ================================
echo.

REM 检查Docker是否安装
docker --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker 未安装，请先安装 Docker Desktop
    pause
    exit /b 1
)

docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker Compose 未安装，请先安装 Docker Compose
    pause
    exit /b 1
)

REM 解析命令
set "command=%1"
if "%command%"=="" set "command=help"

if "%command%"=="start" goto :start
if "%command%"=="start-all" goto :start_all
if "%command%"=="stop" goto :stop
if "%command%"=="restart" goto :restart
if "%command%"=="status" goto :status
if "%command%"=="logs" goto :logs
if "%command%"=="clean" goto :clean
if "%command%"=="help" goto :help
goto :unknown_command

:start
echo [INFO] 启动Redis服务和Web应用...
call start-redis.bat start
call :start_application
call :show_status
goto :end

:start_all
echo [INFO] 启动所有服务...
call start-redis.bat start-all
call :start_application
call :show_status
goto :end

:stop
echo [INFO] 停止所有服务...
docker-compose down
docker stop redis-web-gui 2>nul
docker rm redis-web-gui 2>nul
echo [INFO] 所有服务已停止
goto :end

:restart
echo [INFO] 重启所有服务...
call :stop
timeout /t 2 /nobreak >nul
call start-redis.bat start
call :start_application
call :show_status
goto :end

:status
call :show_status
goto :end

:logs
set "service=%2"
if "%service%"=="" set "service=redis-web-gui"
echo [INFO] 查看 %service% 日志...
docker logs -f %service%
goto :end

:clean
echo [WARNING] 此操作将删除所有数据，确定继续吗？(Y/N)
set /p "response="
if /i "%response%"=="Y" (
    echo [INFO] 清理所有数据...
    docker-compose down -v
    docker volume rm redis-web-gui_redis_data 2>nul
    docker volume rm redis-web-gui_redisinsight_data 2>nul
    echo [INFO] 数据清理完成
) else (
    echo [INFO] 操作已取消
)
goto :end

:help
echo 用法: %0 [命令]
echo.
echo 命令:
echo   start        启动Redis服务和Web应用
echo   start-all    启动所有服务（包括管理工具）
echo   stop         停止所有服务
echo   restart      重启所有服务
echo   status       查看服务状态
echo   logs [服务]   查看服务日志（默认redis-web-gui）
echo   clean        清理所有数据
echo   help         显示此帮助信息
echo.
echo 示例:
echo   %0 start        # 启动基础服务
echo   %0 start-all    # 启动所有服务
echo   %0 status       # 查看状态
echo   %0 logs         # 查看应用日志
goto :end

:unknown_command
echo [ERROR] 未知命令: %command%
call :help
exit /b 1

:start_application
echo [INFO] 构建并启动Redis Web GUI应用...
docker build -t redis-web-gui .
docker stop redis-web-gui 2>nul
docker rm redis-web-gui 2>nul
docker run -d --name redis-web-gui -p 8080:8080 --network redis-web-gui_redis-network redis-web-gui
echo [INFO] 应用启动完成
goto :eof

:show_status
echo [INFO] 服务状态:
echo.

REM Redis服务状态
docker ps | findstr redis-server >nul
if errorlevel 1 (
    echo [ERROR] ✗ Redis服务未运行
) else (
    echo [INFO] ✓ Redis服务运行中
)

REM Web应用状态
docker ps | findstr redis-web-gui >nul
if errorlevel 1 (
    echo [ERROR] ✗ Web应用未运行
) else (
    echo [INFO] ✓ Web应用运行中
)

REM Redis Commander状态
docker ps | findstr redis-commander >nul
if errorlevel 1 (
    echo [WARNING] ○ Redis Commander未启动
) else (
    echo [INFO] ✓ Redis Commander运行中
)

REM Redis Insight状态
docker ps | findstr redis-insight >nul
if errorlevel 1 (
    echo [WARNING] ○ Redis Insight未启动
) else (
    echo [INFO] ✓ Redis Insight运行中
)

echo.
echo [INFO] 访问地址:
echo   - Redis Web GUI: http://localhost:8080/redis-gui
echo   - H2控制台: http://localhost:8080/redis-gui/h2-console
echo   - Redis Commander: http://localhost:8081
echo   - Redis Insight: http://localhost:8001
goto :eof

:end
echo.
pause 