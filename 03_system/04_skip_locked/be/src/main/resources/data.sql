INSERT INTO item (id, quantity) VALUES (1, 9999000);
INSERT INTO item (id, quantity) VALUES (2, 20000000);
INSERT INTO item (id, quantity) VALUES (3, 30000000);
INSERT INTO item (id, quantity) VALUES (4, 40000000);
INSERT INTO item (id, quantity) VALUES (5, 50000000);

INSERT INTO inventory_unit (item_id)
SELECT 1
FROM generate_series(1, 1000);

