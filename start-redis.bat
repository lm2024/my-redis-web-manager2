@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: Redis Docker Compose 管理脚本 (Windows版本)
:: 作者: Redis Web GUI Team
:: 版本: 1.0.0

echo ================================
echo   Redis Docker Compose 管理工具
echo ================================
echo.

:: 检查Docker是否安装
docker --version >nul 2>&1
if errorlevel 1 (
    echo [错误] Docker 未安装，请先安装 Docker Desktop
    pause
    exit /b 1
)

docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo [错误] Docker Compose 未安装，请先安装 Docker Compose
    pause
    exit /b 1
)

:: 创建备份目录
if not exist "backup" (
    mkdir backup
    echo [信息] 创建备份目录: backup
)

:: 解析命令
set "command=%1"
if "%command%"=="" set "command=help"

if "%command%"=="start" goto start_redis
if "%command%"=="start-all" goto start_all
if "%command%"=="start-cluster" goto start_cluster
if "%command%"=="stop" goto stop_services
if "%command%"=="restart" goto restart_services
if "%command%"=="status" goto status
if "%command%"=="logs" goto logs
if "%command%"=="shell" goto shell
if "%command%"=="backup" goto backup
if "%command%"=="clean" goto clean
if "%command%"=="help" goto show_help
goto unknown_command

:start_redis
echo [信息] 启动Redis服务...
docker-compose up -d redis
echo [信息] Redis服务启动完成
goto end

:start_all
echo [信息] 启动所有Redis相关服务...
docker-compose up -d
echo [信息] 所有服务启动完成
goto end

:start_cluster
echo [信息] 启动Redis集群模式...
docker-compose --profile cluster up -d redis-cluster
echo [信息] Redis集群启动完成
echo [警告] 请手动初始化集群配置
goto end

:stop_services
echo [信息] 停止所有服务...
docker-compose down
echo [信息] 所有服务已停止
goto end

:restart_services
echo [信息] 重启所有服务...
docker-compose restart
echo [信息] 所有服务重启完成
goto end

:status
echo [信息] 查看服务状态...
docker-compose ps
goto end

:logs
set "service=%2"
if "%service%"=="" set "service=redis"
echo [信息] 查看 %service% 服务日志...
docker-compose logs -f %service%
goto end

:shell
echo [信息] 进入Redis容器...
docker exec -it redis-server redis-cli
goto end

:backup
echo [信息] 创建Redis数据备份...
docker exec redis-server redis-cli BGSAVE
timeout /t 2 /nobreak >nul
for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value') do set "dt=%%a"
set "YY=%dt:~2,2%" & set "YYYY=%dt:~0,4%" & set "MM=%dt:~4,2%" & set "DD=%dt:~6,2%"
set "HH=%dt:~8,2%" & set "Min=%dt:~10,2%" & set "Sec=%dt:~12,2%"
set "datestamp=%YYYY%%MM%%DD%_%HH%%Min%%Sec%"
docker cp redis-server:/data/dump.rdb ./backup/%datestamp%_dump.rdb
echo [信息] 备份完成
goto end

:clean
echo [警告] 此操作将删除所有Redis数据，确定继续吗？(Y/N)
set /p "response="
if /i "%response%"=="Y" (
    echo [信息] 清理Redis数据...
    docker-compose down -v
    docker volume rm redis-web-gui_redis_data 2>nul
    echo [信息] 数据清理完成
) else (
    echo [信息] 操作已取消
)
goto end

:show_help
echo 用法: %0 [命令]
echo.
echo 命令:
echo   start       启动Redis主服务
echo   start-all   启动所有服务（包括管理工具）
echo   start-cluster 启动Redis集群模式
echo   stop        停止所有服务
echo   restart     重启所有服务
echo   status      查看服务状态
echo   logs [服务]  查看服务日志（默认redis）
echo   shell       进入Redis命令行
echo   backup      备份Redis数据
echo   clean       清理所有数据
echo   help        显示此帮助信息
echo.
echo 示例:
echo   %0 start        # 启动Redis服务
echo   %0 logs redis   # 查看Redis日志
echo   %0 shell        # 进入Redis命令行
goto end

:unknown_command
echo [错误] 未知命令: %command%
goto show_help

:end
echo.
pause 