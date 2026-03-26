package com.example.idempotency.config;

import com.example.idempotency.domain.OrderStatus;
import com.example.idempotency.dto.PaymentDto;
import com.example.idempotency.store.IdempotencyStore;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdempotencyAspectTest {

    private IdempotencyStore idempotencyStore;
    private IdempotencyAspect aspect;

    @BeforeEach
    void setUp() {
        idempotencyStore = mock(IdempotencyStore.class);
        aspect = new IdempotencyAspect(idempotencyStore);
    }

    @Test
    void whenIdempotencyKeyIsMissing_thenProceed() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Object[] args = new Object[]{null, new PaymentDto.Request("1001", "cust-001", 15_000L)};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.handleIdempotency(joinPoint, mock(Idempotent.class));

        assertThat(result).isEqualTo("success");
    }

    @Test
    void whenInvalidOwnership_thenThrowForbidden() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Object[] args = new Object[]{"wronguser:ULID123", new PaymentDto.Request("1001", "cust-001", 15_000L)};
        when(joinPoint.getArgs()).thenReturn(args);

        assertThatThrownBy(() -> aspect.handleIdempotency(joinPoint, mock(Idempotent.class)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Idempotency key does not belong to this customer");
    }

    @Test
    void whenValidOwnershipAndNoExisting_thenProceedAndSave() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        PaymentDto.Request request = new PaymentDto.Request("1001", "cust-001", 15_000L);
        Object[] args = new Object[]{"cust-001:ULID123", request};
        String lockToken = "lock-token-1";

        when(joinPoint.getArgs()).thenReturn(args);
        when(idempotencyStore.getRecord("cust-001", "1001"))
            .thenReturn((IdempotencyStore.IdempotencyRecord) null)
            .thenReturn((IdempotencyStore.IdempotencyRecord) null);
        when(idempotencyStore.lock("cust-001", "1001")).thenReturn(lockToken);

        PaymentDto.Response response = new PaymentDto.Response("1001", "pay-1", "pg-1", OrderStatus.PAID, Instant.now());
        when(joinPoint.proceed()).thenReturn(response);

        Object result = aspect.handleIdempotency(joinPoint, mock(Idempotent.class));

        assertThat(result).isEqualTo(response);
        verify(idempotencyStore).saveProcessing("cust-001", "1001", request);
        verify(idempotencyStore).saveSuccess("cust-001", "1001", request, response);
        verify(idempotencyStore).notifyComplete("cust-001", "1001");
        verify(idempotencyStore).unlock("cust-001", "1001", lockToken);
    }

    @Test
    void whenRecordAppearsAfterLock_thenReturnCachedWithoutProceeding() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        PaymentDto.Request request = new PaymentDto.Request("1001", "cust-001", 15_000L);
        Object[] args = new Object[]{"cust-001:ULID123", request};
        String lockToken = "lock-token-2";

        PaymentDto.Response cachedResponse = new PaymentDto.Response("1001", "pay-cached", "pg-1", OrderStatus.PAID, Instant.now());
        IdempotencyStore.IdempotencyRecord cachedRecord = IdempotencyStore.IdempotencyRecord.success(request, cachedResponse);

        when(joinPoint.getArgs()).thenReturn(args);
        when(idempotencyStore.getRecord("cust-001", "1001")).thenReturn(null, cachedRecord);
        when(idempotencyStore.lock("cust-001", "1001")).thenReturn(lockToken);

        Object result = aspect.handleIdempotency(joinPoint, mock(Idempotent.class));

        assertThat(result).isEqualTo(cachedResponse);
        verify(joinPoint, never()).proceed();
        verify(idempotencyStore, never()).saveSuccess(any(), any(), any(), any());
        verify(idempotencyStore).unlock("cust-001", "1001", lockToken);
    }

    @Test
    void whenExistingProcessingAndAnotherWorkerOwnsLock_thenWaitForStoredResult() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        PaymentDto.Request request = new PaymentDto.Request("1002", "cust-001", 15_000L);
        Object[] args = new Object[]{"cust-001:ULIDWAIT", request};
        PaymentDto.Response storedResponse = new PaymentDto.Response("1002", "pay-2", "pg-2", OrderStatus.PAID, Instant.now());

        when(joinPoint.getArgs()).thenReturn(args);
        when(idempotencyStore.getRecord("cust-001", "1002")).thenReturn(IdempotencyStore.IdempotencyRecord.processing(request));
        when(idempotencyStore.lock("cust-001", "1002")).thenReturn(null);
        when(idempotencyStore.waitForResult("cust-001", "1002", 3_000L))
            .thenReturn(IdempotencyStore.IdempotencyRecord.success(request, storedResponse));

        Object result = aspect.handleIdempotency(joinPoint, mock(Idempotent.class));

        assertThat(result).isEqualTo(storedResponse);
        verify(joinPoint, never()).proceed();
    }

    @Test
    void whenExistingProcessingAndLockIsRecovered_thenResumeProcessing() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        PaymentDto.Request request = new PaymentDto.Request("1002", "cust-001", 15_000L);
        Object[] args = new Object[]{"cust-001:ULIDRECOVER", request};
        String lockToken = "lock-token-recover";
        IdempotencyStore.IdempotencyRecord processingRecord = IdempotencyStore.IdempotencyRecord.processing(request);
        PaymentDto.Response response = new PaymentDto.Response("1002", "pay-2", "pg-2", OrderStatus.PAID, Instant.now());

        when(joinPoint.getArgs()).thenReturn(args);
        when(idempotencyStore.getRecord("cust-001", "1002")).thenReturn(processingRecord, processingRecord);
        when(idempotencyStore.lock("cust-001", "1002")).thenReturn(lockToken);
        when(joinPoint.proceed()).thenReturn(response);

        Object result = aspect.handleIdempotency(joinPoint, mock(Idempotent.class));

        assertThat(result).isEqualTo(response);
        verify(idempotencyStore).saveProcessing("cust-001", "1002", request);
        verify(idempotencyStore).saveSuccess("cust-001", "1002", request, response);
        verify(idempotencyStore).unlock("cust-001", "1002", lockToken);
    }

    @Test
    void whenValidOwnershipAndExisting_thenValidateAndReturnCached() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        PaymentDto.Request request = new PaymentDto.Request("1001", "cust-001", 15_000L);
        Object[] args = new Object[]{"cust-001:ULID123", request};

        when(joinPoint.getArgs()).thenReturn(args);

        PaymentDto.Response cachedResponse = new PaymentDto.Response("1001", "pay-cached", "pg-1", OrderStatus.PAID, Instant.now());
        IdempotencyStore.IdempotencyRecord cachedRecord = IdempotencyStore.IdempotencyRecord.success(request, cachedResponse);
        when(idempotencyStore.getRecord("cust-001", "1001")).thenReturn(cachedRecord);

        Object result = aspect.handleIdempotency(joinPoint, mock(Idempotent.class));

        assertThat(result).isEqualTo(cachedResponse);
        verify(joinPoint, never()).proceed();
        verify(idempotencyStore, never()).saveSuccess(any(), any(), any(), any());
    }

    @Test
    void whenWaitingForExistingFailure_thenThrowStoredFailure() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        PaymentDto.Request request = new PaymentDto.Request("1002", "cust-001", 15_000L);
        Object[] args = new Object[]{"cust-001:ULID999", request};

        when(joinPoint.getArgs()).thenReturn(args);
        when(idempotencyStore.getRecord("cust-001", "1002")).thenReturn(null);
        when(idempotencyStore.lock("cust-001", "1002")).thenReturn(null);
        when(idempotencyStore.waitForResult("cust-001", "1002", 3_000L))
            .thenReturn(IdempotencyStore.IdempotencyRecord.failure(
                request,
                new IdempotencyStore.FailureRecord(HttpStatus.CONFLICT.value(), "Order already processed: 1002")
            ));

        assertThatThrownBy(() -> aspect.handleIdempotency(joinPoint, mock(Idempotent.class)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Order already processed: 1002");
    }

    @Test
    void whenWaitingResultStillProcessing_thenThrowConflict() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        PaymentDto.Request request = new PaymentDto.Request("1002", "cust-001", 15_000L);
        Object[] args = new Object[]{"cust-001:ULIDTIMEOUT", request};

        when(joinPoint.getArgs()).thenReturn(args);
        when(idempotencyStore.getRecord("cust-001", "1002")).thenReturn(null);
        when(idempotencyStore.lock("cust-001", "1002")).thenReturn(null);
        when(idempotencyStore.waitForResult("cust-001", "1002", 3_000L))
            .thenReturn(IdempotencyStore.IdempotencyRecord.processing(request));

        assertThatThrownBy(() -> aspect.handleIdempotency(joinPoint, mock(Idempotent.class)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Payment processing is still in progress for this key");
    }

    @Test
    void whenWaitingForExistingWithDifferentPayload_thenThrowBadRequest() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        PaymentDto.Request incoming = new PaymentDto.Request("1002", "cust-001", 9_900L);
        PaymentDto.Request original = new PaymentDto.Request("1002", "cust-001", 15_000L);
        Object[] args = new Object[]{"cust-001:ULID1000", incoming};

        when(joinPoint.getArgs()).thenReturn(args);
        when(idempotencyStore.getRecord("cust-001", "1002")).thenReturn(null);
        when(idempotencyStore.lock("cust-001", "1002")).thenReturn(null);
        when(idempotencyStore.waitForResult("cust-001", "1002", 3_000L))
            .thenReturn(IdempotencyStore.IdempotencyRecord.success(
                original,
                new PaymentDto.Response("1002", "pay-2", "pg-2", OrderStatus.PAID, Instant.now())
            ));

        assertThatThrownBy(() -> aspect.handleIdempotency(joinPoint, mock(Idempotent.class)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Idempotency key used with different request payload");
    }

    @Test
    void whenBusinessLogicFails_thenPersistFailureAndNotifyWaiters() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        PaymentDto.Request request = new PaymentDto.Request("1003", "cust-001", 0L);
        Object[] args = new Object[]{"cust-001:ULIDFAIL", request};
        String lockToken = "lock-token-3";

        when(joinPoint.getArgs()).thenReturn(args);
        when(idempotencyStore.getRecord("cust-001", "1003"))
            .thenReturn((IdempotencyStore.IdempotencyRecord) null)
            .thenReturn((IdempotencyStore.IdempotencyRecord) null);
        when(idempotencyStore.lock("cust-001", "1003")).thenReturn(lockToken);
        when(joinPoint.proceed()).thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be positive"));

        assertThatThrownBy(() -> aspect.handleIdempotency(joinPoint, mock(Idempotent.class)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("amount must be positive");

        verify(idempotencyStore).saveProcessing("cust-001", "1003", request);
        verify(idempotencyStore).saveFailure("cust-001", "1003", request, 400, "amount must be positive");
        verify(idempotencyStore).notifyComplete("cust-001", "1003");
        verify(idempotencyStore).unlock("cust-001", "1003", lockToken);
    }
}
