local key = KEYS[1] 
local lockValue = ARGV[1]

if redis.call("GET", key) == lockValue then
    return redis.call("DEL", key)
else
    return 0
end