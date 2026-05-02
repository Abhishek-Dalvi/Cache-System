package com.design.cache_system.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.design.cache_system.repo.CacheEntry;
import com.design.cache_system.repo.MockDB;
import com.design.cache_system.repo.MockDBRepository;

@Service("inMemoryCacheService")
public class InMemoryCacheSystemService {
	
	private final ConcurrentHashMap<String, CacheEntry> myMap = new ConcurrentHashMap<>();
	
	@Autowired
	MockDBRepository mockDbRepo;
	
	@Value("${cache.ttlSec:5L}")
	private long expiredIn; //5 seconds for jedis.setex
	
	private final Map<String, Object> locks = new ConcurrentHashMap<>();
	
	public String getValue(String key) throws Exception {
		
		CacheEntry entry = myMap.get(key);
		
		if(entry == null || entry.getExpiryTime() < System.currentTimeMillis()) {
			Object lock = locks.computeIfAbsent(key, k -> new Object());
			// Synchronized block
			synchronized (lock) {
				entry = myMap.get(key);
				if(entry == null || entry.getExpiryTime() < System.currentTimeMillis()) {
					String repoVal = mockDbRepo.findByKey(key);
					if (repoVal==null) {
						throw new Exception("Key: " + key + " doesn't exist!");
					}
					fetchDBupdateCache(key, repoVal);
				}
			}
		}
		
		return myMap.get(key).getValue();
	}
	
	private void fetchDBupdateCache(String key, String repoVal) {
		long currentTime = System.currentTimeMillis();
		long expTime = currentTime + expiredIn * 1000;
		CacheEntry cacheEntry = new CacheEntry(repoVal, expTime);
		myMap.put(key, cacheEntry);
	}
	
	public void setKV(String Key, String Val) {
		MockDB mockDB = new MockDB(Key, Val);
		mockDbRepo.save(mockDB);
	}

}
