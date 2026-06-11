package com.docflow.ai.common.pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("有序消息队列测试")
class OrderedMessageQueueTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ListOperations<String, String> listOps;

    @Test
    @DisplayName("发送消息应使用 rightPush")
    void shouldSendWithRightPush() {
        when(redisTemplate.opsForList()).thenReturn(listOps);

        OrderedMessageQueue queue = new OrderedMessageQueue(redisTemplate);
        queue.send("order-1", "message-1");

        verify(listOps).rightPush("ordered:queue:order-1", "message-1");
    }

    @Test
    @DisplayName("接收消息应使用 leftPop（FIFO）")
    void shouldReceiveWithLeftPop() {
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.leftPop("ordered:queue:order-1")).thenReturn("message-1");

        OrderedMessageQueue queue = new OrderedMessageQueue(redisTemplate);
        String msg = queue.receive("order-1");

        assertThat(msg).isEqualTo("message-1");
    }

    @Test
    @DisplayName("批量消费应返回有序列表")
    void shouldReceiveBatch() {
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.leftPop("ordered:queue:order-1", 3)).thenReturn(List.of("m1", "m2", "m3"));

        OrderedMessageQueue queue = new OrderedMessageQueue(redisTemplate);
        List<String> msgs = queue.receiveBatch("order-1", 3);

        assertThat(msgs).containsExactly("m1", "m2", "m3");
    }

    @Test
    @DisplayName("队列长度")
    void shouldGetSize() {
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.size("ordered:queue:order-1")).thenReturn(5L);

        OrderedMessageQueue queue = new OrderedMessageQueue(redisTemplate);
        long size = queue.getSize("order-1");

        assertThat(size).isEqualTo(5);
    }
}
