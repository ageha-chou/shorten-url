-- Sliding window rate limiter using sorted set (race-condition free)
-- This uses a single key with timestamp-based members
-- KEYS[1]: The rate limiter key (e.g., "rate-limiter::client123")
-- ARGV[1]: Current timestamp in milliseconds
-- ARGV[2]: Window size in milliseconds
-- ARGV[3]: Rate limit (max requests)
-- ARGV[4]: TTL in milliseconds
-- ARGV[5]: Unique request ID (timestamp + random)

local key = KEYS[1]
local now = tonumber(ARGV[1])
local windowMs = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])
local ttlMs = tonumber(ARGV[4])
local requestId = ARGV[5]

-- Validate arguments (helps catch passing errors)
if not now or not windowMs or not limit or not ttlMs or not requestId then
    local errorMsg = "Invalid arguments: now=" .. tostring(ARGV[1]) ..
            ", windowMs=" .. tostring(ARGV[2]) ..
            ", limit=" .. tostring(ARGV[3]) ..
            ", ttlMs=" .. tostring(ARGV[4]) ..
            ", requestId=" .. tostring(ARGV[5]);
    redis.log(redis.LOG_WARNING, errorMsg)
    return redis.error_reply(errorMsg)
end

-- Remove old entries outside the sliding window
local windowStart = now - windowMs
redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)

-- Add the request FIRST (optimistic approach)
redis.call('ZADD', key, now, requestId)

-- Now count total requests INCLUDING the one we just added
local count = redis.call('ZCARD', key)

-- Check if we exceeded the limit
if count > limit then
    -- Exceeded! Remove the request we just added
    redis.call('ZREM', key, requestId)
    return 0  -- Deny
end

-- Within the limit, keep the request and set TTL
redis.call('PEXPIRE', key, ttlMs)

return 1