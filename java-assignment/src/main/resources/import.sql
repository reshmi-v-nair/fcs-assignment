INSERT INTO store(id, name, quantityProductsInStock) VALUES (1, 'TONSTAD', 10);
INSERT INTO store(id, name, quantityProductsInStock) VALUES (2, 'KALLAX', 5);
INSERT INTO store(id, name, quantityProductsInStock) VALUES (3, 'BESTÅ', 3);
ALTER SEQUENCE store_seq RESTART WITH 4;

INSERT INTO product(id, name, stock) VALUES (1, 'TONSTAD', 10);
INSERT INTO product(id, name, stock) VALUES (2, 'KALLAX', 5);
INSERT INTO product(id, name, stock) VALUES (3, 'BESTÅ', 3);
ALTER SEQUENCE product_seq RESTART WITH 4;

INSERT INTO warehouse(id, businessUnitCode, location, capacity, stock, createdAt, archivedAt) 
VALUES (1, 'MWH.001', 'ZWOLLE-001', 100, 10, '2024-07-01', null);
INSERT INTO warehouse(id, businessUnitCode, location, capacity, stock, createdAt, archivedAt)
VALUES (2, 'MWH.012', 'AMSTERDAM-001', 50, 5, '2023-07-01', null);
INSERT INTO warehouse(id, businessUnitCode, location, capacity, stock, createdAt, archivedAt)
VALUES (3, 'MWH.023', 'TILBURG-001', 30, 27, '2021-02-01', null);
ALTER SEQUENCE warehouse_seq RESTART WITH 4;

-- Fulfillment assignments (bonus feature): store 1 (TONSTAD) is fulfilled by 2 warehouses for
-- product 1, staying within the max-2-warehouses-per-product-per-store and max-3-per-store rules.
INSERT INTO fulfillment_assignment(id, store_id, product_id, warehouseBusinessUnitCode, createdAt)
VALUES (1, 1, 1, 'MWH.001', '2024-07-02');
INSERT INTO fulfillment_assignment(id, store_id, product_id, warehouseBusinessUnitCode, createdAt)
VALUES (2, 1, 1, 'MWH.012', '2024-07-02');
INSERT INTO fulfillment_assignment(id, store_id, product_id, warehouseBusinessUnitCode, createdAt)
VALUES (3, 1, 2, 'MWH.012', '2024-07-02');
INSERT INTO fulfillment_assignment(id, store_id, product_id, warehouseBusinessUnitCode, createdAt)
VALUES (4, 2, 3, 'MWH.023', '2023-07-02');
ALTER SEQUENCE fulfillment_assignment_seq RESTART WITH 5;
