#!/bin/bash

# Redis Web GUI 启动脚本
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
    echo -e "${BLUE}  Redis Web GUI 启动脚本${NC}"
    echo -e "${BLUE}================================${NC}"
}

# 检查Java是否安装
check_java() {
    if ! command -v java &> /dev/null; then
        print_error "Java 未安装，请先安装 Java 8 或更高版本"
        exit 1
    fi
    
    # 检查Java版本
    java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_version" -lt "8" ]; then
        print_error "Java版本过低，需要Java 8或更高版本"
        exit 1
    fi
    
    print_message "Java版本检查通过: $(java -version 2>&1 | head -n 1)"
}

# 创建必要的目录
create_directories() {
    mkdir -p data
    mkdir -p logs
    print_message "创建必要目录完成"
}

# 启动应用
start_application() {
    print_message "启动Redis Web GUI应用..."
    
    # 设置Java选项
    JAVA_OPTS="-Xms512m -Xmx1024m -Dfile.encoding=UTF-8"
    
    # 启动应用
    java $JAVA_OPTS -cp "target/classes:target/dependency/*" com.redis.RedisWebGuiApplication
    
    print_message "应用启动完成"
    print_message "访问地址: http://localhost:8080/redis-gui"
    print_message "H2控制台: http://localhost:8080/redis-gui/h2-console"
}

# 编译项目
compile_project() {
    print_message "编译项目..."
    
    # 创建target目录
    mkdir -p target/classes
    mkdir -p target/dependency
    
    # 下载依赖（简化版本，实际项目中应该使用Maven）
    print_warning "注意：这是一个简化版本，需要手动下载依赖"
    print_warning "建议安装Maven或使用IDE来管理依赖"
    
    # 编译Java文件
    if [ -d "src/main/java" ]; then
        find src/main/java -name "*.java" -exec javac -cp "target/dependency/*" -d target/classes {} \;
        print_message "Java文件编译完成"
    else
        print_error "找不到Java源文件"
        exit 1
    fi
}

# 主函数
main() {
    print_header
    
    # 检查Java环境
    check_java
    
    # 创建必要目录
    create_directories
    
    # 编译项目
    compile_project
    
    # 启动应用
    start_application
}

# 执行主函数
main "$@" 