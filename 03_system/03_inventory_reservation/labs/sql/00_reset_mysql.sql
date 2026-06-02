USE inventory_study;

DELETE FROM reserved_quantities;
DELETE FROM reservation_units;
DELETE FROM legacy_reservation_log;
DELETE FROM replenishment_state;
DELETE FROM inventory_ledger;

INSERT INTO inventory_ledger (
  shop_id,
  inventory_item_id,
  location_id,
  total_quantity,
  claimed_quantity
) VALUES
  (1, 100, 1, 100, 0);

INSERT INTO reservation_units (
  shop_id,
  inventory_item_id,
  location_id,
  unit_id
) VALUES
  (1, 100, 1, 1),
  (1, 100, 1, 2),
  (1, 100, 1, 3),
  (1, 100, 1, 4),
  (1, 100, 1, 5),
  (1, 100, 1, 6),
  (1, 100, 1, 7),
  (1, 100, 1, 8),
  (1, 100, 1, 9),
  (1, 100, 1, 10);

INSERT INTO replenishment_state (
  shop_id,
  inventory_item_id,
  location_id,
  next_unit_id,
  target_pool_size,
  low_watermark
) VALUES
  (1, 100, 1, 11, 10, 3);
