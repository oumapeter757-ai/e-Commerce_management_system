package com.peterscode.ecommerce_management_system.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis configuration for caching and rate-limiting.
 * Uses Jackson 3.x JsonMapper with polymorphic type info
 * so deserialization returns the correct Java type.
 */
@Configuration
@EnableCaching
public class RedisConfig {


    private JsonMapper redisJsonMapper() {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();

        return JsonMapper.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();
    }

    /**
     * Custom RedisSerializer using Jackson 3.x JsonMapper.
     * Serializes to JSON with embedded type info; deserializes back to the correct Java class.
     */
    private RedisSerializer<Object> jsonRedisSerializer() {
        JsonMapper mapper = redisJsonMapper();

        return new RedisSerializer<>() {
            @Override
            public byte[] serialize(Object value) throws SerializationException {
                if (value == null) return new byte[0];
                try {
                    return mapper.writeValueAsBytes(value);
                } catch (Exception e) {
                    throw new SerializationException("Error serializing object to Redis JSON", e);
                }
            }

            @Override
            public Object deserialize(byte[] bytes) throws SerializationException {
                if (bytes == null || bytes.length == 0) return null;
                try {
                    return mapper.readValue(bytes, Object.class);
                } catch (Exception e) {
                    throw new SerializationException("Error deserializing Redis JSON to object", e);
                }
            }
        };
    }

    /**
     * RedisTemplate for direct Redis operations (rate limiting, IP blacklists, etc.)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        RedisSerializer<Object> jsonSerializer = jsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * RedisCacheManager with per-cache TTL configuration.
     *
     * Cache TTLs based on data volatility:
     *   products    → 30 min
     *   categories  → 2 hours
     *   users       → 15 min
     *   mpesaToken  → 50 min
     *   default     → 10 min
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisSerializer<Object> jsonSerializer = jsonRedisSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .prefixCacheNameWith("ecommerce:");

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        cacheConfigs.put("products", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put("categories", defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigs.put("users", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put("mpesaToken", defaultConfig.entryTtl(Duration.ofMinutes(50)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }
}