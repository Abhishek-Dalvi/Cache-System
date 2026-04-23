package com.design.cache_system.services;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.design.cache_system.repo.CacheEntry;
import com.design.cache_system.repo.MockDB;
import com.design.cache_system.repo.MockDBRepository;

@Service
public class InMemoryCacheSystemService {
	
	private final ConcurrentHashMap<String, CacheEntry> myMap = new ConcurrentHashMap<>();
	
	@Autowired
	MockDBRepository mockDbRepo;
	
	private final long TTL = 5000L;
	
	public String getValue(String key) throws Exception {
		
		CacheEntry cacheEntry;
		String repoVal;
		
		if(!myMap.containsKey(key)) {
			repoVal = mockDbRepo.findByKey(key);
			if (repoVal==null) {
				throw new Exception("Key: " + key + " doesn't exist!");
			}
			long currentTime = System.currentTimeMillis();
			long expTime = currentTime + TTL;
			cacheEntry = new CacheEntry(repoVal, expTime);
			myMap.put(key, cacheEntry);
			return repoVal;
		} else {
			CacheEntry cacheVal = myMap.get(key);
			
			if(cacheVal.getExpiryTime()> System.currentTimeMillis()) {
				return cacheVal.getValue();
			} else {
				repoVal = mockDbRepo.findByKey(key);
				if (repoVal==null) {
					throw new Exception("Key: " + key + " doesn't exist!");
				}
				long currentTime = System.currentTimeMillis();
				long expTime = currentTime + TTL;
				cacheEntry = new CacheEntry(repoVal, expTime);
				myMap.put(key, cacheEntry);
				return repoVal;
			}
		}
	}
	
	public void setKV(String Key, String Val) {
		MockDB mockDB = new MockDB(Key, Val);
		mockDbRepo.save(mockDB);
	}

}
