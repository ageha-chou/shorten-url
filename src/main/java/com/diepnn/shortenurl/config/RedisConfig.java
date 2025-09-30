package com.diepnn.shortenurl.config;

import com.diepnn.shortenurl.common.properties.RedisCacheProperties;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Configuration
@EnableCaching
@RequiredArgsConstructor
@Slf4j
public class RedisConfig {
    private final RedisCacheProperties redisCacheProperties;
    private final ObjectMapper objectMapper;

    private static final Pattern GENERIC_TYPE_PATTERN = Pattern.compile("([^<]+)<(.+)>");

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
        String cacheTypeString = redisCacheProperties.getCacheType().get(entry.getKey());

        RedisSerializer<?> serializer;
        if (cacheTypeString != null && !cacheTypeString.isEmpty()) {
            try {
                JavaType javaType = parseTypeString(cacheTypeString);
                serializer = new Jackson2JsonRedisSerializer<>(objectMapper, javaType);
                log.info("Created type-specific serializer for cache '{}' with type: {}", entry.getKey(), cacheTypeString);
            } catch (Exception e) {
                log.warn("Failed to parse type '{}' for cache '{}', falling back to generic serializer. Error: {}",
                         cacheTypeString, entry.getKey(), e.getMessage());
                serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
            }
        } else {
            serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        }

        return defaultCacheConfig.entryTtl(Duration.ofMillis(entry.getValue()))
                                 .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }

    /**
     * Parse type strings into JavaType objects.
     * Supports various formats:
     * - Simple types: "com.example.UserDTO"
     * - Generic collections: "java.util.ArrayList<com.example.UrlInfoDTO>"
     * - Generic maps: "java.util.HashMap<java.lang.String,com.example.UrlInfoDTO>"
     * - Nested generics: "java.util.List<java.util.Map<java.lang.String,com.example.UrlInfoDTO>>"
     */
    private JavaType parseTypeString(String typeString) {
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        // Remove whitespace
        typeString = typeString.replaceAll("\\s", "");

        return parseTypeRecursive(typeString, typeFactory);
    }

    private JavaType parseTypeRecursive(String typeString, TypeFactory typeFactory) {
        Matcher matcher = GENERIC_TYPE_PATTERN.matcher(typeString);

        if (matcher.matches()) {
            String containerTypeName = matcher.group(1);
            String typeArgumentsString = matcher.group(2);

            try {
                Class<?> containerClass = Class.forName(containerTypeName);
                List<JavaType> typeArguments = parseTypeArguments(typeArgumentsString, typeFactory);

                if (java.util.Map.class.isAssignableFrom(containerClass)) {
                    // Handle Map<K,V>
                    if (typeArguments.size() != 2) {
                        throw new IllegalArgumentException("Map type must have exactly 2 type arguments: " + typeString);
                    }
                    return typeFactory.constructMapType((Class<? extends Map>) containerClass,
                                                        typeArguments.get(0), typeArguments.get(1));
                } else if (java.util.Collection.class.isAssignableFrom(containerClass)) {
                    // Handle Collection<T>
                    if (typeArguments.size() != 1) {
                        throw new IllegalArgumentException("Collection type must have exactly 1 type argument: " + typeString);
                    }
                    return typeFactory.constructCollectionType((Class<? extends java.util.Collection>) containerClass,
                                                               typeArguments.getFirst());
                } else {
                    // Handle other generic types
                    return typeFactory.constructParametricType(containerClass,
                                                               typeArguments.toArray(new JavaType[0]));
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class not found: " + containerTypeName, e);
            }
        } else {
            // Simple type without generics
            try {
                Class<?> clazz = Class.forName(typeString);
                return typeFactory.constructType(clazz);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class not found: " + typeString, e);
            }
        }
    }

    private List<JavaType> parseTypeArguments(String typeArgumentsString, TypeFactory typeFactory) {
        List<JavaType> typeArguments = new ArrayList<>();
        List<String> argumentStrings = splitTypeArguments(typeArgumentsString);

        for (String argString : argumentStrings) {
            typeArguments.add(parseTypeRecursive(argString.trim(), typeFactory));
        }

        return typeArguments;
    }

    /**
     * Split type arguments considering nested generics.
     * Example: "String,List<Integer>" -> ["String", "List<Integer>"]
     * Example: "String,Map<String,Integer>" -> ["String", "Map<String,Integer>"]
     */
    private List<String> splitTypeArguments(String typeArgumentsString) {
        List<String> arguments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;

        for (char c : typeArgumentsString.toCharArray()) {
            if (c == '<') {
                depth++;
                current.append(c);
            } else if (c == '>') {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0) {
                arguments.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) {
            arguments.add(current.toString().trim());
        }

        return arguments;
    }
}
