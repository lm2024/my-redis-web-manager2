#!/bin/bash

# Redis 服务管理脚本
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
    echo -e "${BLUE}  Redis 服务管理脚本${NC}"
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
    docker compose up -d redis
    print_message "Redis服务启动完成"
}

# 启动所有服务
start_all() {
    print_message "启动所有Redis相关服务..."
    docker compose up -d
    print_message "所有服务启动完成"
}

# 启动Redis集群
start_cluster() {
    print_message "启动Redis集群..."
    docker compose --profile cluster up -d
    print_message "Redis集群启动完成"
    print_message "等待集群初始化..."
    sleep 15
    print_message "集群状态:"
    docker compose --profile cluster logs redis-cluster-init
}

# 停止所有服务
stop_services() {
    print_message "停止所有服务..."
    docker compose down
    docker compose --profile cluster down 2>/dev/null || true
    print_message "所有服务已停止"
}

# 重启服务
restart_services() {
    print_message "重启所有服务..."
    docker compose restart
    print_message "服务重启完成"
}

# 查看服务状态
show_status() {
    print_message "服务状态:"
    echo ""
    
    # Redis服务状态
    if docker ps | grep -q redis-server; then
        echo -e "${GREEN}✓ Redis服务运行中${NC}"
    else
        echo -e "${RED}✗ Redis服务未运行${NC}"
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
    
    # Redis集群状态
    cluster_nodes=$(docker ps | grep redis-cluster | wc -l)
    if [ "$cluster_nodes" -gt 0 ]; then
        echo -e "${GREEN}✓ Redis集群运行中 (${cluster_nodes}个节点)${NC}"
        
        # 检查集群健康状态
        if docker ps | grep -q redis-cluster-1; then
            echo -e "${GREEN}  - 集群节点1 (17000): 运行中${NC}"
        fi
        if docker ps | grep -q redis-cluster-2; then
            echo -e "${GREEN}  - 集群节点2 (17001): 运行中${NC}"
        fi
        if docker ps | grep -q redis-cluster-3; then
            echo -e "${GREEN}  - 集群节点3 (17002): 运行中${NC}"
        fi
        if docker ps | grep -q redis-cluster-4; then
            echo -e "${GREEN}  - 集群节点4 (17003): 运行中${NC}"
        fi
        if docker ps | grep -q redis-cluster-5; then
            echo -e "${GREEN}  - 集群节点5 (17004): 运行中${NC}"
        fi
        if docker ps | grep -q redis-cluster-6; then
            echo -e "${GREEN}  - 集群节点6 (17005): 运行中${NC}"
        fi
    else
        echo -e "${YELLOW}○ Redis集群未启动${NC}"
    fi
    
    echo ""
    print_message "访问地址:"
    echo "  - Redis服务: localhost:6379"
    echo "  - Redis Commander: http://localhost:8081"
    echo "  - Redis Insight: http://localhost:8001"
    echo "  - Redis集群: localhost:7000-7005"
}

# 查看日志
show_logs() {
    local service=${1:-redis}
    print_message "查看 $service 日志..."
    docker compose logs -f $service
}

# 查看集群日志
show_cluster_logs() {
    local node=${1:-redis-cluster-1}
    print_message "查看集群节点 $node 日志..."
    docker compose --profile cluster logs -f $node
}

# 进入Redis命令行
enter_shell() {
    print_message "进入Redis命令行..."
    docker compose exec redis redis-cli
}

# 进入集群节点命令行
enter_cluster_shell() {
    local node=${1:-redis-cluster-1}
    local port=${2:-7000}
    print_message "进入集群节点 $node 命令行..."
    docker compose --profile cluster exec $node redis-cli -p $port
}

# 备份数据
backup_data() {
    local backup_dir="./backups"
    local timestamp=$(date +"%Y%m%d_%H%M%S")
    local backup_file="$backup_dir/redis_backup_$timestamp.rdb"
    
    print_message "备份Redis数据..."
    
    # 创建备份目录
    mkdir -p "$backup_dir"
    
    # 执行备份
    docker compose exec redis redis-cli BGSAVE
    sleep 2
    
    # 复制备份文件
    docker compose cp redis:/data/dump.rdb "$backup_file"
    
    print_message "数据备份完成: $backup_file"
}

# 恢复数据
restore_data() {
    local backup_file=$1
    
    if [ -z "$backup_file" ]; then
        print_error "请指定备份文件路径"
        echo "用法: $0 restore <备份文件路径>"
        exit 1
    fi
    
    if [ ! -f "$backup_file" ]; then
        print_error "备份文件不存在: $backup_file"
        exit 1
    fi
    
    print_warning "此操作将覆盖当前数据，确定继续吗？(y/N)"
    read -r response
    if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
        print_message "恢复Redis数据..."
        
        # 停止Redis服务
        docker compose stop redis
        
        # 复制备份文件
        docker compose cp "$backup_file" redis:/data/dump.rdb
        
        # 启动Redis服务
        docker compose start redis
        
        print_message "数据恢复完成"
    else
        print_message "操作已取消"
    fi
}

# 清理数据
clean_data() {
    print_warning "此操作将删除所有Redis数据，确定继续吗？(y/N)"
    read -r response
    if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
        print_message "清理Redis数据..."
        docker compose down -v
        docker compose --profile cluster down -v 2>/dev/null || true
        docker volume rm redis-web-gui_redis_data 2>/dev/null || true
        docker volume rm redis-web-gui_redisinsight_data 2>/dev/null || true
        docker volume rm redis-web-gui_redis_cluster_1 2>/dev/null || true
        docker volume rm redis-web-gui_redis_cluster_2 2>/dev/null || true
        docker volume rm redis-web-gui_redis_cluster_3 2>/dev/null || true
        docker volume rm redis-web-gui_redis_cluster_4 2>/dev/null || true
        docker volume rm redis-web-gui_redis_cluster_5 2>/dev/null || true
        docker volume rm redis-web-gui_redis_cluster_6 2>/dev/null || true
        print_message "数据清理完成"
    else
        print_message "操作已取消"
    fi
}

# 显示帮助信息
show_help() {
    echo "用法: $0 [命令] [参数]"
    echo ""
    echo "命令:"
    echo "  start        启动Redis服务"
    echo "  start-all    启动所有服务（包括管理工具）"
    echo "  start-cluster 启动Redis集群"
    echo "  stop         停止所有服务"
    echo "  restart      重启所有服务"
    echo "  status       查看服务状态"
    echo "  logs [服务]   查看服务日志（默认redis）"
    echo "  cluster-logs [节点] 查看集群节点日志"
    echo "  shell        进入Redis命令行"
    echo "  cluster-shell [节点] [端口] 进入集群节点命令行"
    echo "  backup       备份Redis数据"
    echo "  restore <文件> 恢复Redis数据"
    echo "  clean        清理所有数据"
    echo "  help         显示此帮助信息"
    echo ""
    echo "服务名称:"
    echo "  redis        主Redis服务"
    echo "  redis-commander  Redis Commander管理界面"
    echo "  redis-insight    Redis Insight管理工具"
    echo "  redis-cluster-1 到 redis-cluster-6  Redis集群节点"
    echo ""
    echo "示例:"
    echo "  $0 start        # 启动Redis服务"
    echo "  $0 start-all    # 启动所有服务"
    echo "  $0 start-cluster # 启动Redis集群"
    echo "  $0 status       # 查看状态"
    echo "  $0 logs redis   # 查看Redis日志"
    echo "  $0 cluster-logs redis-cluster-1  # 查看集群节点日志"
    echo "  $0 shell        # 进入Redis命令行"
    echo "  $0 cluster-shell redis-cluster-1 7000  # 进入集群节点命令行"
    echo "  $0 backup       # 备份数据"
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
            show_status
            ;;
        start-all)
            start_all
            show_status
            ;;
        start-cluster)
            start_cluster
            show_status
            ;;
        stop)
            stop_services
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
        cluster-logs)
            show_cluster_logs $2
            ;;
        shell)
            enter_shell
            ;;
        cluster-shell)
            enter_cluster_shell $2 $3
            ;;
        backup)
            backup_data
            ;;
        restore)
            restore_data $2
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