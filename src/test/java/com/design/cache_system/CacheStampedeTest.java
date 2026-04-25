package com.design.cache_system;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.design.cache_system.repo.MockDB;
import com.design.cache_system.repo.MockDBRepository;
import com.design.cache_system.services.InMemoryCacheSystemService;

@SpringBootTest
public class CacheStampedeTest {
	
	@Autowired
	private MockDBRepository mockDBRepository;
	
	@Autowired
	private InMemoryCacheSystemService inMemoryCacheSystemService;
	
	@Test
	@Execution(ExecutionMode.SAME_THREAD)
	public void stampedeTest() throws InterruptedException {
		
		String randomString = UUID.randomUUID().toString();
		
		MockDB mockDB = new MockDB(randomString, "Hello:"+randomString);
		
		mockDBRepository.save(mockDB);
		
		int threadCounts = 50;
		CountDownLatch startLatch = new CountDownLatch(1);
		
		for (int i = 0; i < threadCounts; i++) {
            Thread t = new Thread(() -> {
                try {
                    startLatch.await(); // wait until latch is released
                    inMemoryCacheSystemService.getValue(randomString);
                    
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            });
            t.start();
        }
		
		// Simulate setup work before starting all threads
		Thread.sleep(1000);
        System.out.println("Releasing latch, all threads start together!");
        startLatch.countDown(); // release all threads
        
        // Fetching DB hit count after completion of threads
        Thread.sleep(1000);
        AtomicInteger dbHits = mockDBRepository.getAtomicInteger();
        
        assert (dbHits.get() ==  threadCounts);
	}

}
