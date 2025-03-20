package org.sang.labmanagement.redis;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BaseRedisService<T>{
	void set(String key, T value);
	void setWithExpiration(String key, T value, long timeoutInSeconds);
	T get(String key);
	boolean exists(String key);
	void delete(String key);

	Long getTTL(String key);

	// Hash Operations
	void hashSet(String key, String field, T value);
	boolean hashExists(String key, String field);
	T hashGet(String key, String field);
	Map<String, T> getAllHashFields(String key);
	List<T> hashGetByFieldPrefix(String key, String fieldPrefix);
	void deleteHashField(String key, String field);
	void deleteHashFields(String key, List<String> fields);

	// Other Utilities
	Set<String> scanKeys(String pattern);
}
