package com.design.cache_system.services;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.design.cache_system.repo.MockDB;
import com.design.cache_system.repo.MockDBRepository;

@Service
public class InMemoryCacheSystemService {
	
	private ConcurrentHashMap<String, String> myMap = new ConcurrentHashMap<>();
	
	@Autowired
	MockDBRepository mockDbRepo;
	
	public String getValue(String key) throws Exception {
		
		String value;
		
		if(myMap.contains(key)) {
			value = myMap.get(key);
			
		} else if(mockDbRepo.findByKey(key)!=null) {
			value = mockDbRepo.findByKey(key);
			myMap.put(key, value);
			
		} else {
			
			throw new Exception("Key: " + key + " doesn't exist!");
		}
		
		return value;
		
	}
	
	public void setKV(String Key, String Val) {
		MockDB mockDB = new MockDB(Key, Val);
		
		mockDbRepo.save(mockDB);
		myMap.put(Key, Val);
	}

}
