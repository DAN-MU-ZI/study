package com.example.idempotency.config;

import com.example.idempotency.dto.PaymentDto;
import com.example.idempotency.store.IdempotencyStore;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Aspect
@Component
public class IdempotencyAspect {

    private static final long WAIT_TIMEOUT_MS = 3_000L;
    private static final String MISSING_REQUEST_MESSAGE = "Payment request is required";
    private static final String INVALID_PAYLOAD_MESSAGE = "Idempotency key used with different request payload";
    private static final String PROCESSING_MESSAGE = "Payment processing is still in progress for this cart";
    private static final String TIMEOUT_MESSAGE = "Payment processing timed out for this cart";

    private final IdempotencyStore idempotencyStore;

    public IdempotencyAspect(IdempotencyStore idempotencyStore) {
        this.idempotencyStore = idempotencyStore;
    }

    @Around("@annotation(idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String idempotencyKey = extractIdempotencyKey(joinPoint.getArgs());
        if (!useIdempotency(idempotencyKey)) {
            return joinPoint.proceed();
        }

        PaymentDto.Request request = extractRequest(joinPoint.getArgs());
        String customerId = request.customerId();
        String cartId = request.cartId();
        validateKeyOwnership(idempotencyKey, customerId);

        IdempotencyStore.IdempotencyRecord existing = idempotencyStore.getRecord(cartId);
        if (existing != null) {
            validateRequestConsistency(existing.request(), request);
            if (existing.terminal()) {
                return resolveRecord(existing, request);
            }
        }

        String lockToken = idempotencyStore.lock(cartId);
        if (lockToken == null) {
            return waitForExistingResult(cartId, request);
        }
        try {
            existing = idempotencyStore.getRecord(cartId);
            if (existing != null) {
                validateRequestConsistency(existing.request(), request);
                if (existing.terminal()) {
                    return resolveRecord(existing, request);
                }
            }

            idempotencyStore.saveProcessing(cartId, request);
            PaymentDto.Response response = (PaymentDto.Response) joinPoint.proceed();
            idempotencyStore.saveSuccess(cartId, request, response);
            idempotencyStore.notifyComplete(cartId);
            return response;
        } catch (Throwable throwable) {
            IdempotencyStore.FailureRecord failure = toFailureRecord(throwable);
            idempotencyStore.saveFailure(cartId, request, failure.statusCode(), failure.message());
            idempotencyStore.notifyComplete(cartId);
            throw throwable;
        } finally {
            idempotencyStore.unlock(cartId, lockToken);
        }
    }

    private String extractIdempotencyKey(Object[] args) {
        return (String) args[0];
    }

    private PaymentDto.Request extractRequest(Object[] args) {
        PaymentDto.Request request = (PaymentDto.Request) args[1];
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MISSING_REQUEST_MESSAGE);
        }
        return request;
    }

    private boolean useIdempotency(String idempotencyKey) {
        return idempotencyKey != null && !idempotencyKey.isBlank();
    }

    private void validateKeyOwnership(String idempotencyKey, String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "customerId is required");
        }
        if (!idempotencyKey.startsWith(customerId + ":")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Idempotency key does not belong to this customer");
        }
    }

    private void validateRequestConsistency(PaymentDto.Request cached, PaymentDto.Request incoming) {
        if (!cached.equals(incoming)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_PAYLOAD_MESSAGE);
        }
    }

    private PaymentDto.Response waitForExistingResult(String cartId, PaymentDto.Request request) {
        IdempotencyStore.IdempotencyRecord record = idempotencyStore.waitForResult(cartId, WAIT_TIMEOUT_MS);
        if (record != null) {
            validateRequestConsistency(record.request(), request);
            if (record.processing()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, PROCESSING_MESSAGE);
            }
            return resolveRecord(record, request);
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, TIMEOUT_MESSAGE);
    }

    private PaymentDto.Response resolveRecord(IdempotencyStore.IdempotencyRecord record, PaymentDto.Request incoming) {
        validateRequestConsistency(record.request(), incoming);
        if (record.failed()) {
            throw toResponseStatusException(record.failure());
        }
        return record.response();
    }

    private IdempotencyStore.FailureRecord toFailureRecord(Throwable throwable) {
        if (throwable instanceof ResponseStatusException responseStatusException) {
            String message = responseStatusException.getReason();
            if (message == null || message.isBlank()) {
                message = responseStatusException.getMessage();
            }
            return new IdempotencyStore.FailureRecord(responseStatusException.getStatusCode().value(), message);
        }

        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            message = "Payment processing failed";
        }
        return new IdempotencyStore.FailureRecord(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }

    private ResponseStatusException toResponseStatusException(IdempotencyStore.FailureRecord failure) {
        HttpStatus status = HttpStatus.resolve(failure.statusCode());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ResponseStatusException(status, failure.message());
    }
}
