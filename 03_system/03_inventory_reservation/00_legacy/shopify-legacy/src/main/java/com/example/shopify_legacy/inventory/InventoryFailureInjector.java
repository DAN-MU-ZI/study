package com.example.shopify_legacy.inventory;

public interface InventoryFailureInjector {

    void afterLedgerSave();

    void afterRedisCleanup();
}
