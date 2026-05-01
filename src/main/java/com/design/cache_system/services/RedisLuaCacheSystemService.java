package com.design.cache_system.services;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.design.cache_system.repo.MockDB;
import com.design.cache_system.repo.MockDBRepository;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service("redisLuaCacheService")
public class RedisLuaCacheSystemService {
	
	private final JedisPool jedisPool;
	private final int expiredIn = 50;
	private final long threadLockTime = 2000L;
	
	@Autowired
	MockDBRepository mockDbRepo;
	
	@Autowired
	public RedisLuaCacheSystemService(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}
	
	public String getValue(String key) throws Exception {
		String cacheVal;
		try (Jedis jedis = jedisPool.getResource()) {
			if (!jedis.exists(key)) {
				
				// Loading Lua script
				InputStream stream = new ClassPathResource("scripts/lock_aquire_script.lua").getInputStream();
				String script = new Scanner(stream, StandardCharsets.UTF_8).useDelimiter("\\A").next();
				
				// Load script into Redis and get SHA1
	            String sha = jedis.scriptLoad(script);
	            
	            long threadId = Thread.currentThread().getId();

	            // JVM process ID (PID)
	            String jvmName = ManagementFactory.getRuntimeMXBean().getName();
	            String pid = jvmName.split("@")[0]; // before '@' is usually the PID

	            String uniqueThreadId = pid + "-" + threadId;
	            
	            System.out.println("Key doesn't exist for id: " + uniqueThreadId);
	            
	            String lockKey = "lock:" + key;
	            
	            long isTrue = (long) jedis.evalsha(sha, 1, lockKey, uniqueThreadId, String.valueOf(threadLockTime));
				
	            while (isTrue !=1) {
	            	System.out.println("Inside while loop for thread id: " + uniqueThreadId );
	            	Thread.sleep(20);
	            	// Checking if it is updated at redis to avoid loop forever
	            	cacheVal = jedis.get(key);
	            	if (cacheVal != null) {
	            		System.out.println("Breaking");
	            		break;
	            	}
	            	isTrue = (long) jedis.evalsha(sha, 1, key, uniqueThreadId, String.valueOf(threadLockTime));
	            }
	            
	            // This will allow only one thread will pass to update DB
	            if (!jedis.exists(key)) {
	            	System.out.println("Doing DB fetch for id: " +  uniqueThreadId);
	            	String repoVal = mockDbRepo.findByKey(key);
					if (repoVal==null) {
						throw new Exception("Key: " + key + " doesn't exist!");
					}
					jedis.setex(key,expiredIn, repoVal);
	            }
	            
			}
			
			cacheVal = jedis.get(key);
			System.out.println("cache value is: "+ cacheVal);
		}
		return cacheVal;
		
	}
	
	public void setKV(String Key, String Val) {
		MockDB mockDB = new MockDB(Key, Val);
		mockDbRepo.save(mockDB);
	}

}
