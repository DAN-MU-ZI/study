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

    public String lock(String cartId) {
        return lockByScope(cartId);
    }

    public void saveSuccess(String cartId, PaymentDto.Request request, PaymentDto.Response response) {
        saveRecordByScope(cartId, IdempotencyRecord.success(request, response));
    }

    public void saveProcessing(String cartId, PaymentDto.Request request) {
        saveRecordByScope(cartId, IdempotencyRecord.processing(request));
    }

    public void saveFailure(String cartId, PaymentDto.Request request, int statusCode, String message) {
        saveRecordByScope(cartId, IdempotencyRecord.failure(request, new FailureRecord(statusCode, message)));
    }

    public void notifyComplete(String cartId) {
        notifyCompleteByScope(cartId);
    }

    public IdempotencyRecord getRecord(String cartId) {
        return getRecordByScope(cartId);
    }

    public IdempotencyRecord waitForResult(String cartId, long timeoutMs) {
        return waitForResultByScope(cartId, timeoutMs);
    }

    public void unlock(String cartId, String ownerToken) {
        unlockByScope(cartId, ownerToken);
    }

    private String lockByScope(String cartId) {
        return tryAcquireLock(buildKey(LOCK_PREFIX, cartId));
    }

    private void notifyCompleteByScope(String cartId) {
        String channel = buildKey(CHANNEL_PREFIX, cartId);
        redisTemplate.convertAndSend(channel, "DONE");
    }

    private IdempotencyRecord getRecordByScope(String cartId) {
        String json = redisTemplate.opsForValue().get(buildKey(RESULT_PREFIX, cartId));
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, IdempotencyRecord.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize idempotency record", e);
        }
    }

    private IdempotencyRecord waitForResultByScope(String cartId, long timeoutMs) {
        IdempotencyRecord existing = getRecordByScope(cartId);
        if (existing != null && existing.terminal()) {
            return existing;
        }

        CountDownLatch latch = new CountDownLatch(1);
        String channel = buildKey(CHANNEL_PREFIX, cartId);
        ChannelTopic topic = new ChannelTopic(channel);
        MessageListener listener = (message, pattern) -> latch.countDown();

        listenerContainer.addMessageListener(listener, topic);
        try {
            existing = getRecordByScope(cartId);
            if (existing != null && existing.terminal()) {
                return existing;
            }

            latch.await(timeoutMs, TimeUnit.MILLISECONDS);
            return getRecordByScope(cartId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            listenerContainer.removeMessageListener(listener, topic);
        }
    }

    private void unlockByScope(String cartId, String ownerToken) {
        if (ownerToken == null || ownerToken.isBlank()) {
            return;
        }
        redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(buildKey(LOCK_PREFIX, cartId)), ownerToken);
    }

    private void saveRecordByScope(String cartId, IdempotencyRecord record) {
        try {
            String json = objectMapper.writeValueAsString(record);
            redisTemplate.opsForValue().set(buildKey(RESULT_PREFIX, cartId), json, Duration.ofHours(24));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize idempotency record", e);
        }
    }

    private String tryAcquireLock(String redisKey) {
        String ownerToken = UUID.randomUUID().toString();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(redisKey, ownerToken, Duration.ofMinutes(1));
        return Boolean.TRUE.equals(success) ? ownerToken : null;
    }

    private String buildKey(String prefix, String cartId) {
        return prefix + "cart:" + cartId;
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
