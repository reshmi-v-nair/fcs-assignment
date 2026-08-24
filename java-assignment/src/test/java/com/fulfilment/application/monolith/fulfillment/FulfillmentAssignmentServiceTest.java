package com.fulfilment.application.monolith.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.fulfillment.exceptions.MaxWarehousesPerProductPerStoreExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

/**
 * Direct service-layer tests, complementing {@link FulfillmentResourceTest}'s REST-level
 * coverage. Note this can't be a pure Mockito unit test: {@link
 * com.fulfilment.application.monolith.stores.Store} is a Panache active-record entity, so {@code
 * Store.findById(...)} requires a live persistence context - one of the concrete trade-offs
 * discussed in java-assignment/QUESTIONS.md's question about the codebase's mixed data-access
 * styles.
 */
@QuarkusTest
public class FulfillmentAssignmentServiceTest {

  @Inject FulfillmentAssignmentService fulfillmentAssignmentService;

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