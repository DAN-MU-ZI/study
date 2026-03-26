package com.example.idempotency.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method whose payment execution should be guarded by idempotency handling.
 *
 * <p>The annotated method is intercepted by {@link IdempotencyAspect} and is expected to receive
 * {@code String idempotencyKey} as the first parameter and {@code PaymentDto.Request} as the
 * second parameter.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
}

