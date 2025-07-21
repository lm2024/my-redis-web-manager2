#!/bin/bash

# Redis Web GUI 完整启动脚本
# 作者: Redis Web GUI Team
# 版本: 1.0.0

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_message() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}  Redis Web GUI 完整启动脚本${NC}"
    echo -e "${BLUE}================================${NC}"
}

# 检查Docker是否安装
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker 未安装，请先安装 Docker"
        return 1
    fi
    
    # 检查Docker Compose（新版本）
    if ! docker compose version &> /dev/null; then
        print_error "Docker Compose 不可用，请检查Docker安装"
        return 1
    fi
    
    return 0
}

# 启动Redis服务
start_redis() {
    print_message "启动Redis服务..."
    if [ -f "start-redis.sh" ]; then
        ./start-redis.sh start
    else
        docker compose up -d redis
    fi
    print_message "Redis服务启动完成"
}

# 构建并启动应用
start_application() {
    print_message "构建并启动Redis Web GUI应用..."
    
    # 构建Docker镜像
    docker build -t redis-web-gui .
    
    # 停止并删除旧容器
    docker stop redis-web-gui 2>/dev/null || true
    docker rm redis-web-gui 2>/dev/null || true
    
    # 启动新容器
    docker run -d \
        --name redis-web-gui \
        -p 8080:8080 \
        --network redis-web-gui_redis-network \
        redis-web-gui
    
    print_message "应用启动完成"
}

# 显示服务状态
show_status() {
    print_message "服务状态:"
    echo ""
    
    # Redis服务状态
    if docker ps | grep -q redis-server; then
        echo -e "${GREEN}✓ Redis服务运行中${NC}"
    else
        echo -e "${RED}✗ Redis服务未运行${NC}"
    fi
    
    # Web应用状态
    if docker ps | grep -q redis-web-gui; then
        echo -e "${GREEN}✓ Web应用运行中${NC}"
    else
        echo -e "${RED}✗ Web应用未运行${NC}"
    fi
    
    # Redis Commander状态
    if docker ps | grep -q redis-commander; then
        echo -e "${GREEN}✓ Redis Commander运行中${NC}"
    else
        echo -e "${YELLOW}○ Redis Commander未启动${NC}"
    fi
    
    # Redis Insight状态
    if docker ps | grep -q redis-insight; then
        echo -e "${GREEN}✓ Redis Insight运行中${NC}"
    else
        echo -e "${YELLOW}○ Redis Insight未启动${NC}"
    fi
    
    echo ""
    print_message "访问地址:"
    echo "  - Redis Web GUI: http://localhost:8080/redis-gui"
    echo "  - H2控制台: http://localhost:8080/redis-gui/h2-console"
    echo "  - Redis Commander: http://localhost:8081"
    echo "  - Redis Insight: http://localhost:8001"
}

# 停止所有服务
stop_all() {
    print_message "停止所有服务..."
    docker compose down
    docker stop redis-web-gui 2>/dev/null || true
    docker rm redis-web-gui 2>/dev/null || true
    print_message "所有服务已停止"
}

# 查看日志
show_logs() {
    local service=${1:-redis-web-gui}
    print_message "查看 $service 日志..."
    docker logs -f $service
}

# 重启服务
restart_services() {
    print_message "重启所有服务..."
    stop_all
    sleep 2
    start_redis
    start_application
    print_message "服务重启完成"
}

# 清理数据
clean_data() {
    print_warning "此操作将删除所有数据，确定继续吗？(y/N)"
    read -r response
    if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
        print_message "清理所有数据..."
        docker compose down -v
        docker volume rm redis-web-gui_redis_data 2>/dev/null || true
        docker volume rm redis-web-gui_redisinsight_data 2>/dev/null || true
        print_message "数据清理完成"
    else
        print_message "操作已取消"
    fi
}

# 显示帮助信息
show_help() {
    echo "用法: $0 [命令]"
    echo ""
    echo "命令:"
    echo "  start        启动Redis服务和Web应用"
    echo "  start-all    启动所有服务（包括管理工具）"
    echo "  stop         停止所有服务"
    echo "  restart      重启所有服务"
    echo "  status       查看服务状态"
    echo "  logs [服务]   查看服务日志（默认redis-web-gui）"
    echo "  clean        清理所有数据"
    echo "  help         显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 start        # 启动基础服务"
    echo "  $0 start-all    # 启动所有服务"
    echo "  $0 status       # 查看状态"
    echo "  $0 logs         # 查看应用日志"
}

# 主函数
main() {
    print_header
    
    # 检查Docker环境
    if ! check_docker; then
        print_error "Docker环境检查失败，请先安装Docker"
        exit 1
    fi
    
    # 解析命令
    case "${1:-help}" in
        start)
            start_redis
            start_application
            show_status
            ;;
        start-all)
            ./start-redis.sh start-all
            start_application
            show_status
            ;;
        stop)
            stop_all
            ;;
        restart)
            restart_services
            show_status
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs $2
            ;;
        clean)
            clean_data
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            print_error "未知命令: $1"
            show_help
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@" 