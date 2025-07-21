package com.redis.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Redis监控记录实体
 */
@Entity
@Table(name = "redis_monitor_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedisMonitorRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "connection_id")
    private Long connectionId;

    @Column(name = "connection_name")
    private String connectionName;

    @Column(name = "total_connections_received")
    private Long totalConnectionsReceived;

    @Column(name = "total_commands_processed")
    private Long totalCommandsProcessed;

    @Column(name = "instantaneous_ops_per_sec")
    private Long instantaneousOpsPerSec;

    @Column(name = "total_net_input_bytes")
    private Long totalNetInputBytes;

    @Column(name = "total_net_output_bytes")
    private Long totalNetOutputBytes;

    @Column(name = "instantaneous_input_kbps")
    private Double instantaneousInputKbps;

    @Column(name = "instantaneous_output_kbps")
    private Double instantaneousOutputKbps;

    @Column(name = "rejected_connections")
    private Long rejectedConnections;

    @Column(name = "sync_full")
    private Long syncFull;

    @Column(name = "sync_partial_ok")
    private Long syncPartialOk;

    @Column(name = "sync_partial_err")
    private Long syncPartialErr;

    @Column(name = "expired_keys")
    private Long expiredKeys;

    @Column(name = "evicted_keys")
    private Long evictedKeys;

    @Column(name = "keyspace_hits")
    private Long keyspaceHits;

    @Column(name = "keyspace_misses")
    private Long keyspaceMisses;

    @Column(name = "pubsub_channels")
    private Long pubsubChannels;

    @Column(name = "pubsub_patterns")
    private Long pubsubPatterns;

    @Column(name = "latest_fork_usec")
    private Long latestForkUsec;

    @Column(name = "migrate_cached_sockets")
    private Long migrateCachedSockets;

    @Column(name = "slave_expires_tracked_keys")
    private Long slaveExpiresTrackedKeys;

    @Column(name = "active_defrag_hits")
    private Long activeDefragHits;

    @Column(name = "active_defrag_misses")
    private Long activeDefragMisses;

    @Column(name = "active_defrag_key_hits")
    private Long activeDefragKeyHits;

    @Column(name = "active_defrag_key_misses")
    private Long activeDefragKeyMisses;

    @Column(name = "used_memory")
    private Long usedMemory;

    @Column(name = "used_memory_human")
    private String usedMemoryHuman;

    @Column(name = "used_memory_rss")
    private Long usedMemoryRss;

    @Column(name = "used_memory_rss_human")
    private String usedMemoryRssHuman;

    @Column(name = "used_memory_peak")
    private Long usedMemoryPeak;

    @Column(name = "used_memory_peak_human")
    private String usedMemoryPeakHuman;

    @Column(name = "used_memory_peak_perc")
    private String usedMemoryPeakPerc;

    @Column(name = "used_memory_overhead")
    private Long usedMemoryOverhead;

    @Column(name = "used_memory_startup")
    private Long usedMemoryStartup;

    @Column(name = "used_memory_dataset")
    private Long usedMemoryDataset;

    @Column(name = "used_memory_dataset_perc")
    private String usedMemoryDatasetPerc;

    @Column(name = "total_system_memory")
    private Long totalSystemMemory;

    @Column(name = "total_system_memory_human")
    private String totalSystemMemoryHuman;

    @Column(name = "used_memory_lua")
    private Long usedMemoryLua;

    @Column(name = "used_memory_lua_human")
    private String usedMemoryLuaHuman;

    @Column(name = "maxmemory")
    private Long maxmemory;

    @Column(name = "maxmemory_human")
    private String maxmemoryHuman;

    @Column(name = "maxmemory_policy")
    private String maxmemoryPolicy;

    @Column(name = "mem_fragmentation_ratio")
    private Double memFragmentationRatio;

    @Column(name = "mem_allocator")
    private String memAllocator;

    @Column(name = "active_defrag_running")
    private Long activeDefragRunning;

    @Column(name = "lazyfree_pending_objects")
    private Long lazyfreePendingObjects;

    @Column(name = "lazyfreed_objects")
    private Long lazyfreedObjects;

    @Column(name = "record_time")
    private LocalDateTime recordTime;

    @PrePersist
    protected void onCreate() {
        recordTime = LocalDateTime.now();
    }
} 