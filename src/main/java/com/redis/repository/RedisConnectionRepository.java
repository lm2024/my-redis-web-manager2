package com.redis.repository;

import com.redis.entity.RedisConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Redis连接配置Repository
 */
@Repository
public interface RedisConnectionRepository extends JpaRepository<RedisConnection, Long> {

    /**
     * 根据名称查找连接
     */
    Optional<RedisConnection> findByName(String name);

    /**
     * 查找所有活跃的连接
     */
    List<RedisConnection> findByIsActiveTrue();

    /**
     * 查找默认连接
     */
    Optional<RedisConnection> findByIsDefaultTrue();

    /**
     * 检查名称是否存在
     */
    boolean existsByName(String name);

    /**
     * 根据主机和端口查找连接
     */
    List<RedisConnection> findByHostAndPort(String host, Integer port);

    /**
     * 查找最近创建的连接
     */
    @Query("SELECT rc FROM RedisConnection rc ORDER BY rc.createdTime DESC")
    List<RedisConnection> findRecentConnections();
} 