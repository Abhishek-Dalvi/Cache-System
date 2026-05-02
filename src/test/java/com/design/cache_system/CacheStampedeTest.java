package com.design.cache_system;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	
	private static final Logger log = LoggerFactory.getLogger(CacheStampedeTest.class);
	
	/*
	 * Note for testing purpose we can just switch the service we want to test
	 * 1. In Memory cache system works on single pod for this test
	 * 2. Redis cache system doesn't work for stampede
	 * 3. Redis Lua script work on multi pod since logic is shifted towards redis server infra
	 */
	
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
	
	@RepeatedTest(100)
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
                    String cacheVal = redisLuaCacheSystemService.getValue(randomString);
                    assert cacheVal.equals("Hello:"+randomString);
                    
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
        
        log.debug("Total DB hits are: " + dbHits.get());
        
        assert (dbHits.get() ==  1);
	}

}
