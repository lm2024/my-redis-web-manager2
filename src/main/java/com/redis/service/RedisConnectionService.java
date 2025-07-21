package com.redis.service;

import com.redis.entity.RedisConnection;
import com.redis.repository.RedisConnectionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

/**
 * Redis连接管理服务
 */
@Slf4j
@Service
public class RedisConnectionService {

    @Autowired
    private RedisConnectionRepository redisConnectionRepository;

    /**
     * 初始化默认连接
     */
    @PostConstruct
    public void initDefaultConnection() {
        if (redisConnectionRepository.count() == 0) {
            RedisConnection defaultConnection = RedisConnection.builder()
                    .name("本地Redis")
                    .host("localhost")
                    .port(6379)
                    .password("")
                    .database(0)
                    .timeout(5000)
                    .description("默认本地Redis连接")
                    .isDefault(true)
                    .isActive(true)
                    .build();
            
            redisConnectionRepository.save(defaultConnection);
            log.info("创建默认Redis连接: {}", defaultConnection.getName());
        }
    }

    /**
     * 获取所有连接
     */
    public List<RedisConnection> getAllConnections() {
        return redisConnectionRepository.findByIsActiveTrue();
    }

    /**
     * 根据ID获取连接
     */
    public RedisConnection getConnectionById(Long id) {
        Optional<RedisConnection> connection = redisConnectionRepository.findById(id);
        return connection.orElse(null);
    }

    /**
     * 根据名称获取连接
     */
    public RedisConnection getConnectionByName(String name) {
        Optional<RedisConnection> connection = redisConnectionRepository.findByName(name);
        return connection.orElse(null);
    }

    /**
     * 获取默认连接
     */
    public RedisConnection getDefaultConnection() {
        Optional<RedisConnection> connection = redisConnectionRepository.findByIsDefaultTrue();
        return connection.orElse(null);
    }

    /**
     * 创建连接
     */
    public RedisConnection createConnection(RedisConnection connection) {
        // 检查名称是否已存在
        if (redisConnectionRepository.existsByName(connection.getName())) {
            throw new RuntimeException("连接名称已存在: " + connection.getName());
        }

        // 测试连接
        if (!testConnection(connection)) {
            throw new RuntimeException("Redis连接测试失败");
        }

        // 如果设置为默认连接，先取消其他默认连接
        if (Boolean.TRUE.equals(connection.getIsDefault())) {
            clearDefaultConnection();
        }

        RedisConnection savedConnection = redisConnectionRepository.save(connection);
        log.info("创建Redis连接: {}", savedConnection.getName());
        return savedConnection;
    }

    /**
     * 更新连接
     */
    public RedisConnection updateConnection(Long id, RedisConnection connection) {
        RedisConnection existingConnection = getConnectionById(id);
        if (existingConnection == null) {
            throw new RuntimeException("连接不存在: " + id);
        }

        // 检查名称是否已被其他连接使用
        if (!existingConnection.getName().equals(connection.getName()) &&
            redisConnectionRepository.existsByName(connection.getName())) {
            throw new RuntimeException("连接名称已存在: " + connection.getName());
        }

        // 测试连接
        if (!testConnection(connection)) {
            throw new RuntimeException("Redis连接测试失败");
        }

        // 如果设置为默认连接，先取消其他默认连接
        if (Boolean.TRUE.equals(connection.getIsDefault())) {
            clearDefaultConnection();
        }

        connection.setId(id);
        connection.setCreatedTime(existingConnection.getCreatedTime());
        
        RedisConnection updatedConnection = redisConnectionRepository.save(connection);
        
        log.info("更新Redis连接: {}", updatedConnection.getName());
        return updatedConnection;
    }

    /**
     * 删除连接
     */
    public void deleteConnection(Long id) {
        RedisConnection connection = getConnectionById(id);
        if (connection == null) {
            throw new RuntimeException("连接不存在: " + id);
        }

        // 不能删除默认连接
        if (Boolean.TRUE.equals(connection.getIsDefault())) {
            throw new RuntimeException("不能删除默认连接");
        }

        connection.setIsActive(false);
        redisConnectionRepository.save(connection);
        
        log.info("删除Redis连接: {}", connection.getName());
    }

    /**
     * 设置默认连接
     */
    public RedisConnection setDefaultConnection(Long id) {
        RedisConnection connection = getConnectionById(id);
        if (connection == null) {
            throw new RuntimeException("连接不存在: " + id);
        }

        // 取消其他默认连接
        clearDefaultConnection();

        // 设置新的默认连接
        connection.setIsDefault(true);
        RedisConnection updatedConnection = redisConnectionRepository.save(connection);
        
        log.info("设置默认Redis连接: {}", updatedConnection.getName());
        return updatedConnection;
    }

    /**
     * 测试连接
     */
    public boolean testConnection(Long id) {
        RedisConnection connection = getConnectionById(id);
        if (connection == null) {
            return false;
        }
        return testConnection(connection);
    }

    /**
     * 测试连接
     */
    public boolean testConnection(RedisConnection connection) {
        try {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(connection.getHost());
            config.setPort(connection.getPort());
            config.setDatabase(connection.getDatabase());
            if (connection.getPassword() != null && !connection.getPassword().isEmpty()) {
                config.setPassword(connection.getPassword());
            }

            LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
            factory.afterPropertiesSet();
            factory.getConnection().ping();
            return true;
        } catch (Exception e) {
            log.error("Redis连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 清除默认连接标记
     */
    private void clearDefaultConnection() {
        Optional<RedisConnection> defaultConnection = redisConnectionRepository.findByIsDefaultTrue();
        if (defaultConnection.isPresent()) {
            RedisConnection connection = defaultConnection.get();
            connection.setIsDefault(false);
            redisConnectionRepository.save(connection);
        }
    }

    /**
     * 获取连接总数
     */
    public long getConnectionCount() {
        return redisConnectionRepository.count();
    }

    /**
     * 获取活跃连接数
     */
    public long getActiveConnectionCount() {
        return redisConnectionRepository.findByIsActiveTrue().size();
    }
} 