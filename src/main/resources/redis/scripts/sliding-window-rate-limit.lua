-- Sliding window rate limiter
-- KEYS[1..n-1]: All sub-window keys to check (read-only)
-- KEYS[n]: Current sub-window key to increment (write)
-- ARGV[1]: Rate limit
-- ARGV[2]: TTL in milliseconds

local limit = tonumber(ARGV[1])
local ttlMs = tonumber(ARGV[2])

-- The last key is the current key to increment
local currentKey = KEYS[#KEYS]

-- Sum all requests from sub-windows
local total = 0;
for i, key in ipairs(KEYS) do
    local count = redis.call('GET', key)
    if count then
        total = total + tonumber(count)
    end
end

-- Check and increment
if (total < limit) then
    redis.call('INCR', currentKey)
    redis.call('PEXPIRE', currentKey, ttlMs)
    return 1
end

return 0