package com.design.cache_system.repo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

@Repository
public class MockDBRepository {
	
	
	private final Map<String, String> store = new ConcurrentHashMap<>();
	
	public void save(MockDB mockDB) {
		store.put(mockDB.getKey(), mockDB.getValue());
	}
	
	public String findByKey(String key) {
		return store.get(key);
	}
	
	public void delete(String key) {
		store.remove(key);
	}
	
	public Map<String, String> findAll() {
        return store;
    }
	
	
}