USE inventory_study;

CREATE TABLE inventory_ledger (
  shop_id BIGINT NOT NULL,
  inventory_item_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  total_quantity INT NOT NULL,
  claimed_quantity INT NOT NULL DEFAULT 0,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (shop_id, inventory_item_id, location_id),
  CONSTRAINT chk_inventory_quantity CHECK (total_quantity >= 0),
  CONSTRAINT chk_claimed_quantity CHECK (claimed_quantity >= 0),
  CONSTRAINT chk_claimed_not_over_total CHECK (claimed_quantity <= total_quantity)
);

CREATE TABLE legacy_reservation_log (
  reservation_id VARCHAR(64) NOT NULL,
  shop_id BIGINT NOT NULL,
  inventory_item_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  status VARCHAR(20) NOT NULL,
  expires_at DATETIME NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (reservation_id),
  KEY idx_legacy_item_status (shop_id, inventory_item_id, location_id, status),
  CONSTRAINT chk_legacy_quantity CHECK (quantity > 0)
);

CREATE TABLE reservation_units (
  shop_id BIGINT NOT NULL,
  inventory_item_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  unit_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (shop_id, inventory_item_id, location_id, unit_id)
);

CREATE TABLE reserved_quantities (
  reservation_id VARCHAR(64) NOT NULL,
  shop_id BIGINT NOT NULL,
  inventory_item_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  unit_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  expires_at DATETIME NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (reservation_id, unit_id),
  KEY idx_reserved_item_status (shop_id, inventory_item_id, location_id, status),
  KEY idx_reserved_expires_at (expires_at),
  CONSTRAINT fk_reserved_unit_scope
    FOREIGN KEY (shop_id, inventory_item_id, location_id)
    REFERENCES inventory_ledger (shop_id, inventory_item_id, location_id)
);

CREATE TABLE replenishment_state (
  shop_id BIGINT NOT NULL,
  inventory_item_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  next_unit_id BIGINT NOT NULL,
  target_pool_size INT NOT NULL,
  low_watermark INT NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (shop_id, inventory_item_id, location_id),
  CONSTRAINT chk_target_pool_size CHECK (target_pool_size > 0),
  CONSTRAINT chk_low_watermark CHECK (low_watermark >= 0)
);
