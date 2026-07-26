--[[
Weighted Rate Limiting Lua Script - Atomic Sliding Window with Token Costs

KEYS[1] = rate limit key (e.g., "rate_limit:192.168.1.1")
ARGV[1] = window start time (current_time - window_size)
ARGV[2] = current timestamp
ARGV[3] = request ID with cost (e.g., "uuid:5")
ARGV[4] = token limit (max tokens)
ARGV[5] = window size in seconds (for expiration)
ARGV[6] = token cost for this request

Returns:
  >= 0: remaining tokens (request allowed)
  -1: request denied (token limit exceeded)
--]]

local key = KEYS[1]
local window_start = tonumber(ARGV[1])
local now = tonumber(ARGV[2])
local request_id = ARGV[3]
local limit = tonumber(ARGV[4])
local window_size = tonumber(ARGV[5])
local cost = tonumber(ARGV[6])

-- Remove expired entries (atomic operation)
redis.call('ZREMRANGEBYSCORE', key, 0, window_start)

-- Get all members and calculate current token usage
local members = redis.call('ZRANGEBYSCORE', key, window_start, now)
local current_usage = 0

for i, member in ipairs(members) do
    -- Extract cost from member (format: "uuid:cost")
    local member_cost = tonumber(string.match(member, ":(%d+)$"))
    if member_cost then
        current_usage = current_usage + member_cost
    end
end

if current_usage + cost <= limit then
    -- Add new request with cost encoded
    redis.call('ZADD', key, now, request_id)
    -- Set expiration for automatic cleanup
    redis.call('EXPIRE', key, window_size + 1)
    -- Return remaining tokens
    return limit - current_usage - cost
else
    -- Token limit exceeded
    return -1
end
