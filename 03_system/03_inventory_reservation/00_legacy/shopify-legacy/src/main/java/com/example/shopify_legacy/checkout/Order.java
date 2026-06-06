package com.example.shopify_legacy.checkout;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    private Long checkoutId;
    private String paymentId;

    private Order(Long checkoutId, String paymentId) {
        this.checkoutId = checkoutId;
        this.paymentId = paymentId;
    }

    public static Order completed(Long checkoutId, String paymentId) {
        return new Order(checkoutId, paymentId);
    }
}
