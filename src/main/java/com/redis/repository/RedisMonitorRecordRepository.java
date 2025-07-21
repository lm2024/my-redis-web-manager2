package com.redis.repository;

import com.redis.entity.RedisMonitorRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Redis监控记录Repository
 */
@Repository
public interface RedisMonitorRecordRepository extends JpaRepository<RedisMonitorRecord, Long> {

    /**
     * 根据连接ID查找监控记录
     */
    List<RedisMonitorRecord> findByConnectionIdOrderByRecordTimeDesc(Long connectionId);

    /**
     * 根据连接ID和时间范围查找监控记录
     */
    @Query("SELECT rmr FROM RedisMonitorRecord rmr WHERE rmr.connectionId = :connectionId " +
           "AND rmr.recordTime BETWEEN :startTime AND :endTime ORDER BY rmr.recordTime DESC")
    List<RedisMonitorRecord> findByConnectionIdAndTimeRange(
            @Param("connectionId") Long connectionId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查找最近的监控记录
     */
    @Query("SELECT rmr FROM RedisMonitorRecord rmr ORDER BY rmr.recordTime DESC")
    List<RedisMonitorRecord> findRecentRecords();

    /**
     * 根据连接ID查找最新的监控记录
     */
    List<RedisMonitorRecord> findTop1ByConnectionIdOrderByRecordTimeDesc(Long connectionId);

    /**
     * 删除指定时间之前的监控记录
     */
    @Query("DELETE FROM RedisMonitorRecord rmr WHERE rmr.recordTime < :beforeTime")
    void deleteRecordsBefore(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 统计指定连接ID的记录数量
     */
    long countByConnectionId(Long connectionId);
} 