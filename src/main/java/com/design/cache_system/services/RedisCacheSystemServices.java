package com.design.cache_system.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.design.cache_system.repo.MockDB;
import com.design.cache_system.repo.MockDBRepository;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service("redisCacheService")
public class RedisCacheSystemServices {
	
	private final JedisPool jedisPool;
	private final int expiredIn = 5;
	
	@Autowired
	MockDBRepository mockDbRepo;
	
	@Autowired
	public RedisCacheSystemServices(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}
	
	public String getValue(String key) throws Exception {
		String cacheVal;
		try (Jedis jedis = jedisPool.getResource()) {
			if (!jedis.exists(key)) {
				String repoVal = mockDbRepo.findByKey(key);
				if (repoVal==null) {
					throw new Exception("Key: " + key + " doesn't exist!");
				}
				jedis.setex(key,expiredIn, repoVal);
			}
			cacheVal = jedis.get(key);
		}
		return cacheVal;
		
	}
	
	public void setKV(String Key, String Val) {
		MockDB mockDB = new MockDB(Key, Val);
		mockDbRepo.save(mockDB);
	}
}
