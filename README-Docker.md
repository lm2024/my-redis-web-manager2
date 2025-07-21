# Redis Docker Compose 使用说明

本项目提供了完整的Redis Docker Compose配置，包含Redis主服务、Redis Commander和Redis Insight管理工具。

## 快速开始

### 1. 启动基础服务

```bash
# 启动Redis主服务和Redis Commander
docker-compose up -d redis redis-commander

# 或者启动所有服务（包括Redis Insight）
docker-compose up -d
```

### 2. 访问服务

- **Redis服务**: `localhost:6379`
- **Redis Commander**: http://localhost:8081
- **Redis Insight**: http://localhost:8001

### 3. 停止服务

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷
docker-compose down -v
```

## 服务说明

### Redis主服务 (redis)
- **镜像**: redis:7.2-alpine
- **端口**: 6379
- **数据持久化**: 使用Docker卷 `redis_data`
- **配置文件**: `redis.conf`

### Redis Commander (redis-commander)
- **镜像**: rediscommander/redis-commander:latest
- **端口**: 8081
- **功能**: 轻量级Web管理界面
- **特点**: 简单易用，支持多连接管理

### Redis Insight (redisinsight)
- **镜像**: redislabs/redisinsight:latest
- **端口**: 8001
- **功能**: Redis官方管理工具
- **特点**: 功能强大，支持集群管理、性能分析等

## 配置说明

### Redis配置文件 (redis.conf)
主要配置项：
- 内存限制: 256MB
- 持久化: AOF + RDB
- 数据库数量: 16
- 最大连接数: 10000

### 自定义配置
如需修改Redis配置，编辑 `redis.conf` 文件后重启服务：

```bash
docker-compose restart redis
```

## 集群模式

### 启动Redis集群
```bash
# 启动集群模式（需要6个节点）
docker-compose --profile cluster up -d redis-cluster
```

### 初始化集群
```bash
# 进入容器
docker exec -it redis-cluster sh

# 创建集群（3主3从）
redis-cli --cluster create 127.0.0.1:7000 127.0.0.1:7001 127.0.0.1:7002 127.0.0.1:7003 127.0.0.1:7004 127.0.0.1:7005 --cluster-replicas 1
```

## 数据管理

### 备份数据
```bash
# 创建数据备份
docker exec redis-server redis-cli BGSAVE

# 复制备份文件
docker cp redis-server:/data/dump.rdb ./backup/
```

### 恢复数据
```bash
# 停止Redis服务
docker-compose stop redis

# 复制备份文件到容器
docker cp ./backup/dump.rdb redis-server:/data/

# 启动Redis服务
docker-compose start redis
```

## 监控和日志

### 查看日志
```bash
# 查看Redis日志
docker-compose logs redis

# 查看Redis Commander日志
docker-compose logs redis-commander

# 实时查看日志
docker-compose logs -f redis
```

### 监控命令
```bash
# 进入Redis容器
docker exec -it redis-server redis-cli

# 查看Redis信息
INFO

# 查看内存使用
INFO memory

# 查看连接数
INFO clients
```

## 安全配置

### 设置密码
1. 编辑 `redis.conf` 文件
2. 取消注释并设置密码：
   ```
   requirepass your_strong_password
   ```
3. 重启Redis服务：
   ```bash
   docker-compose restart redis
   ```

### 网络安全
- 默认只绑定到本地网络
- 如需外部访问，请配置防火墙规则
- 建议在生产环境中使用密码认证

## 性能优化

### 内存优化
- 根据实际需求调整 `maxmemory` 配置
- 选择合适的 `maxmemory-policy`
- 监控内存使用情况

### 持久化优化
- 根据数据重要性选择RDB或AOF
- 调整 `save` 配置以平衡性能和数据安全
- 监控磁盘I/O性能

## 故障排除

### 常见问题

1. **端口冲突**
   ```bash
   # 检查端口占用
   netstat -tulpn | grep 6379
   
   # 修改docker-compose.yml中的端口映射
   ```

2. **内存不足**
   ```bash
   # 查看容器资源使用
   docker stats redis-server
   
   # 调整maxmemory配置
   ```

3. **连接失败**
   ```bash
   # 检查容器状态
   docker-compose ps
   
   # 查看容器日志
   docker-compose logs redis
   ```

### 健康检查
```bash
# 检查Redis服务状态
docker exec redis-server redis-cli ping

# 检查集群状态
docker exec redis-cluster redis-cli cluster info
```

## 与Spring Boot应用集成

在Spring Boot应用中连接Redis：

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: your_password  # 如果设置了密码
    database: 0
    timeout: 5000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: -1ms
```

## 生产环境建议

1. **安全配置**
   - 设置强密码
   - 限制网络访问
   - 启用SSL/TLS

2. **高可用**
   - 配置Redis Sentinel
   - 使用Redis Cluster
   - 设置主从复制

3. **监控告警**
   - 配置监控工具
   - 设置性能告警
   - 定期备份数据

4. **资源管理**
   - 合理设置内存限制
   - 监控系统资源
   - 优化配置参数 