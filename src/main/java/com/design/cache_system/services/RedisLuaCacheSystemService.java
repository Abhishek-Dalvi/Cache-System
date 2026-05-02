package com.design.cache_system.services;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.design.cache_system.repo.MockDB;
import com.design.cache_system.repo.MockDBRepository;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service("redisLuaCacheService")
public class RedisLuaCacheSystemService {
	
	private final JedisPool jedisPool;
	
	@Value("${cache.ttlSec:5}")
	private int expiredIn; //5 seconds for jedis.setex
	
	@Value("${cache.threadttlMillisec:500L}")
	private long threadLockTime; //500 ms for lua script
	
	private static final Logger log = LoggerFactory.getLogger(RedisLuaCacheSystemService.class);
	
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
				
				// Loading Lua script for acquire lock
				InputStream streamLockAcquire = new ClassPathResource("scripts/lock_aquire_script.lua").getInputStream();
				String scriptLockAcquire = new Scanner(streamLockAcquire, StandardCharsets.UTF_8).useDelimiter("\\A").next();
				
				// Load script into Redis and get SHA1 for acquiring the lock
	            String shaAcquire = jedis.scriptLoad(scriptLockAcquire);
	            
	            // Loading Lua script for release lock
				InputStream streamLockRelease= new ClassPathResource("scripts/lock_release_script.lua").getInputStream();
				String scriptLockRelease= new Scanner(streamLockRelease, StandardCharsets.UTF_8).useDelimiter("\\A").next();
				
				// Load script into Redis and get SHA1 for releasing the lock
	            String shaRelease = jedis.scriptLoad(scriptLockRelease);
	            
	            long threadId = Thread.currentThread().getId();

	            // JVM process ID (PID)
	            String jvmName = ManagementFactory.getRuntimeMXBean().getName();
	            String pid = jvmName.split("@")[0]; // before '@' is usually the PID

	            String uniqueThreadId = pid + "-" + threadId;
	            
	            log.debug("Key doesn't exist for id: " + uniqueThreadId);
	            
	            String lockKey = "lock:" + key;
	            
	            long isTrue = (long) jedis.evalsha(shaAcquire, 1, lockKey, uniqueThreadId, String.valueOf(threadLockTime));
				
	            
	            // Loop for trying to acquire lock (Polling to the redis, all other thread except one which is move ahead for fetching DB, will stay in loop)
	            while (isTrue !=1) {
	            	log.debug("Inside while loop for thread id: " + uniqueThreadId );
	            	Thread.sleep(20);
	            	// Checking if it is updated at redis (by another thread which goes to fetch DB) to avoid loop forever
	            	cacheVal = jedis.get(key);
	            	if (cacheVal != null) {
	            		log.debug("Breaking for threadId:"+uniqueThreadId);
	            		break;
	            	}
	            	isTrue = (long) jedis.evalsha(shaAcquire, 1, lockKey, uniqueThreadId, String.valueOf(threadLockTime));
	            }
	            
	            // This will allow only one thread will pass to update DB
	            if (!jedis.exists(key)) {
	            	log.debug("Doing DB fetch for id: " +  uniqueThreadId);
	            	String repoVal = mockDbRepo.findByKey(key);
					if (repoVal==null) {
						throw new Exception("Key: " + key + " doesn't exist!");
					}
					jedis.setex(key,expiredIn, repoVal);
					
					//Releasing lock
					jedis.evalsha(shaRelease, 1, lockKey, uniqueThreadId);
	            }
	            
			}
			
			cacheVal = jedis.get(key);
			log.debug("cache value is: "+ cacheVal);
		}
		return cacheVal;
		
	}
	
	public void setKV(String Key, String Val) {
		MockDB mockDB = new MockDB(Key, Val);
		mockDbRepo.save(mockDB);
		log.debug("Key value stored in DB");
	}

}
