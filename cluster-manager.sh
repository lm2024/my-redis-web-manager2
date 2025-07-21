#!/bin/bash

# Redis集群管理脚本
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
    echo -e "${BLUE}  Redis集群管理脚本${NC}"
    echo -e "${BLUE}================================${NC}"
}

# 检查Docker是否安装
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker 未安装，请先安装 Docker"
        return 1
    fi
    
    if ! docker compose version &> /dev/null; then
        print_error "Docker Compose 不可用，请检查Docker安装"
        return 1
    fi
    
    return 0
}

# 启动集群
start_cluster() {
    print_message "启动Redis集群..."
    docker compose --profile cluster up -d
    
    print_message "等待集群节点启动..."
    sleep 10
    
    print_message "初始化集群..."
    docker compose --profile cluster up redis-cluster-init
    
    print_message "Redis集群启动完成！"
}

# 停止集群
stop_cluster() {
    print_message "停止Redis集群..."
    docker compose --profile cluster down
    print_message "Redis集群已停止"
}

# 重启集群
restart_cluster() {
    print_message "重启Redis集群..."
    stop_cluster
    sleep 5
    start_cluster
}

# 查看集群状态
show_cluster_status() {
    print_message "Redis集群状态:"
    echo ""
    
    # 检查集群节点
    local nodes=("redis-cluster-1" "redis-cluster-2" "redis-cluster-3" "redis-cluster-4" "redis-cluster-5" "redis-cluster-6")
    local ports=(17000 17001 17002 17003 17004 17005)
    
    for i in "${!nodes[@]}"; do
        local node=${nodes[$i]}
        local port=${ports[$i]}
        
        if docker ps | grep -q "$node"; then
            echo -e "${GREEN}✓ $node (端口: $port): 运行中${NC}"
            
            # 检查节点是否响应
            if docker compose --profile cluster exec "$node" redis-cli -p "$port" ping &>/dev/null; then
                echo -e "${GREEN}  - 节点响应: 正常${NC}"
            else
                echo -e "${RED}  - 节点响应: 异常${NC}"
            fi
        else
            echo -e "${RED}✗ $node (端口: $port): 未运行${NC}"
        fi
    done
    
    echo ""
    
    # 显示集群信息
    if docker ps | grep -q redis-cluster-1; then
        print_message "集群详细信息:"
        echo ""
        
        # 集群信息
        print_message "集群信息:"
        docker compose --profile cluster exec redis-cluster-1 redis-cli -p 17000 cluster info
        
        echo ""
        
        # 节点信息
        print_message "节点信息:"
        docker compose --profile cluster exec redis-cluster-1 redis-cli -p 17000 cluster nodes
        
        echo ""
        
        # 槽位分配
        print_message "槽位分配:"
        docker compose --profile cluster exec redis-cluster-1 redis-cli -p 17000 cluster slots
    fi
}

# 查看集群日志
show_cluster_logs() {
    local node=${1:-redis-cluster-1}
    print_message "查看集群节点 $node 日志..."
    docker compose --profile cluster logs -f "$node"
}

# 进入集群节点
enter_cluster_node() {
    local node=${1:-redis-cluster-1}
    local port=${2:-17000}
    print_message "进入集群节点 $node (端口: $port)..."
    docker compose --profile cluster exec "$node" redis-cli -p "$port"
}

# 测试集群连接
test_cluster() {
    print_message "测试集群连接..."
    
    if ! docker ps | grep -q redis-cluster-1; then
        print_error "集群未运行，请先启动集群"
        return 1
    fi
    
    # 测试基本连接
    print_message "测试基本连接..."
    docker compose --profile cluster exec redis-cluster-1 redis-cli -p 17000 ping
    
    # 测试集群模式
    print_message "测试集群模式..."
    docker compose --profile cluster exec redis-cluster-1 redis-cli -p 17000 cluster info
    
    # 测试数据写入
    print_message "测试数据写入..."
    docker compose --profile cluster exec redis-cluster-1 redis-cli -p 17000 set test_key "Hello Cluster!"
    
    # 测试数据读取
    print_message "测试数据读取..."
    docker compose --profile cluster exec redis-cluster-1 redis-cli -p 17000 get test_key
    
    # 测试数据删除
    print_message "测试数据删除..."
    docker compose --profile cluster exec redis-cluster-1 redis-cli -p 17000 del test_key
    
    print_message "集群测试完成！"
}

# 集群性能测试
benchmark_cluster() {
    print_message "开始集群性能测试..."
    
    if ! docker ps | grep -q redis-cluster-1; then
        print_error "集群未运行，请先启动集群"
        return 1
    fi
    
    # 使用redis-benchmark进行性能测试
    print_message "运行性能测试 (10秒)..."
    docker compose --profile cluster exec redis-cluster-1 redis-benchmark -h redis-cluster-1 -p 17000 -t set,get -d 100 -n 10000 -c 50 --cluster
}

# 备份集群数据
backup_cluster() {
    local backup_dir="./backups/cluster"
    local timestamp=$(date +"%Y%m%d_%H%M%S")
    
    print_message "备份集群数据..."
    
    # 创建备份目录
    mkdir -p "$backup_dir"
    
    # 备份每个节点
    local nodes=("redis-cluster-1" "redis-cluster-2" "redis-cluster-3" "redis-cluster-4" "redis-cluster-5" "redis-cluster-6")
    local ports=(17000 17001 17002 17003 17004 17005)
    
    for i in "${!nodes[@]}"; do
        local node=${nodes[$i]}
        local port=${ports[$i]}
        
        if docker ps | grep -q "$node"; then
            print_message "备份节点 $node..."
            
            # 执行BGSAVE
            docker compose --profile cluster exec "$node" redis-cli -p "$port" BGSAVE
            sleep 2
            
            # 复制备份文件
            local backup_file="$backup_dir/${node}_backup_${timestamp}.rdb"
            docker compose --profile cluster cp "$node:/data/dump.rdb" "$backup_file"
            
            print_message "节点 $node 备份完成: $backup_file"
        else
            print_warning "节点 $node 未运行，跳过备份"
        fi
    done
    
    print_message "集群数据备份完成！"
}

# 清理集群数据
clean_cluster() {
    print_warning "此操作将删除所有集群数据，确定继续吗？(y/N)"
    read -r response
    if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
        print_message "清理集群数据..."
        docker compose --profile cluster down -v
        docker volume rm redis-web-gui_redis_cluster_1 2>/dev/null || true
        docker volume rm redis-web-gui_redis_cluster_2 2>/dev/null || true
        docker volume rm redis-web-gui_redis_cluster_3 2>/dev/null || true
        docker volume rm redis-web-gui_redis_cluster_4 2>/dev/null || true
        docker volume rm redis-web-gui_redis_cluster_5 2>/dev/null || true
        docker volume rm redis-web-gui_redis_cluster_6 2>/dev/null || true
        print_message "集群数据清理完成"
    else
        print_message "操作已取消"
    fi
}

# 显示帮助信息
show_help() {
    echo "用法: $0 [命令] [参数]"
    echo ""
    echo "命令:"
    echo "  start        启动Redis集群"
    echo "  stop         停止Redis集群"
    echo "  restart      重启Redis集群"
    echo "  status       查看集群状态"
    echo "  logs [节点]   查看集群节点日志"
    echo "  shell [节点] [端口] 进入集群节点命令行"
    echo "  test         测试集群连接"
    echo "  benchmark    集群性能测试"
    echo "  backup       备份集群数据"
    echo "  clean        清理集群数据"
    echo "  help         显示此帮助信息"
    echo ""
    echo "节点名称:"
    echo "  redis-cluster-1 到 redis-cluster-6"
    echo ""
    echo "端口:"
    echo "  17000, 17001, 17002, 17003, 17004, 17005"
    echo ""
    echo "示例:"
    echo "  $0 start        # 启动集群"
    echo "  $0 status       # 查看集群状态"
    echo "  $0 test         # 测试集群"
    echo "  $0 logs redis-cluster-1  # 查看节点日志"
    echo "  $0 shell redis-cluster-1 17000  # 进入节点命令行"
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
            start_cluster
            ;;
        stop)
            stop_cluster
            ;;
        restart)
            restart_cluster
            ;;
        status)
            show_cluster_status
            ;;
        logs)
            show_cluster_logs $2
            ;;
        shell)
            enter_cluster_node $2 $3
            ;;
        test)
            test_cluster
            ;;
        benchmark)
            benchmark_cluster
            ;;
        backup)
            backup_cluster
            ;;
        clean)
            clean_cluster
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