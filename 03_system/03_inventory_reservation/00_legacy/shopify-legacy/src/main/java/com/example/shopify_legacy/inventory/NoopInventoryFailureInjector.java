package com.example.shopify_legacy.inventory;

import org.springframework.stereotype.Component;

@Component
public class NoopInventoryFailureInjector implements InventoryFailureInjector {

    @Override
    public void afterLedgerSave() {
    }

    @Override
    public void afterRedisCleanup() {
    }
}
