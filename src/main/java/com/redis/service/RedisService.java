package com.redis.service;

import com.redis.entity.RedisConnection;
import com.redis.entity.RedisMonitorRecord;
import com.redis.repository.RedisMonitorRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Redis服务类
 */
@Slf4j
@Service
public class RedisService {

    @Autowired
    private RedisMonitorRecordRepository monitorRecordRepository;

    // 缓存RedisTemplate实例
    private final Map<Long, RedisTemplate<String, Object>> redisTemplateCache = new ConcurrentHashMap<>();

    /**
     * 获取RedisTemplate实例
     */
    public RedisTemplate<String, Object> getRedisTemplate(RedisConnection connection) {
        return redisTemplateCache.computeIfAbsent(connection.getId(), id -> createRedisTemplate(connection));
    }

    /**
     * 创建RedisTemplate实例
     */
    private RedisTemplate<String, Object> createRedisTemplate(RedisConnection connection) {
        if (connection == null) {
            throw new RuntimeException("Redis连接不能为空");
        }

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(connection.getHost());
        config.setPort(connection.getPort());
        config.setDatabase(connection.getDatabase());
        if (connection.getPassword() != null && !connection.getPassword().isEmpty()) {
            config.setPassword(connection.getPassword());
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();

        return template;
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
     * 获取Redis信息
     */
    public Map<String, Object> getRedisInfo(RedisConnection connection) {
        try {
            RedisTemplate<String, Object> template = getRedisTemplate(connection);
            Properties info = template.getConnectionFactory().getConnection().info();
            
            Map<String, Object> result = new HashMap<>();
            for (String key : info.stringPropertyNames()) {
                result.put(key, info.getProperty(key));
            }
            
            return result;
        } catch (Exception e) {
            log.error("获取Redis信息失败: {}", e.getMessage());
            throw new RuntimeException("获取Redis信息失败", e);
        }
    }

    /**
     * 获取键列表
     */
    public List<String> getKeys(RedisConnection connection, String pattern, int limit) {
        try {
            RedisTemplate<String, Object> template = getRedisTemplate(connection);
            Set<String> keys = template.keys(pattern != null ? pattern : "*");
            
            if (keys == null) {
                return new ArrayList<>();
            }
            
            return keys.stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取键列表失败: {}", e.getMessage());
            throw new RuntimeException("获取键列表失败", e);
        }
    }

    /**
     * 获取键值
     */
    public Object getValue(RedisConnection connection, String key) {
        try {
            RedisTemplate<String, Object> template = getRedisTemplate(connection);
            return template.opsForValue().get(key);
        } catch (Exception e) {
            log.error("获取键值失败: {}", e.getMessage());
            throw new RuntimeException("获取键值失败", e);
        }
    }

    /**
     * 设置键值
     */
    public void setValue(RedisConnection connection, String key, String value, Long ttl) {
        log.info("=== RedisService.setValue开始 ===");
        log.info("连接: {}", connection.getName());
        log.info("键: {}", key);
        log.info("值: {}", value);
        log.info("TTL: {}", ttl);
        
        try {
            RedisTemplate<String, Object> template = getRedisTemplate(connection);
            log.info("获取到RedisTemplate");
            
            if (ttl != null && ttl > 0) {
                log.info("设置键值带TTL: {}秒", ttl);
                template.opsForValue().set(key, value, Duration.ofSeconds(ttl));
            } else {
                log.info("设置键值无TTL");
                template.opsForValue().set(key, value);
            }
            
            log.info("键值设置成功");
            log.info("=== RedisService.setValue完成 ===");
            
        } catch (Exception e) {
            log.error("RedisService.setValue失败", e);
            throw e;
        }
    }

    /**
     * 删除键
     */
    public boolean deleteKey(RedisConnection connection, String key) {
        log.info("=== RedisService.deleteKey开始 ===");
        log.info("连接: {}", connection.getName());
        log.info("键: {}", key);
        
        try {
            RedisTemplate<String, Object> template = getRedisTemplate(connection);
            log.info("获取到RedisTemplate");
            
            Boolean result = template.delete(key);
            log.info("删除结果: {}", result);
            
            log.info("=== RedisService.deleteKey完成 ===");
            return result != null && result;
            
        } catch (Exception e) {
            log.error("RedisService.deleteKey失败", e);
            throw e;
        }
    }

    /**
     * 批量删除键
     */
    public int batchDeleteKeys(RedisConnection connection, List<String> keys) {
        log.info("=== RedisService.batchDeleteKeys开始 ===");
        log.info("连接: {}", connection.getName());
        log.info("键数量: {}", keys.size());
        
        try {
            RedisTemplate<String, Object> template = getRedisTemplate(connection);
            log.info("获取到RedisTemplate");
            
            Long deletedCount = template.delete(keys);
            log.info("批量删除结果: {}", deletedCount);
            
            log.info("=== RedisService.batchDeleteKeys完成 ===");
            return deletedCount != null ? deletedCount.intValue() : 0;
            
        } catch (Exception e) {
            log.error("RedisService.batchDeleteKeys失败", e);
            throw e;
        }
    }

    /**
     * 获取键类型
     */
    public String getKeyType(RedisConnection connection, String key) {
        try {
            RedisTemplate<String, Object> template = getRedisTemplate(connection);
            return template.type(key).name();
        } catch (Exception e) {
            log.error("获取键类型失败: {}", e.getMessage());
            throw new RuntimeException("获取键类型失败", e);
        }
    }

    /**
     * 获取键TTL
     */
    public Long getKeyTtl(RedisConnection connection, String key) {
        try {
            RedisTemplate<String, Object> template = getRedisTemplate(connection);
            return template.getExpire(key);
        } catch (Exception e) {
            log.error("获取键TTL失败: {}", e.getMessage());
            throw new RuntimeException("获取键TTL失败", e);
        }
    }

    /**
     * 获取键大小
     */
    public Long getKeySize(RedisConnection connection, String key) {
        try {
            RedisTemplate<String, Object> template = getRedisTemplate(connection);
            String keyType = template.type(key).name();
            
            switch (keyType) {
                case "STRING":
                    Object value = template.opsForValue().get(key);
                    return value != null ? (long) value.toString().length() : 0L;
                case "HASH":
                    return template.opsForHash().size(key);
                case "LIST":
                    return template.opsForList().size(key);
                case "SET":
                    return template.opsForSet().size(key);
                case "ZSET":
                    return template.opsForZSet().size(key);
                default:
                    return 0L;
            }
        } catch (Exception e) {
            log.error("获取键大小失败: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 设置键TTL
     */
    public boolean setKeyTtl(RedisConnection connection, String key, Long ttl) {
        try {
            RedisTemplate<String, Object> template = getRedisTemplate(connection);
            if (ttl == null || ttl <= 0) {
                // 清除TTL，设置为永不过期
                return Boolean.TRUE.equals(template.persist(key));
            } else {
                // 设置TTL
                return Boolean.TRUE.equals(template.expire(key, Duration.ofSeconds(ttl)));
            }
        } catch (Exception e) {
            log.error("设置键TTL失败: {}", e.getMessage());
            throw new RuntimeException("设置键TTL失败", e);
        }
    }

    /**
     * 获取Hash字段
     */
    public Map<Object, Object> getHashFields(RedisConnection connection, String key) {
        try {
            RedisTemplate<String, Object> template = getRedisTemplate(connection);
            return template.opsForHash().entries(key);
        } catch (Exception e) {
            log.error("获取Hash字段失败: {}", e.getMessage());
            throw new RuntimeException("获取Hash字段失败", e);
        }
    }

    /**
     * 获取List元素
     */
    public List<Object> getListElements(RedisConnection connection, String key, long start, long end) {
        try {
            RedisTemplate<String, Object> template = getRedisTemplate(connection);
            return template.opsForList().range(key, start, end);
        } catch (Exception e) {
            log.error("获取List元素失败: {}", e.getMessage());
            throw new RuntimeException("获取List元素失败", e);
        }
    }

    /**
     * 获取Set元素
     */
    public Set<Object> getSetElements(RedisConnection connection, String key) {
        try {
            RedisTemplate<String, Object> template = getRedisTemplate(connection);
            return template.opsForSet().members(key);
        } catch (Exception e) {
            log.error("获取Set元素失败: {}", e.getMessage());
            throw new RuntimeException("获取Set元素失败", e);
        }
    }

    /**
     * 获取ZSet元素
     */
    public Set<Object> getZSetElements(RedisConnection connection, String key, long start, long end) {
        try {
            RedisTemplate<String, Object> template = getRedisTemplate(connection);
            return template.opsForZSet().range(key, start, end);
        } catch (Exception e) {
            log.error("获取ZSet元素失败: {}", e.getMessage());
            throw new RuntimeException("获取ZSet元素失败", e);
        }
    }

    /**
     * 清除连接缓存
     */
    public void clearConnectionCache(Long connectionId) {
        redisTemplateCache.remove(connectionId);
    }

    /**
     * 清除所有连接缓存
     */
    public void clearAllConnectionCache() {
        redisTemplateCache.clear();
    }
} 