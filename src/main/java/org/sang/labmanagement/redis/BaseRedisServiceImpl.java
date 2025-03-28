package org.sang.labmanagement.redis;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BaseRedisServiceImpl<T> implements BaseRedisService<T> {


	private final RedisTemplate<String, T> redisTemplate;

	@Override
	public void set(String key, T value) {
		redisTemplate.opsForValue().set(key, value);
	}

	@Override
	public void setWithExpiration(String key, T value, long timeoutInSeconds) {
		redisTemplate.opsForValue().set(key, value, timeoutInSeconds, TimeUnit.MILLISECONDS);
	}

	@Override
	public T get(String key) {
		return redisTemplate.opsForValue().get(key);
	}

	@Override
	public boolean exists(String key) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(key));
	}

	@Override
	public void delete(String key) {
		redisTemplate.delete(key);
	}

	@Override
	public void hashSet(String key, String field, T value) {
		redisTemplate.opsForHash().put(key, field, value);
	}

	@Override
	public boolean hashExists(String key, String field) {
		return redisTemplate.opsForHash().hasKey(key, field);
	}

	@Override
	public T hashGet(String key, String field) {
		return (T) redisTemplate.opsForHash().get(key, field);
	}

	@Override
	public Map<String, T> getAllHashFields(String key) {
		Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

		return entries.entrySet().stream()
				.collect(Collectors.toMap(
						e -> String.valueOf(e.getKey()), // Chuyển key về String
						e -> (T) e.getValue() // Ép kiểu value về T
				));
	}

	@Override
	public List<T> hashGetByFieldPrefix(String key, String fieldPrefix) {
		return List.of();
	}

	@Override
	public void deleteHashField(String key, String field) {
		redisTemplate.opsForHash().delete(key, field);
	}

	@Override
	public void deleteHashFields(String key, List<String> fields) {
		redisTemplate.opsForHash().delete(key, fields.toArray());
	}

	@Override
	public Set<String> scanKeys(String pattern) {
		return redisTemplate.keys(pattern);
	}

	@Override
	public Long getTTL(String key) {
		return redisTemplate.getExpire(key, TimeUnit.SECONDS);
	}

}
