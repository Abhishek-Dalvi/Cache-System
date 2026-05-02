# Distributed Cache with Stampede Protection using Redis + Lua

## Summary: 
- Prevents cache stampede in distributed system by coordinating requests using Redis-based locking and Lua script

# Problem Statement
1. When multiple requests hit an expired or missing cache entry all of them query the database simultaneously (cache stampede), leading to DB overload and laterncy spikes.
2. This problem worsen in distributed systems where in-memory locks do not work across instance

# Solution Overview
-  Redis used as Centralized coordinate layer
- Lua script ensures atomic decision making
- Only one request fetches from DB
- Other waits and retries

# Architecture Diagram 
Client
    |
Service
    |
Redis (Lua)
    |
DB (only 1 call)

MISS_OWNER - DB fetch (Handle by creating lock with key as lock:uuid and value as thread id)
WAIT - retry (while loop, checking if cache update before trying again, break if update)
HIT -return cache

# Flow explanation
1. Request hit service
2. Lua script check redis:
    -   If value exists -> return (HIT)
    -   If not exist -> try to acquire lock
3. If lock acquired:
    -   Fetch from DB
    -   Update cache
    -   Release lock
4. If not:
    -   Retry after small delay

# Key concepts
1. Cache stampede: Concurrent cache misses for the same key causing multiple database calls, leading to load spikes and latency issues.
2. Request Coalescing: Combining multiple concurrent requests so that only one fetches data, while others wait and reuse the result.
3. Distributed locking: Using shared system (eg. Redis) to ensure only one process across multiple instances perform critical operation at a time.
4. Polling: Active retry with delays to reduce system load.
5. Blocking: A mechanism where a thread waits (its suspended) until condition is met, without actively retrying (Not implemented here)

# Tradeoffs
1. Lock TTL vs DB latency
    -   Short TTL: duplicate DB calls
    -   Long TTL: higher wait time if thread crash
2. Polling approach
    -   Simple but increase Redis load
3. Eventual consistency
    -  Small delay befor cache is updated

# Edge cases
-   Lock expiry before DB fetch completes -> Multiple DB calls
-   Owner thread crash -> lock holds until TTL
-   High rety frequency -> Redis overload
-   Stale data reintroduced (if write race exist)

# Handling stampede
-   Lock TTL tuned relative to DB latency
-   Retry with backoff instead of tight loop
-   Timeout handling for waiting threads

# Other cache problems
1. Stale reads
    -   Cache may return outdated data shortly after DB updates.
    -   Accepted as eventual consistency
2. Stale write-back
    -   In-flight reads may reintroduce old data into cache after invalidation. (Not handled in current implementation)
3. Lock expiry race:
    -   Lock may expire before DB fetch completes. allowing duplicate DB calls.
    -   Migrate via TTL tuning
4. Crash while holding lock:
    -   If owner fails, other requests wait until lock TTL expires.

# Testing strategy
1. Basic cache retrieval test to confirm if after first call, key-val is taking it from cache by intentionally introducing latency for DB fetch and check if second call is faster than first.
2. Concurrent stampede test:
    -   50 threads simultaneously initiating service function using Latch
    -   Verified DB call count (should be 1)
    -   Simulate latency using mock DB
    -   Repeated test to validate race condition

# Limitations
-   Use polling instead of push notification
-   Lock renewal not implemented
-   No distribued tracing / monitoring

# Future improvement
-   Stale-while-revalidating strategy
-   Pub/Sub instead of polling
-   Lock renewal mechanism
-   Multi-Key batching

# How to run
-   Start Redis (docker)
-   Run application
-   Execute concurrency test