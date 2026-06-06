package com.example.shopify_legacy.inventory;

import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
public class ReservationLine {
    
    @Getter
    private Long inventoryItemId;
    @Getter
    private long quantity;

    protected ReservationLine() {
    }

    public ReservationLine(Long inventoryItemId, long quantity) {
        this.inventoryItemId = inventoryItemId;
        this.quantity = quantity;
    }
}
