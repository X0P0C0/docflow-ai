package com.docflow.ai.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    @Bean
    public RedisCacheConfiguration defaultCacheConfiguration(ObjectMapper objectMapper) {
        ObjectMapper cacheMapper = objectMapper.copy()
                .registerModule(new JavaTimeModule())
                .activateDefaultTyping(
                        LaissezFaireSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(cacheMapper)))
                .disableCachingNullValues();
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer cacheTtlCustomizer() {
        return builder -> builder
                .withCacheConfiguration("ticket-detail",
                        defaultCacheConfiguration(null).entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("ticket-list",
                        defaultCacheConfiguration(null).entryTtl(Duration.ofMinutes(2)))
                .withCacheConfiguration("knowledge-detail",
                        defaultCacheConfiguration(null).entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("knowledge-list",
                        defaultCacheConfiguration(null).entryTtl(Duration.ofMinutes(5)));
    }
}
