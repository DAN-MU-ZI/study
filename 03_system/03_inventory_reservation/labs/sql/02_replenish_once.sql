USE inventory_study;

START TRANSACTION;

SELECT
  shop_id,
  inventory_item_id,
  location_id,
  next_unit_id,
  target_pool_size,
  low_watermark
FROM replenishment_state
WHERE shop_id = 1
  AND inventory_item_id = 100
  AND location_id = 1
FOR UPDATE;

SELECT
  @should_replenish := CASE
    WHEN rs.next_unit_id <= il.total_quantity
     AND (
       SELECT COUNT(*)
       FROM reservation_units ru
       WHERE ru.shop_id = rs.shop_id
         AND ru.inventory_item_id = rs.inventory_item_id
         AND ru.location_id = rs.location_id
     ) < rs.target_pool_size
    THEN 1
    ELSE 0
  END
FROM replenishment_state rs
JOIN inventory_ledger il
  ON il.shop_id = rs.shop_id
 AND il.inventory_item_id = rs.inventory_item_id
 AND il.location_id = rs.location_id
WHERE rs.shop_id = 1
  AND rs.inventory_item_id = 100
  AND rs.location_id = 1;

INSERT IGNORE INTO reservation_units (
  shop_id,
  inventory_item_id,
  location_id,
  unit_id
)
SELECT
  rs.shop_id,
  rs.inventory_item_id,
  rs.location_id,
  rs.next_unit_id
FROM replenishment_state rs
JOIN inventory_ledger il
  ON il.shop_id = rs.shop_id
 AND il.inventory_item_id = rs.inventory_item_id
 AND il.location_id = rs.location_id
WHERE rs.shop_id = 1
  AND rs.inventory_item_id = 100
  AND rs.location_id = 1
  AND @should_replenish = 1;

UPDATE replenishment_state
SET next_unit_id = next_unit_id + 1
WHERE shop_id = 1
  AND inventory_item_id = 100
  AND location_id = 1
  AND @should_replenish = 1;

COMMIT;
