package com.docflow.ai.common.pattern;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 消息顺序性保证服务
 * <p>
 * 问题：如何保证消息按顺序消费？
 * <pre>
 *   生产者 → [消息1, 消息2, 消息3] → 消费者
 *   如果不保证顺序：可能先处理消息3，再处理消息1
 * </pre>
 * <p>
 * 解决方案：
 * <ul>
 *   <li>方案1：同一业务 ID 的消息发到同一队列（分区）</li>
 *   <li>方案2：使用 Redis List 的 FIFO 特性</li>
 *   <li>方案3：消息中带序号，消费者按序号排序</li>
 * </ul>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>Kafka 分区有序 vs 全局有序</li>
 *   <li>RabbitMQ 的 Queue 有序 vs Exchange 有序</li>
 *   <li>顺序性与性能的权衡</li>
 * </ul>
 */
@Slf4j
@Component
public class OrderedMessageQueue {

    private static final String QUEUE_PREFIX = "ordered:queue:";

    private final StringRedisTemplate redisTemplate;

    public OrderedMessageQueue(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 发送有序消息（同一业务 ID 的消息保证顺序）
     * @param businessId 业务 ID（如工单 ID）
     * @param message 消息内容
     */
    public void send(String businessId, String message) {
        String queueKey = QUEUE_PREFIX + businessId;
        redisTemplate.opsForList().rightPush(queueKey, message);
        log.debug("Sent ordered message to queue: {}", businessId);
    }

    /**
     * 消费有序消息（FIFO 顺序）
     * @param businessId 业务 ID
     * @return 消息内容，null 表示队列为空
     */
    public String receive(String businessId) {
        String queueKey = QUEUE_PREFIX + businessId;
        return redisTemplate.opsForList().leftPop(queueKey);
    }

    /**
     * 批量消费（保证顺序）
     * @param businessId 业务 ID
     * @param count 消费数量
     * @return 消息列表
     */
    public List<String> receiveBatch(String businessId, int count) {
        String queueKey = QUEUE_PREFIX + businessId;
        return redisTemplate.opsForList().leftPop(queueKey, count);
    }

    /**
     * 查看队列长度
     */
    public long getSize(String businessId) {
        String queueKey = QUEUE_PREFIX + businessId;
        Long size = redisTemplate.opsForList().size(queueKey);
        return size != null ? size : 0;
    }

    /**
     * 清空队列
     */
    public void clear(String businessId) {
        redisTemplate.delete(QUEUE_PREFIX + businessId);
    }
}
