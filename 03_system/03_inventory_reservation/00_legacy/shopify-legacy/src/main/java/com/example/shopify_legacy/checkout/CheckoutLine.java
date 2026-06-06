package com.example.shopify_legacy.checkout;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

public record CheckoutLine (
    @NotNull Long inventoryItemId,
    @Min(1) int quantity
) {
}