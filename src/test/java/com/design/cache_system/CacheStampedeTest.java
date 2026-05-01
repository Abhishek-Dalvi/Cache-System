package com.design.cache_system;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.design.cache_system.repo.MockDB;
import com.design.cache_system.repo.MockDBRepository;
import com.design.cache_system.services.InMemoryCacheSystemService;
import com.design.cache_system.services.RedisCacheSystemServices;
import com.design.cache_system.services.RedisLuaCacheSystemService;

@SpringBootTest
public class CacheStampedeTest {
	
	@Autowired
	private MockDBRepository mockDBRepository;
	
//	@Autowired
////	private InMemoryCacheSystemService inMemoryCacheSystemService;
//	private RedisCacheSystemServices redisCacheSystemServices;
	
	private final RedisCacheSystemServices redisCacheSystemServices;
	
	private final InMemoryCacheSystemService inMemoryCacheSystemService;
	
	private final RedisLuaCacheSystemService redisLuaCacheSystemService;
	
	// Constructor injection — Spring will supply the bean
    @Autowired
    CacheStampedeTest(RedisCacheSystemServices redisCacheSystemServices, InMemoryCacheSystemService inMemoryCacheSystemService, RedisLuaCacheSystemService redisLuaCacheSystemService) {
        this.redisCacheSystemServices = redisCacheSystemServices;
        this.inMemoryCacheSystemService = inMemoryCacheSystemService;
        this.redisLuaCacheSystemService = redisLuaCacheSystemService;
    }
	
	@RepeatedTest(20)
	@Execution(ExecutionMode.SAME_THREAD)
	public void stampedeTest() throws InterruptedException {
		
		String randomString = UUID.randomUUID().toString();
		
		MockDB mockDB = new MockDB(randomString, "Hello:"+randomString);
		
		mockDBRepository.save(mockDB);
		
		int threadCounts = 50;
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(threadCounts); // wait for completion
		
		for (int i = 0; i < threadCounts; i++) {
            Thread t = new Thread(() -> {
                try {
                    startLatch.await(); // wait until latch is released
//                    inMemoryCacheSystemService.getValue(randomString);
                    redisLuaCacheSystemService.getValue(randomString);
                    
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                } finally {
					doneLatch.countDown();
				}
            });
            t.start();
        }
		
		// Simulate setup work before starting all threads
        startLatch.countDown(); // release all threads
        
        // Wait until all threads finish
        doneLatch.await();
        
        AtomicInteger dbHits = mockDBRepository.getAtomicInteger();
        
        System.out.println(dbHits.get());
        
        assert (dbHits.get() ==  1);
	}

}
