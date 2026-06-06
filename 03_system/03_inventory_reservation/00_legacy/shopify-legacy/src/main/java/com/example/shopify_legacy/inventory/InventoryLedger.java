package com.example.shopify_legacy.inventory;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class InventoryLedger {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long inventoryItemId;
	private long quantity;
	private String paymentId;
	private String type;
	private Instant createdAt;

	protected InventoryLedger() {
	}

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

	public Long getId() {
		return id;
	}
}
