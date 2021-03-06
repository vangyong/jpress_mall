/**
 * Copyright (c) 2015-2016, Michael Yang 杨福海 (fuhai999@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jpress.cache.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.jfinal.plugin.ehcache.IDataLoader;
import com.jfinal.plugin.redis.Redis;

import io.jpress.cache.ICache;
import io.jpress.utils.StringUtils;

/**
 * redis的缓存实现
 */
public class RedisCache implements ICache {
	@Override
	public <T> T get(String cacheName, Object key) {
		return Redis.use("redis1").hget(cacheName, key);
	}

	@Override
	public void put(String cacheName, Object key, Object value) {
	    Redis.use("redis1").hset(cacheName, key, value);
	}

	@Override
	public List<?> getKeys(String cacheName) {
	    Set<Object> set = Redis.use("redis1").hkeys(cacheName);
	    String[] aa = new String[set.size()];
        set.toArray(aa);
	    return Arrays.asList(aa);
	}

	@Override
	public void remove(String cacheName, Object key) {
	    Redis.use("redis1").hdel(cacheName, key);
	}

	@Override
	public void removeAll(String cacheName) {
	    Redis.use("redis1").del(cacheName);
	}

	@SuppressWarnings("unchecked")
    @Override
	public <T> T get(String cacheName, Object key, IDataLoader dataLoader) {
	    Object data = get(cacheName, key);
	    if (data == null) {
	        data = dataLoader.load();
            put(cacheName, key, data);
	    } 
		return (T)data;
	}
}
