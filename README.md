# Redis Web GUI

一个基于Spring Boot的Redis管理监控Web工具，提供直观的Web界面来管理Redis数据库。

## 🚨 启动问题解决方案

### 问题描述
如果遇到以下错误：
```
Failed to convert value of type 'java.lang.String' to required type 'long'; 
nested exception is java.lang.NumberFormatException: For input string: "5000ms"
```

### ✅ 已修复
- 修复了配置文件中的类型转换问题
- 统一使用 `Duration` 类型处理时间配置
- 修复了导入错误

### 🛠️ 快速启动

#### 方法一：使用Docker（推荐）
```bash
# Linux/Mac
./start-all.sh start

# Windows
start-all.bat start
```

#### 方法二：使用IDE
1. 使用IntelliJ IDEA或Eclipse打开项目
2. 等待Maven依赖下载完成
3. 运行 `RedisWebGuiApplication.java`

#### 方法三：安装Maven
```bash
# macOS
brew install maven

# Ubuntu/Debian
sudo apt install maven

# 然后运行
mvn spring-boot:run
```

详细说明请查看 [QUICK-START.md](QUICK-START.md)

## ✨ 功能特性

### 🔗 连接管理
- 多Redis连接支持
- 连接测试和状态监控
- 连接配置管理
- 默认连接设置

### 📊 数据浏览
- 键值对查看和编辑
- 支持所有Redis数据类型（String、Hash、List、Set、ZSet）
- 模式匹配搜索
- 批量操作支持

### 📈 监控功能
- 实时性能监控
- 内存使用统计
- 连接数监控
- 命令执行统计
- 历史数据图表

### 🛠️ 管理工具
- 键过期时间管理
- 数据库切换
- 数据导入导出
- 配置管理

## 🏗️ 技术架构

### 后端技术栈
- **Java 8+**
- **Spring Boot 2.7.18** - 主框架
- **Spring Data Redis** - Redis操作
- **H2 Database** - 内嵌数据库
- **Thymeleaf** - 模板引擎
- **Bootstrap 5** - 前端框架
- **Chart.js** - 图表库

### 项目结构
```
src/
├── main/
│   ├── java/com/redis/
│   │   ├── config/          # 配置类
│   │   ├── controller/      # 控制器
│   │   ├── entity/          # 实体类
│   │   ├── repository/      # 数据访问层
│   │   ├── service/         # 业务逻辑层
│   │   └── RedisWebGuiApplication.java
│   └── resources/
│       ├── templates/       # Thymeleaf模板
│       └── application.yml  # 配置文件
```

## 🚀 快速开始

### 环境要求
- Java 8 或更高版本
- Maven 3.6+ 或 IDE
- Docker & Docker Compose（可选）

### 1. 克隆项目
```bash
git clone <repository-url>
cd redisWebGui
```

### 2. 启动Redis服务
```bash
# 使用Docker Compose启动Redis
./start-redis.sh start

# 或手动启动Redis
docker run -d --name redis-server -p 6379:6379 redis:7-alpine
```

### 3. 启动应用
```bash
# 方法一：使用完整启动脚本
./start-all.sh start

# 方法二：使用Maven
mvn spring-boot:run

# 方法三：使用IDE
# 直接运行 RedisWebGuiApplication.java
```

### 4. 访问应用
- **主界面**: http://localhost:8080/redis-gui
- **H2控制台**: http://localhost:8080/redis-gui/h2-console

## 📖 使用说明

### 连接管理
1. 访问应用首页
2. 点击"连接管理"
3. 添加新的Redis连接
4. 测试连接并保存

### 数据浏览
1. 选择要操作的连接
2. 浏览键列表
3. 点击键名查看详情
4. 编辑或删除数据

### 监控功能
1. 在监控页面查看实时数据
2. 查看历史监控记录
3. 分析性能趋势

## 🐳 Docker支持

### 完整Docker环境
项目包含完整的Docker Compose配置：

```bash
# 启动所有服务
./start-redis.sh start-all

# 访问各个服务
# Redis Web GUI: http://localhost:8080/redis-gui
# Redis Commander: http://localhost:8081
# Redis Insight: http://localhost:8001
```

### 服务说明
- **redis-server**: Redis主服务
- **redis-commander**: Redis命令行工具
- **redis-insight**: Redis可视化工具
- **redis-web-gui**: 本项目Web界面

## 🔧 配置说明

### 应用配置 (application.yml)
```yaml
server:
  port: 8080
  servlet:
    context-path: /redis-gui

spring:
  redis:
    host: localhost
    port: 6379
    timeout: 5000ms
  datasource:
    url: jdbc:h2:file:./data/redis_gui_db
    driver-class-name: org.h2.Driver
```

### Redis配置
- 支持多连接配置
- 连接池配置
- 超时设置
- 密码认证

## 📊 监控功能

### 实时监控
- CPU使用率
- 内存使用情况
- 连接数统计
- 命令执行频率

### 历史数据
- 数据持久化存储
- 趋势分析图表
- 性能报告

## 🛠️ 开发指南

### 添加新功能
1. 在 `service` 包中添加业务逻辑
2. 在 `controller` 包中添加API接口
3. 在 `templates` 目录中添加页面模板

### 自定义配置
1. 修改 `application.yml` 配置文件
2. 在 `config` 包中添加配置类
3. 使用 `@Value` 注解注入配置

### 数据库迁移
项目使用H2数据库，数据文件位于 `data/` 目录。

## 🐛 故障排除

### 常见问题

#### 1. 应用启动失败
- 检查Java版本（需要Java 8+）
- 检查端口占用情况
- 查看启动日志

#### 2. Redis连接失败
- 确认Redis服务正在运行
- 检查连接配置（主机、端口、密码）
- 测试网络连通性

#### 3. 页面访问异常
- 检查应用是否正常启动
- 确认访问地址正确
- 查看浏览器控制台错误

### 日志查看
```bash
# 查看应用日志
./start-all.sh logs redis-web-gui

# 查看Redis日志
./start-all.sh logs redis-server
```

## 📝 更新日志

### v1.0.0
- 初始版本发布
- 基础Redis管理功能
- 多连接支持
- 实时监控功能
- Docker支持

## 🤝 贡献指南

1. Fork 项目
2. 创建功能分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 📞 支持

如果您遇到问题或有建议，请：
1. 查看 [QUICK-START.md](QUICK-START.md) 快速启动指南
2. 查看 [README-Docker.md](README-Docker.md) Docker使用说明
3. 提交 Issue 或 Pull Request

---

**Redis Web GUI** - 让Redis管理更简单！ 