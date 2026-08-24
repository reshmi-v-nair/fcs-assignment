package com.fulfilment.application.monolith.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.fulfillment.exceptions.MaxProductsPerWarehouseExceededException;
import com.fulfilment.application.monolith.fulfillment.exceptions.MaxWarehousesPerProductPerStoreExceededException;
import com.fulfilment.application.monolith.fulfillment.exceptions.MaxWarehousesPerStoreExceededException;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

/**
 * Direct service-layer tests, complementing {@link FulfillmentResourceTest}'s REST-level coverage.
 * Note this can't be a pure Mockito unit test: {@link
 * com.fulfilment.application.monolith.stores.Store} is a Panache active-record entity, so {@code
 * Store.findById(...)} requires a live persistence context - one of the concrete trade-offs
 * discussed in java-assignment/QUESTIONS.md's question about the codebase's mixed data-access
 * styles.
 */
@QuarkusTest
public class FulfillmentAssignmentServiceTest {

  @Inject FulfillmentAssignmentService fulfillmentAssignmentService;

  @Inject ProductRepository productRepository;

  @Inject CreateWarehouseOperation createWarehouseOperation;

  @Transactional
  Store createStore(String name) {
    Store store = new Store(name);
    store.persist();
    return store;
  }

  @Transactional
  Product createProduct(String name) {
    Product product = new Product(name);
    productRepository.persist(product);
    return product;
  }

  void createWarehouse(String buCode, String location, int capacity, int stock) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = buCode;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    createWarehouseOperation.create(warehouse);
  }

  @Test
  void rejectsFourthDistinctWarehouseForStore() {
    // AMSTERDAM-001 allows up to 5 warehouses and has plenty of spare capacity; only the seeded
    // MWH.012 uses it so far, so this test has room to create its own warehouses there.
    createWarehouse("MWH.960", "AMSTERDAM-001", 10, 5);
    createWarehouse("MWH.961", "AMSTERDAM-001", 10, 5);
    createWarehouse("MWH.962", "AMSTERDAM-001", 10, 5);
    createWarehouse("MWH.963", "AMSTERDAM-001", 10, 5);

    Store store = createStore("SVC-TEST-STORE-MAX-WAREHOUSES");
    Product p1 = createProduct("SVC-TEST-PRODUCT-MW-1");
    Product p2 = createProduct("SVC-TEST-PRODUCT-MW-2");
    Product p3 = createProduct("SVC-TEST-PRODUCT-MW-3");
    Product p4 = createProduct("SVC-TEST-PRODUCT-MW-4");

    // Each product is fulfilled by a distinct single warehouse, so the store reaches 3 distinct
    // warehouses without ever hitting the (separate) max-2-warehouses-per-product-per-store rule.
    fulfillmentAssignmentService.assign(store.id, p1.id, "MWH.960");
    fulfillmentAssignmentService.assign(store.id, p2.id, "MWH.961");
    fulfillmentAssignmentService.assign(store.id, p3.id, "MWH.962");

    assertThrows(
        MaxWarehousesPerStoreExceededException.class,
        () -> fulfillmentAssignmentService.assign(store.id, p4.id, "MWH.963"));
  }

  @Test
  void rejectsSixthProductForWarehouse() {
    // Uses ZWOLLE-002 rather than AMSTERDAM-001 - the latter is exactly saturated to its max of 5
    // warehouses by rejectsFourthDistinctWarehouseForStore's 4 warehouses plus the seeded MWH.012,
    // so a 6th warehouse there would fail warehouse *creation* itself, independently of the
    // product-cap rule this test actually exercises.
    createWarehouse("MWH.964", "ZWOLLE-002", 10, 5);

    Store store = createStore("SVC-TEST-STORE-MAX-PRODUCTS");
    // All 6 products go through the very same warehouse, so the store never approaches the
    // (separate) max-3-warehouses-per-store rule - only the warehouse's product cap is exercised.
    for (int i = 1; i <= 5; i++) {
      Product product = createProduct("SVC-TEST-PRODUCT-MP-" + i);
      fulfillmentAssignmentService.assign(store.id, product.id, "MWH.964");
    }

    Product sixthProduct = createProduct("SVC-TEST-PRODUCT-MP-6");
    assertThrows(
        MaxProductsPerWarehouseExceededException.class,
        () -> fulfillmentAssignmentService.assign(store.id, sixthProduct.id, "MWH.964"));
  }

  @Test
  void assignsSuccessfully() {
    // store 2 (KALLAX) already fulfills product 3 via MWH.023; product 2 via the same warehouse
    // is a fresh combination.
    FulfillmentAssignment assignment = fulfillmentAssignmentService.assign(2L, 2L, "MWH.023");

    assertEquals("MWH.023", assignment.warehouseBusinessUnitCode);
  }

  @Test
  void rejectsUnknownStore() {
    assertThrows(
        WebApplicationException.class,
        () -> fulfillmentAssignmentService.assign(999999L, 1L, "MWH.001"));
  }

  @Test
  void rejectsUnknownProduct() {
    assertThrows(
        WebApplicationException.class,
        () -> fulfillmentAssignmentService.assign(1L, 999999L, "MWH.001"));
  }

  @Test
  void rejectsUnknownWarehouse() {
    assertThrows(
        WarehouseNotFoundException.class,
        () -> fulfillmentAssignmentService.assign(1L, 1L, "MWH.999999"));
  }

  @Test
  void rejectsThirdWarehouseForSameProductAtSameStore() {
    // product 1 at store 1 is already fulfilled by MWH.001 and MWH.012 (the max of 2).
    assertThrows(
        MaxWarehousesPerProductPerStoreExceededException.class,
        () -> fulfillmentAssignmentService.assign(1L, 1L, "MWH.023"));
  }
}
