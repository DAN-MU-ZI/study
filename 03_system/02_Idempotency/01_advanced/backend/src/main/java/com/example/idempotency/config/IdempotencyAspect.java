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

    private final IdempotencyStore idempotencyStore;

    public IdempotencyAspect(IdempotencyStore idempotencyStore) {
        this.idempotencyStore = idempotencyStore;
    }

    @Around("@annotation(idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String idempotencyKey = (String) args[0];
        PaymentDto.Request request = (PaymentDto.Request) args[1];

        boolean useIdempotency = idempotencyKey != null && !idempotencyKey.isBlank();
        if (!useIdempotency) {
            return joinPoint.proceed();
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment request is required");
        }

        String customerId = request.customerId();
        String orderId = request.orderId();
        validateKeyOwnership(idempotencyKey, customerId);

        IdempotencyStore.IdempotencyRecord existing = idempotencyStore.getRecord(customerId, orderId);
        if (existing != null) {
            validateRequestConsistency(existing.request(), request);
            if (existing.terminal()) {
                return resolveRecord(existing, request);
            }
        }

        String lockToken = idempotencyStore.lock(customerId, orderId);
        if (lockToken == null) {
            return waitForExistingResult(customerId, orderId, request);
        }

        try {
            existing = idempotencyStore.getRecord(customerId, orderId);
            if (existing != null) {
                validateRequestConsistency(existing.request(), request);
                if (existing.terminal()) {
                    return resolveRecord(existing, request);
                }
            }

            idempotencyStore.saveProcessing(customerId, orderId, request);
            PaymentDto.Response response = (PaymentDto.Response) joinPoint.proceed();
            idempotencyStore.saveSuccess(customerId, orderId, request, response);
            idempotencyStore.notifyComplete(customerId, orderId);
            return response;
        } catch (Throwable throwable) {
            IdempotencyStore.FailureRecord failure = toFailureRecord(throwable);
            idempotencyStore.saveFailure(customerId, orderId, request, failure.statusCode(), failure.message());
            idempotencyStore.notifyComplete(customerId, orderId);
            throw throwable;
        } finally {
            idempotencyStore.unlock(customerId, orderId, lockToken);
        }
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency key used with different request payload");
        }
    }

    private PaymentDto.Response waitForExistingResult(String customerId, String idempotencyKey, PaymentDto.Request request) {
        IdempotencyStore.IdempotencyRecord record = idempotencyStore.waitForResult(customerId, idempotencyKey, WAIT_TIMEOUT_MS);
        if (record != null) {
            validateRequestConsistency(record.request(), request);
            if (record.processing()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment processing is still in progress for this key");
            }
            return resolveRecord(record, request);
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment processing timed out for this key");
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
