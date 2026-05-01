package com.design.cache_system;

import java.util.UUID;

import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.design.cache_system.repo.MockDB;
import com.design.cache_system.repo.MockDBRepository;
import com.design.cache_system.services.RedisLuaCacheSystemService;

@SpringBootTest
public class CacheRetrievalTest {
	
	@Autowired
	private RedisLuaCacheSystemService inMemoryCacheSystemService;
	
	@Autowired
	private MockDBRepository mockDBRepository;
	
	@RepeatedTest(10)
	public void performanceTest() {
		try {
			String randomString = UUID.randomUUID().toString();
			
			MockDB mockDB = new MockDB(randomString, "Hello:"+randomString);
			
			mockDBRepository.save(mockDB);
			
			long a = System.currentTimeMillis();
			
			String val = inMemoryCacheSystemService.getValue(randomString);
			
			long b = System.currentTimeMillis();
			
			String valAgain = inMemoryCacheSystemService.getValue(randomString);
			
			long c = System.currentTimeMillis();
			
			
			assert ((b-a) > (c-b));
			assert val.equals(valAgain);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
