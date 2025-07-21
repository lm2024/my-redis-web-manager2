package com.redis.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.entity.RedisConnection;
import com.redis.entity.RedisMonitorRecord;
import com.redis.service.RedisConnectionService;
import com.redis.service.RedisMonitorService;
import com.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Redis管理控制器
 */
@Slf4j
@Controller
@RequestMapping("/redis")
public class RedisController {

    @Autowired
    private RedisConnectionService redisConnectionService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private RedisMonitorService redisMonitorService;
    
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 首页
     */
    @GetMapping({"/", ""})
    public String index(Model model) {
        List<RedisConnection> connections = redisConnectionService.getAllConnections();
        RedisConnection defaultConnection = redisConnectionService.getDefaultConnection();
        
        model.addAttribute("connections", connections);
        model.addAttribute("defaultConnection", defaultConnection);
        model.addAttribute("connectionCount", connections.size());
        
        // 获取监控统计
        Map<String, Object> monitorStats = redisMonitorService.getMonitorStatistics();
        model.addAttribute("monitorStats", monitorStats);
        
        return "index";
    }

    /**
     * 连接管理页面
     */
    @GetMapping("/connections")
    public String connections(Model model) {
        List<RedisConnection> connections = redisConnectionService.getAllConnections();
        model.addAttribute("connections", connections);
        return "connections";
    }

    /**
     * 创建连接页面
     */
    @GetMapping("/connections/new")
    public String newConnection(Model model) {
        model.addAttribute("connection", new RedisConnection());
        return "connection-form";
    }

    /**
     * 编辑连接页面
     */
    @GetMapping("/connections/{id}/edit")
    public String editConnection(@PathVariable Long id, Model model) {
        RedisConnection connection = redisConnectionService.getConnectionById(id);
        if (connection == null) {
            return "redirect:/redis/connections";
        }
        model.addAttribute("connection", connection);
        return "connection-form";
    }

    /**
     * 保存连接
     */
    @PostMapping("/connections")
    public String saveConnection(@ModelAttribute RedisConnection connection, RedirectAttributes redirectAttributes) {
        log.info("=== 保存连接开始 ===");
        log.info("连接信息: {}", connection);
        
        try {
            if (connection.getId() == null) {
                log.info("创建新连接");
                redisConnectionService.createConnection(connection);
                redirectAttributes.addFlashAttribute("message", "连接创建成功");
                log.info("连接创建成功");
            } else {
                log.info("更新连接，ID: {}", connection.getId());
                redisConnectionService.updateConnection(connection.getId(), connection);
                redirectAttributes.addFlashAttribute("message", "连接更新成功");
                log.info("连接更新成功");
            }
        } catch (Exception e) {
            log.error("保存连接失败", e);
            redirectAttributes.addFlashAttribute("error", "操作失败: " + e.getMessage());
        }
        
        log.info("=== 保存连接完成 ===");
        return "redirect:/redis/connections";
    }

    /**
     * 删除连接
     */
    @PostMapping("/connections/{id}/delete")
    public String deleteConnection(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("=== 删除连接开始 ===");
        log.info("连接ID: {}", id);
        
        try {
            redisConnectionService.deleteConnection(id);
            redirectAttributes.addFlashAttribute("message", "连接删除成功");
            log.info("连接删除成功");
        } catch (Exception e) {
            log.error("删除连接失败", e);
            redirectAttributes.addFlashAttribute("error", "删除失败: " + e.getMessage());
        }
        
        log.info("=== 删除连接完成 ===");
        return "redirect:/redis/connections";
    }

    /**
     * 设置默认连接
     */
    @PostMapping("/connections/{id}/default")
    public String setDefaultConnection(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            redisConnectionService.setDefaultConnection(id);
            redirectAttributes.addFlashAttribute("message", "默认连接设置成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "设置失败: " + e.getMessage());
        }
        return "redirect:/redis/connections";
    }

    /**
     * 测试连接
     */
    @PostMapping("/connections/{id}/test")
    @ResponseBody
    public Map<String, Object> testConnection(@PathVariable Long id) {
        log.info("=== 测试连接开始 ===");
        log.info("连接ID: {}", id);
        
        boolean success = redisConnectionService.testConnection(id);
        log.info("测试结果: {}", success);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "连接成功" : "连接失败");
        
        log.info("=== 测试连接完成 ===");
        return result;
    }

    /**
     * 数据浏览页面
     */
    @GetMapping("/browse")
    public String browse(@RequestParam(defaultValue = "1") Long connectionId,
                        @RequestParam(defaultValue = "*") String pattern,
                        @RequestParam(defaultValue = "100") int limit,
                        Model model) {
        try {
            RedisConnection connection = redisConnectionService.getConnectionById(connectionId);
            if (connection == null) {
                connection = redisConnectionService.getDefaultConnection();
            }
            
            List<String> keys = redisService.getKeys(connection, pattern, limit);
            
            // 为每个键获取详细信息
            List<Map<String, Object>> keyDetails = new ArrayList<>();
            for (String key : keys) {
                Map<String, Object> keyInfo = new HashMap<>();
                keyInfo.put("name", key);
                
                try {
                    String keyType = redisService.getKeyType(connection, key);
                    Long ttl = redisService.getKeyTtl(connection, key);
                    Long size = redisService.getKeySize(connection, key);
                    
                    keyInfo.put("type", keyType);
                    keyInfo.put("ttl", ttl);
                    keyInfo.put("size", size);
                } catch (Exception e) {
                    keyInfo.put("type", "未知");
                    keyInfo.put("ttl", -1L);
                    keyInfo.put("size", 0L);
                }
                
                keyDetails.add(keyInfo);
            }
            
            model.addAttribute("connection", connection);
            model.addAttribute("connections", redisConnectionService.getAllConnections());
            model.addAttribute("keys", keyDetails);
            model.addAttribute("pattern", pattern);
            model.addAttribute("limit", limit);
            
        } catch (Exception e) {
            model.addAttribute("error", "获取数据失败: " + e.getMessage());
        }
        
        return "browse";
    }

    /**
     * 查看键值
     */
    @GetMapping("/browse/{connectionId}/key/{key}")
    public String viewKey(@PathVariable Long connectionId, 
                         @PathVariable String key,
                         Model model) {
        try {
            RedisConnection connection = redisConnectionService.getConnectionById(connectionId);
            String keyType = redisService.getKeyType(connection, key);
            Long ttl = redisService.getKeyTtl(connection, key);
            Object value = null;
            
            switch (keyType) {
                case "STRING":
                    value = redisService.getValue(connection, key);
                    break;
                case "HASH":
                    value = redisService.getHashFields(connection, key);
                    break;
                case "LIST":
                    value = redisService.getListElements(connection, key, 0, -1);
                    break;
                case "SET":
                    value = redisService.getSetElements(connection, key);
                    break;
                case "ZSET":
                    value = redisService.getZSetElements(connection, key, 0, -1);
                    break;
            }
            
            model.addAttribute("connection", connection);
            model.addAttribute("key", key);
            model.addAttribute("keyType", keyType);
            model.addAttribute("ttl", ttl);
            model.addAttribute("value", value);
            
        } catch (Exception e) {
            model.addAttribute("error", "获取键值失败: " + e.getMessage());
        }
        
        return "view-key";
    }

    /**
     * 监控页面
     */
    @GetMapping("/monitor")
    public String monitor(@RequestParam(defaultValue = "1") Long connectionId,
                         @RequestParam(required = false) String startTime,
                         @RequestParam(required = false) String endTime,
                         Model model) {
        try {
            RedisConnection connection = redisConnectionService.getConnectionById(connectionId);
            if (connection == null) {
                connection = redisConnectionService.getDefaultConnection();
            }
            
            // 获取Redis信息
            Map<String, Object> redisInfo = redisService.getRedisInfo(connection);
            
            // 转换时间参数
            LocalDateTime start = null;
            LocalDateTime end = null;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            if (startTime != null && !startTime.isEmpty()) {
                start = LocalDateTime.parse(startTime, formatter);
            }
            if (endTime != null && !endTime.isEmpty()) {
                end = LocalDateTime.parse(endTime, formatter);
            }
            
            // 获取监控记录
            List<RedisMonitorRecord> records = redisMonitorService.getMonitorRecords(
                connectionId, start, end);
            
            model.addAttribute("connection", connection);
            model.addAttribute("connections", redisConnectionService.getAllConnections());
            model.addAttribute("redisInfo", redisInfo);
            model.addAttribute("records", records);
            model.addAttribute("startTime", startTime);
            model.addAttribute("endTime", endTime);
            
        } catch (Exception e) {
            model.addAttribute("error", "获取监控数据失败: " + e.getMessage());
        }
        
        return "monitor";
    }

    /**
     * 获取最新监控数据
     */
    @GetMapping("/api/monitor/latest")
    @ResponseBody
    public Map<String, Object> getLatestMonitorData(@RequestParam(defaultValue = "1") Long connectionId) {
        try {
            RedisConnection connection = redisConnectionService.getConnectionById(connectionId);
            if (connection == null) {
                connection = redisConnectionService.getDefaultConnection();
            }
            
            Map<String, Object> redisInfo = redisService.getRedisInfo(connection);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", redisInfo);
            return result;
            
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 设置键值
     */
    @PostMapping("/api/keys")
    @ResponseBody
    public Map<String, Object> setKey(@RequestParam Long connectionId,
                                     @RequestParam String key,
                                     @RequestParam String keyType,
                                     @RequestParam String value,
                                     @RequestParam(required = false) Long ttl) {
        try {
            log.info("=== 设置键值开始 ===");
            log.info("连接ID: {}", connectionId);
            log.info("键名: {}", key);
            log.info("键类型: {}", keyType);
            log.info("键值: {}", value);
            log.info("TTL: {}", ttl);
            
            RedisConnection connection = redisConnectionService.getConnectionById(connectionId);
            log.info("获取到连接: {}", connection != null ? connection.getName() : "null");
            
            if (connection == null) {
                log.error("连接不存在，连接ID: {}", connectionId);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "连接不存在");
                return result;
            }
            
            // 目前只支持String类型，其他类型暂时用String存储
            log.info("开始设置键值到Redis...");
            redisService.setValue(connection, key, value, ttl);
            log.info("键值设置成功");
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "键值设置成功");
            log.info("=== 设置键值完成 ===");
            return result;
            
        } catch (Exception e) {
            log.error("设置键值失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 删除键
     */
    @DeleteMapping("/api/keys")
    @ResponseBody
    public Map<String, Object> deleteKey(@RequestParam Long connectionId,
                                        @RequestParam String key) {
        log.info("=== 删除键开始 ===");
        log.info("连接ID: {}", connectionId);
        log.info("键名: {}", key);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            RedisConnection connection = redisConnectionService.getConnectionById(connectionId);
            if (connection == null) {
                log.error("连接不存在: {}", connectionId);
                result.put("success", false);
                result.put("message", "连接不存在");
                return result;
            }
            
            log.info("获取到连接: {}", connection.getName());
            log.info("开始删除键...");
            
            boolean deleted = redisService.deleteKey(connection, key);
            log.info("删除结果: {}", deleted);
            
            result.put("success", deleted);
            result.put("message", deleted ? "键删除成功" : "键删除失败");
            
            log.info("=== 删除键完成 ===");
            
        } catch (Exception e) {
            log.error("删除键失败", e);
            result.put("success", false);
            result.put("message", "删除失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 批量删除键
     */
    @DeleteMapping("/api/keys/batch")
    @ResponseBody
    public Map<String, Object> batchDeleteKeys(@RequestParam Long connectionId,
                                              @RequestParam String keys) {
        log.info("=== 批量删除键开始 ===");
        log.info("连接ID: {}", connectionId);
        log.info("键列表: {}", keys);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            RedisConnection connection = redisConnectionService.getConnectionById(connectionId);
            if (connection == null) {
                log.error("连接不存在: {}", connectionId);
                result.put("success", false);
                result.put("message", "连接不存在");
                return result;
            }
            
            log.info("获取到连接: {}", connection.getName());
            
            // 解析键列表
            List<String> keyList = new ArrayList<>();
            try {
                keyList = objectMapper.readValue(keys, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.error("解析键列表失败", e);
                result.put("success", false);
                result.put("message", "键列表格式错误");
                return result;
            }
            
            log.info("开始批量删除 {} 个键...", keyList.size());
            
            int deletedCount = redisService.batchDeleteKeys(connection, keyList);
            log.info("批量删除完成，成功删除 {} 个键", deletedCount);
            
            result.put("success", true);
            result.put("message", "批量删除完成");
            result.put("deletedCount", deletedCount);
            result.put("totalCount", keyList.size());
            
            log.info("=== 批量删除键完成 ===");
            
        } catch (Exception e) {
            log.error("批量删除键失败", e);
            result.put("success", false);
            result.put("message", "批量删除失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 测试连接（表单提交）
     */
    @PostMapping("/connections/test")
    @ResponseBody
    public Map<String, Object> testConnectionForm(@RequestParam String name,
                                                 @RequestParam String host,
                                                 @RequestParam Integer port,
                                                 @RequestParam Integer database,
                                                 @RequestParam(required = false) String password,
                                                 @RequestParam(required = false) Integer timeout) {
        try {
            RedisConnection connection = RedisConnection.builder()
                    .name(name)
                    .host(host)
                    .port(port)
                    .database(database)
                    .password(password != null ? password : "")
                    .timeout(timeout != null ? timeout : 5000)
                    .build();
            
            boolean success = redisConnectionService.testConnection(connection);
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", success ? "连接测试成功" : "连接测试失败");
            return result;
            
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 设置键TTL
     */
    @PostMapping("/api/keys/ttl")
    @ResponseBody
    public Map<String, Object> setKeyTtl(@RequestParam Long connectionId,
                                        @RequestParam String key,
                                        @RequestParam(required = false) Long ttl) {
        try {
            log.info("=== 设置键TTL开始 ===");
            log.info("连接ID: {}", connectionId);
            log.info("键名: {}", key);
            log.info("TTL: {}", ttl);
            
            RedisConnection connection = redisConnectionService.getConnectionById(connectionId);
            log.info("获取到连接: {}", connection != null ? connection.getName() : "null");
            
            if (connection == null) {
                log.error("连接不存在，连接ID: {}", connectionId);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "连接不存在");
                return result;
            }
            
            log.info("开始设置键TTL...");
            boolean success = redisService.setKeyTtl(connection, key, ttl);
            log.info("TTL设置结果: {}", success);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", success ? "TTL设置成功" : "TTL设置失败");
            log.info("=== 设置键TTL完成 ===");
            return result;
            
        } catch (Exception e) {
            log.error("设置键TTL失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 获取Redis信息
     */
    @GetMapping("/api/info")
    @ResponseBody
    public Map<String, Object> getRedisInfo(@RequestParam(defaultValue = "1") Long connectionId) {
        try {
            RedisConnection connection = redisConnectionService.getConnectionById(connectionId);
            if (connection == null) {
                connection = redisConnectionService.getDefaultConnection();
            }
            
            Map<String, Object> redisInfo = redisService.getRedisInfo(connection);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", redisInfo);
            return result;
            
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }
} 