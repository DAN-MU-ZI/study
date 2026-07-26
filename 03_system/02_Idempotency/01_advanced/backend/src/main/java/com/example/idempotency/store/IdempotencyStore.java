package com.example.idempotency.store;

import com.example.idempotency.dto.PaymentDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
public class IdempotencyStore {

    private static final String LOCK_PREFIX = "idempotency:lock:";
    private static final String RESULT_PREFIX = "idempotency:result:";
    private static final String CHANNEL_PREFIX = "idempotency:done:";
    private static final String ORDER_SCOPE = "order:";
    private static final RedisScript<Long> UNLOCK_SCRIPT = createUnlockScript();

    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer listenerContainer;
    private final ObjectMapper objectMapper;

    public IdempotencyStore(
        StringRedisTemplate redisTemplate,
        RedisMessageListenerContainer listenerContainer,
        ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.listenerContainer = listenerContainer;
        this.objectMapper = objectMapper;
    }

    public String lock(String customerId, String key) {
        return lockByScope(customerId, ORDER_SCOPE + key);
    }

    public void saveSuccess(String customerId, String key, PaymentDto.Request request, PaymentDto.Response response) {
        saveRecordByScope(customerId, ORDER_SCOPE + key, IdempotencyRecord.success(request, response));
    }

    public void saveProcessing(String customerId, String key, PaymentDto.Request request) {
        saveRecordByScope(customerId, ORDER_SCOPE + key, IdempotencyRecord.processing(request));
    }

    public void saveFailure(String customerId, String key, PaymentDto.Request request, int statusCode, String message) {
        saveRecordByScope(customerId, ORDER_SCOPE + key, IdempotencyRecord.failure(request, new FailureRecord(statusCode, message)));
    }

    public void notifyComplete(String customerId, String key) {
        notifyCompleteByScope(customerId, ORDER_SCOPE + key);
    }

    public IdempotencyRecord getRecord(String customerId, String key) {
        return getRecordByScope(customerId, ORDER_SCOPE + key);
    }

    public IdempotencyRecord waitForResult(String customerId, String key, long timeoutMs) {
        return waitForResultByScope(customerId, ORDER_SCOPE + key, timeoutMs);
    }

    public void unlock(String customerId, String key, String ownerToken) {
        unlockByScope(customerId, ORDER_SCOPE + key, ownerToken);
    }

    private String lockByScope(String customerId, String scopeKey) {
        String redisKey = buildKey(LOCK_PREFIX, customerId, scopeKey);
        String ownerToken = UUID.randomUUID().toString();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(redisKey, ownerToken, Duration.ofMinutes(1));
        return Boolean.TRUE.equals(success) ? ownerToken : null;
    }

    private void notifyCompleteByScope(String customerId, String scopeKey) {
        String channel = buildKey(CHANNEL_PREFIX, customerId, scopeKey);
        redisTemplate.convertAndSend(channel, "DONE");
    }

    private IdempotencyRecord getRecordByScope(String customerId, String scopeKey) {
        String redisKey = buildKey(RESULT_PREFIX, customerId, scopeKey);
        String json = redisTemplate.opsForValue().get(redisKey);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, IdempotencyRecord.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize idempotency record", e);
        }
    }

    private IdempotencyRecord waitForResultByScope(String customerId, String scopeKey, long timeoutMs) {
        IdempotencyRecord existing = getRecordByScope(customerId, scopeKey);
        if (existing != null && existing.terminal()) {
            return existing;
        }

        CountDownLatch latch = new CountDownLatch(1);
        String channel = buildKey(CHANNEL_PREFIX, customerId, scopeKey);
        ChannelTopic topic = new ChannelTopic(channel);
        MessageListener listener = (message, pattern) -> latch.countDown();

        listenerContainer.addMessageListener(listener, topic);
        try {
            existing = getRecordByScope(customerId, scopeKey);
            if (existing != null && existing.terminal()) {
                return existing;
            }

            latch.await(timeoutMs, TimeUnit.MILLISECONDS);
            return getRecordByScope(customerId, scopeKey);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            listenerContainer.removeMessageListener(listener, topic);
        }
    }

    private void unlockByScope(String customerId, String scopeKey, String ownerToken) {
        if (ownerToken == null || ownerToken.isBlank()) {
            return;
        }
        String redisKey = buildKey(LOCK_PREFIX, customerId, scopeKey);
        redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(redisKey), ownerToken);
    }

    private void saveRecordByScope(String customerId, String scopeKey, IdempotencyRecord record) {
        try {
            String redisKey = buildKey(RESULT_PREFIX, customerId, scopeKey);
            String json = objectMapper.writeValueAsString(record);
            redisTemplate.opsForValue().set(redisKey, json, Duration.ofHours(24));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize idempotency record", e);
        }
    }

    private String buildKey(String prefix, String customerId, String idempotencyKey) {
        return prefix + "{" + customerId + "}:" + idempotencyKey;
    }

    private static RedisScript<Long> createUnlockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText("if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end");
        script.setResultType(Long.class);
        return script;
    }

    public record IdempotencyRecord(
        PaymentDto.Request request,
        State state,
        PaymentDto.Response response,
        FailureRecord failure
    ) {
        public static IdempotencyRecord processing(PaymentDto.Request request) {
            return new IdempotencyRecord(request, State.PROCESSING, null, null);
        }

        public static IdempotencyRecord success(PaymentDto.Request request, PaymentDto.Response response) {
            return new IdempotencyRecord(request, State.SUCCEEDED, response, null);
        }

        public static IdempotencyRecord failure(PaymentDto.Request request, FailureRecord failure) {
            return new IdempotencyRecord(request, State.FAILED, null, failure);
        }

        public boolean failed() {
            return state == State.FAILED;
        }

        public boolean processing() {
            return state == State.PROCESSING;
        }

        public boolean terminal() {
            return state == State.SUCCEEDED || state == State.FAILED;
        }

        public enum State {
            PROCESSING,
            SUCCEEDED,
            FAILED
        }
    }

    public record FailureRecord(int statusCode, String message) {
    }
}
