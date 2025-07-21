# Redis Web GUI 快速启动指南

## 🚨 启动问题解决方案

### 问题描述
应用启动时出现以下错误：
```
Failed to convert value of type 'java.lang.String' to required type 'long'; 
nested exception is java.lang.NumberFormatException: For input string: "5000ms"
```

### ✅ 已修复的问题
1. **配置文件类型错误**: 已将 `RedisConfig.java` 中的 `timeout` 字段类型从 `long` 改为 `Duration`
2. **导入错误**: 已修复 `RedisMonitorRecordRepository` 的导入问题
3. **Duration使用**: 已统一使用 `Duration` 类型处理时间配置

### 🛠️ 启动方法

#### 方法一：使用IDE（推荐）

1. **IntelliJ IDEA**
   - 打开项目
   - 等待Maven依赖下载完成
   - 运行 `RedisWebGuiApplication.java`

2. **Eclipse**
   - 导入Maven项目
   - 等待依赖下载
   - 运行 `RedisWebGuiApplication.java`

3. **VS Code**
   - 安装Java扩展
   - 打开项目
   - 使用Spring Boot Dashboard运行

#### 方法二：安装Maven

**macOS (使用Homebrew):**
```bash
brew install maven
```

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install maven
```

**Windows:**
- 下载Maven二进制包
- 配置环境变量

**验证安装:**
```bash
mvn -version
```

#### 方法三：使用Docker（最简单）

1. **启动Redis服务:**
   ```bash
   # Linux/Mac
   ./start-redis.sh start
   
   # Windows
   start-redis.bat start
   ```

2. **构建并运行应用:**
   ```bash
   # 构建Docker镜像
   docker build -t redis-web-gui .
   
   # 运行容器
   docker run -d -p 8080:8080 --name redis-web-gui redis-web-gui
   ```

### 📋 项目依赖

项目需要以下主要依赖：
- Spring Boot 2.7.18
- Spring Data Redis
- H2 Database
- Thymeleaf
- Bootstrap 5
- Chart.js

### 🔧 手动解决依赖问题

如果无法使用Maven，可以手动下载依赖：

1. **创建lib目录:**
   ```bash
   mkdir -p lib
   ```

2. **下载核心依赖:**
   - Spring Boot Starter Web
   - Spring Boot Starter Data Redis
   - H2 Database
   - Thymeleaf
   - Lombok

3. **编译和运行:**
   ```bash
   javac -cp "lib/*" -d target/classes src/main/java/com/redis/*.java
   java -cp "target/classes:lib/*" com.redis.RedisWebGuiApplication
   ```

### 🐳 Docker Compose 完整方案

如果您想快速体验完整功能：

```bash
# 1. 启动所有Redis服务
./start-redis.sh start-all

# 2. 访问各个服务
# Redis Web GUI: http://localhost:8080/redis-gui
# Redis Commander: http://localhost:8081
# Redis Insight: http://localhost:8001
# H2控制台: http://localhost:8080/redis-gui/h2-console
```

### 📞 获取帮助

如果仍然遇到问题：

1. **检查Java版本**: 确保使用Java 8或更高版本
2. **检查端口占用**: 确保8080端口未被占用
3. **查看日志**: 检查启动日志中的详细错误信息
4. **使用IDE**: 推荐使用IntelliJ IDEA或Eclipse

### 🎯 推荐开发环境

- **IDE**: IntelliJ IDEA 或 Eclipse
- **Java**: JDK 8 或更高版本
- **构建工具**: Maven 3.6+
- **容器**: Docker & Docker Compose

---

**注意**: 这个项目需要完整的Maven依赖管理才能正常运行。建议使用IDE或安装Maven来管理依赖。 