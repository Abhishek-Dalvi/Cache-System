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

	public String findByKey(String key) throws Exception {
		try {
			Thread.sleep(200); // simulate latency
			return store.get(key);
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}
	}

	public void delete(String key) {
		store.remove(key);
	}

	public Map<String, String> findAll() throws Exception {
		try {
			Thread.sleep(100);
			return store;
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}

	}
}