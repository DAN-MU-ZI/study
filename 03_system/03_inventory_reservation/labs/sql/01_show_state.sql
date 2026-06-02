USE inventory_study;

SELECT
  shop_id,
  inventory_item_id,
  location_id,
  total_quantity,
  claimed_quantity,
  total_quantity - claimed_quantity AS ledger_available
FROM inventory_ledger;

SELECT
  shop_id,
  inventory_item_id,
  location_id,
  COUNT(*) AS reservation_unit_count,
  MIN(unit_id) AS min_unit_id,
  MAX(unit_id) AS max_unit_id
FROM reservation_units
GROUP BY shop_id, inventory_item_id, location_id;

SELECT
  status,
  COUNT(*) AS reserved_count
FROM reserved_quantities
GROUP BY status;

SELECT
  shop_id,
  inventory_item_id,
  location_id,
  next_unit_id,
  target_pool_size,
  low_watermark
FROM replenishment_state;
