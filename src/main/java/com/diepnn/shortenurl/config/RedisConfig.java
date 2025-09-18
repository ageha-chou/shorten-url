package com.diepnn.shortenurl.config;

import com.diepnn.shortenurl.common.properties.RedisCacheProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableCaching
@RequiredArgsConstructor
public class RedisConfig {
    private final RedisCacheProperties redisCacheProperties;
    private final ObjectMapper objectMapper;

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        return new JedisConnectionFactory();
    }

    @Bean
    public RedisCacheManager redisCacheManager() {
        RedisCacheConfiguration defaultCacheConfig = defaultCacheConfig();

        Map<String, RedisCacheConfiguration> cacheConfigs =
                redisCacheProperties.getCacheTtl()
                                    .entrySet()
                                    .stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey,
                                                              entry -> getRedisCacheConfiguration(entry, defaultCacheConfig)));

        return RedisCacheManager.builder(jedisConnectionFactory())
                                .cacheDefaults(defaultCacheConfig)
                                .withInitialCacheConfigurations(cacheConfigs)
                                .build();
    }

    @Bean
    public RedisSerializer<String> keySerializer() {
        return new StringRedisSerializer();
    }

    @Bean
    public RedisSerializer<Object> valueSerializer() {
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(jedisConnectionFactory());
        template.setKeySerializer(keySerializer());
        template.setValueSerializer(valueSerializer());
        template.setHashKeySerializer(keySerializer());
        template.setHashValueSerializer(valueSerializer());
        return template;
    }

    private RedisCacheConfiguration defaultCacheConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                                      .disableCachingNullValues()
                                      .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer()))
                                      .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer()));
    }

    private RedisCacheConfiguration getRedisCacheConfiguration(Map.Entry<String, Long> entry, RedisCacheConfiguration defaultCacheConfig) {
        Class<?> clazz = redisCacheProperties.getCacheType().get(entry.getKey());
        return defaultCacheConfig.entryTtl(Duration.ofMillis(entry.getValue()))
                                 .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, clazz)));
    }
}
