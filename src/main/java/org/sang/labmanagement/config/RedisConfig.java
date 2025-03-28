package org.sang.labmanagement.config;

import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisConfig {

	@Bean
	public RedisSerializer<Object> redisSerializer() {
		return new GenericJackson2JsonRedisSerializer();
	}

	@Bean
	public RedisCacheConfiguration redisCacheConfiguration() {
		return RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofHours(1)) // TTL là 1 hour
				.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
				.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer()));
	}

	@Bean
	public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
		return RedisCacheManager.builder(redisConnectionFactory)
				.cacheDefaults(redisCacheConfiguration())
				.build();
	}
	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(redisConnectionFactory);

		// Sử dụng String cho Key và JSON cho Value
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

		template.setHashKeySerializer(new StringRedisSerializer());
		template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

		template.afterPropertiesSet();
		return template;
	}


}

