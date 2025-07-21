package com.redis.service;

import com.redis.entity.RedisConnection;
import com.redis.entity.RedisMonitorRecord;
import com.redis.repository.RedisMonitorRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Redis监控服务
 */
@Slf4j
@Service
public class RedisMonitorService {

    @Autowired
    private RedisConnectionService redisConnectionService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private RedisMonitorRecordRepository monitorRecordRepository;

    /**
     * 定时收集监控数据 (每5秒)
     */
    @Scheduled(fixedRate = 5000)
    public void collectMonitorData() {
        try {
            List<RedisConnection> connections = redisConnectionService.getAllConnections();
            for (RedisConnection connection : connections) {
                collectConnectionMonitorData(connection);
            }
        } catch (Exception e) {
            log.error("收集监控数据失败: {}", e.getMessage());
        }
    }

    /**
     * 收集单个连接的监控数据
     */
    public void collectConnectionMonitorData(RedisConnection connection) {
        try {
            Map<String, Object> info = redisService.getRedisInfo(connection);
            
            RedisMonitorRecord record = RedisMonitorRecord.builder()
                    .connectionId(connection.getId())
                    .connectionName(connection.getName())
                    .totalConnectionsReceived(getLongValue(info, "total_connections_received"))
                    .totalCommandsProcessed(getLongValue(info, "total_commands_processed"))
                    .instantaneousOpsPerSec(getLongValue(info, "instantaneous_ops_per_sec"))
                    .totalNetInputBytes(getLongValue(info, "total_net_input_bytes"))
                    .totalNetOutputBytes(getLongValue(info, "total_net_output_bytes"))
                    .instantaneousInputKbps(getDoubleValue(info, "instantaneous_input_kbps"))
                    .instantaneousOutputKbps(getDoubleValue(info, "instantaneous_output_kbps"))
                    .rejectedConnections(getLongValue(info, "rejected_connections"))
                    .syncFull(getLongValue(info, "sync_full"))
                    .syncPartialOk(getLongValue(info, "sync_partial_ok"))
                    .syncPartialErr(getLongValue(info, "sync_partial_err"))
                    .expiredKeys(getLongValue(info, "expired_keys"))
                    .evictedKeys(getLongValue(info, "evicted_keys"))
                    .keyspaceHits(getLongValue(info, "keyspace_hits"))
                    .keyspaceMisses(getLongValue(info, "keyspace_misses"))
                    .pubsubChannels(getLongValue(info, "pubsub_channels"))
                    .pubsubPatterns(getLongValue(info, "pubsub_patterns"))
                    .latestForkUsec(getLongValue(info, "latest_fork_usec"))
                    .migrateCachedSockets(getLongValue(info, "migrate_cached_sockets"))
                    .slaveExpiresTrackedKeys(getLongValue(info, "slave_expires_tracked_keys"))
                    .activeDefragHits(getLongValue(info, "active_defrag_hits"))
                    .activeDefragMisses(getLongValue(info, "active_defrag_misses"))
                    .activeDefragKeyHits(getLongValue(info, "active_defrag_key_hits"))
                    .activeDefragKeyMisses(getLongValue(info, "active_defrag_key_misses"))
                    .usedMemory(getLongValue(info, "used_memory"))
                    .usedMemoryHuman(getStringValue(info, "used_memory_human"))
                    .usedMemoryRss(getLongValue(info, "used_memory_rss"))
                    .usedMemoryRssHuman(getStringValue(info, "used_memory_rss_human"))
                    .usedMemoryPeak(getLongValue(info, "used_memory_peak"))
                    .usedMemoryPeakHuman(getStringValue(info, "used_memory_peak_human"))
                    .usedMemoryPeakPerc(getStringValue(info, "used_memory_peak_perc"))
                    .usedMemoryOverhead(getLongValue(info, "used_memory_overhead"))
                    .usedMemoryStartup(getLongValue(info, "used_memory_startup"))
                    .usedMemoryDataset(getLongValue(info, "used_memory_dataset"))
                    .usedMemoryDatasetPerc(getStringValue(info, "used_memory_dataset_perc"))
                    .totalSystemMemory(getLongValue(info, "total_system_memory"))
                    .totalSystemMemoryHuman(getStringValue(info, "total_system_memory_human"))
                    .usedMemoryLua(getLongValue(info, "used_memory_lua"))
                    .usedMemoryLuaHuman(getStringValue(info, "used_memory_lua_human"))
                    .maxmemory(getLongValue(info, "maxmemory"))
                    .maxmemoryHuman(getStringValue(info, "maxmemory_human"))
                    .maxmemoryPolicy(getStringValue(info, "maxmemory_policy"))
                    .memFragmentationRatio(getDoubleValue(info, "mem_fragmentation_ratio"))
                    .memAllocator(getStringValue(info, "mem_allocator"))
                    .activeDefragRunning(getLongValue(info, "active_defrag_running"))
                    .lazyfreePendingObjects(getLongValue(info, "lazyfree_pending_objects"))
                    .lazyfreedObjects(getLongValue(info, "lazyfreed_objects"))
                    .build();

            monitorRecordRepository.save(record);
            
        } catch (Exception e) {
            log.error("收集连接 {} 的监控数据失败: {}", connection.getName(), e.getMessage());
        }
    }

    /**
     * 获取监控记录
     */
    public List<RedisMonitorRecord> getMonitorRecords(Long connectionId, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null) {
            return monitorRecordRepository.findByConnectionIdAndTimeRange(connectionId, startTime, endTime);
        } else {
            return monitorRecordRepository.findByConnectionIdOrderByRecordTimeDesc(connectionId);
        }
    }

    /**
     * 获取最新监控记录
     */
    public RedisMonitorRecord getLatestMonitorRecord(Long connectionId) {
        List<RedisMonitorRecord> records = monitorRecordRepository.findTop1ByConnectionIdOrderByRecordTimeDesc(connectionId);
        return records.isEmpty() ? null : records.get(0);
    }

    /**
     * 获取所有连接的最新监控记录
     */
    public List<RedisMonitorRecord> getAllLatestMonitorRecords() {
        List<RedisConnection> connections = redisConnectionService.getAllConnections();
        return connections.stream()
                .map(connection -> {
                    List<RedisMonitorRecord> records = monitorRecordRepository.findTop1ByConnectionIdOrderByRecordTimeDesc(connection.getId());
                    return records.isEmpty() ? null : records.get(0);
                })
                .filter(record -> record != null)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 清理历史监控数据 (保留7天)
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void cleanHistoricalData() {
        try {
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            monitorRecordRepository.deleteRecordsBefore(sevenDaysAgo);
            log.info("清理7天前的监控数据完成");
        } catch (Exception e) {
            log.error("清理历史监控数据失败: {}", e.getMessage());
        }
    }

    /**
     * 获取监控统计信息
     */
    public Map<String, Object> getMonitorStatistics() {
        Map<String, Object> statistics = new java.util.HashMap<>();
        
        List<RedisConnection> connections = redisConnectionService.getAllConnections();
        statistics.put("totalConnections", connections.size());
        
        long totalRecords = 0;
        for (RedisConnection connection : connections) {
            totalRecords += monitorRecordRepository.countByConnectionId(connection.getId());
        }
        statistics.put("totalRecords", totalRecords);
        
        return statistics;
    }

    /**
     * 获取Long值
     */
    private Long getLongValue(Map<String, Object> info, String key) {
        Object value = info.get(key);
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 获取Double值
     */
    private Double getDoubleValue(Map<String, Object> info, String key) {
        Object value = info.get(key);
        if (value == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * 获取String值
     */
    private String getStringValue(Map<String, Object> info, String key) {
        Object value = info.get(key);
        return value != null ? value.toString() : "";
    }
} 