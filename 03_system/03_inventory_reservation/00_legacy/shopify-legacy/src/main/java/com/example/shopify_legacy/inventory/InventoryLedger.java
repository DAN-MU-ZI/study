package com.example.shopify_legacy.inventory;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryLedger {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long inventoryItemId;
	private long quantity;
	private String paymentId;
	private String type;
	private Instant createdAt;

	private InventoryLedger(Long inventoryItemId, long quantity, String paymentId, String type) {
		this.inventoryItemId = inventoryItemId;
		this.quantity = quantity;
		this.paymentId = paymentId;
		this.type = type;
		this.createdAt = Instant.now();
	}

	public static InventoryLedger claim(Long inventoryItemId, long quantity, String paymentId) {
		return new InventoryLedger(inventoryItemId, quantity, paymentId, "CLAIM");
	}
}
