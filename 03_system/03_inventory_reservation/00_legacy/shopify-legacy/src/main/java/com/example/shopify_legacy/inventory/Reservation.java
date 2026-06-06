package com.example.shopify_legacy.inventory;

import java.util.List;

import com.example.shopify_legacy.checkout.CheckoutLine;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

@Entity
public class Reservation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private Long checkoutId;

	@Enumerated(EnumType.STRING)
	private ReservationStatus status;

	private String releaseReason;

	@ElementCollection
	@CollectionTable(
		name = "reservation_lines",
		joinColumns = @JoinColumn(name = "reservation_id")
	)
	private List<ReservationLine> lines;

	protected Reservation() {
	}

	private Reservation(Long checkoutId, List<ReservationLine> lines) {
		this.checkoutId = checkoutId;
		this.lines = lines;
		this.status = ReservationStatus.RESERVED;
	}

	public static Reservation reserved(Long checkoutId, List<CheckoutLine> lines) {
		List<ReservationLine> reservationLines = lines.stream()
			.map(line -> new ReservationLine(line.inventoryItemId(), line.quantity()))
			.toList();

		return new Reservation(checkoutId, reservationLines);
	}

	public void claim() {
		if (status == ReservationStatus.CLAIMED) {
			return;
		}

		if (status == ReservationStatus.RELEASED) {
			throw new IllegalStateException("ALREADY_RELEASED");
		}
	
		this.status = ReservationStatus.CLAIMED;
	}

	public void release(String reason) {
		if (status == ReservationStatus.RELEASED) {
			return;
		}

		if (status == ReservationStatus.CLAIMED) {
			throw new IllegalStateException("ALREADY_CLAIMED");
		}
	
		this.status = ReservationStatus.RELEASED;
		this.releaseReason = reason;
	}

	public boolean isClaimed() {
		return this.status == ReservationStatus.CLAIMED;
	}

	public boolean isReleased() {
		return this.status == ReservationStatus.RELEASED;
	}

	public Long getId() {
		return id;
	}

	public List<ReservationLine> getLines() {
		return lines;
	}
}
