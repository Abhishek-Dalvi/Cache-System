local key = KEYS[1]
local threadId = ARGV[1]
local ttl = tonumber(ARGV[2])

if redis.call('SETNX', key, threadId) == 1 then
    redis.call('PEXPIRE', key, ttl)
    return 1
else
    return 0
end