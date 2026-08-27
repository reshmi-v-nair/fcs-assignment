package com.fulfilment.application.monolith.fulfillment.validation;

import com.fulfilment.application.monolith.fulfillment.adapters.database.FulfillmentAssignmentRepository;
import com.fulfilment.application.monolith.fulfillment.exceptions.MaxProductsPerWarehouseExceededException;
import com.fulfilment.application.monolith.fulfillment.exceptions.MaxWarehousesPerProductPerStoreExceededException;
import com.fulfilment.application.monolith.fulfillment.exceptions.MaxWarehousesPerStoreExceededException;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cardinality rules for the bonus Warehouse-Store-Product fulfillment feature, kept separate from
 * {@link com.fulfilment.application.monolith.fulfillment.FulfillmentAssignmentService}'s
 * orchestration (existence lookups, idempotency check, persisting) so each rule is independently
 * testable.
 */
@ApplicationScoped
public class FulfillmentAssignmentValidator {

  private static final int MAX_WAREHOUSES_PER_PRODUCT_PER_STORE = 2;
  private static final int MAX_WAREHOUSES_PER_STORE = 3;
  private static final int MAX_PRODUCTS_PER_WAREHOUSE = 5;

  private final FulfillmentAssignmentRepository fulfillmentAssignmentRepository;

  public FulfillmentAssignmentValidator(
      FulfillmentAssignmentRepository fulfillmentAssignmentRepository) {
    this.fulfillmentAssignmentRepository = fulfillmentAssignmentRepository;
  }

  public void validate(Store store, Product product, String warehouseBusinessUnitCode) {
    validateMaxWarehousesPerProductPerStore(store, product);
    validateMaxWarehousesPerStore(store, warehouseBusinessUnitCode);
    validateMaxProductsPerWarehouse(product, warehouseBusinessUnitCode);
  }

  private void validateMaxWarehousesPerProductPerStore(Store store, Product product) {
    Set<String> warehousesForProductAtStore =
        fulfillmentAssignmentRepository.listByStoreAndProduct(store, product).stream()
            .map(a -> a.warehouseBusinessUnitCode)
            .collect(Collectors.toSet());
    if (warehousesForProductAtStore.size() >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new MaxWarehousesPerProductPerStoreExceededException(
          "Product "
              + product.id
              + " at store "
              + store.id
              + " is already fulfilled by the maximum of "
              + MAX_WAREHOUSES_PER_PRODUCT_PER_STORE
              + " warehouses");
    }
  }

  private void validateMaxWarehousesPerStore(Store store, String warehouseBusinessUnitCode) {
    Set<String> warehousesForStore =
        fulfillmentAssignmentRepository.listByStore(store).stream()
            .map(a -> a.warehouseBusinessUnitCode)
            .collect(Collectors.toSet());
    if (!warehousesForStore.contains(warehouseBusinessUnitCode)
        && warehousesForStore.size() >= MAX_WAREHOUSES_PER_STORE) {
      throw new MaxWarehousesPerStoreExceededException(
          "Store "
              + store.id
              + " is already fulfilled by the maximum of "
              + MAX_WAREHOUSES_PER_STORE
              + " warehouses");
    }
  }

  private void validateMaxProductsPerWarehouse(Product product, String warehouseBusinessUnitCode) {
    Set<Long> productsForWarehouse =
        fulfillmentAssignmentRepository
            .listByWarehouseBusinessUnitCode(warehouseBusinessUnitCode)
            .stream()
            .map(a -> a.product.id)
            .collect(Collectors.toSet());
    if (!productsForWarehouse.contains(product.id)
        && productsForWarehouse.size() >= MAX_PRODUCTS_PER_WAREHOUSE) {
      throw new MaxProductsPerWarehouseExceededException(
          "Warehouse "
              + warehouseBusinessUnitCode
              + " already stocks the maximum of "
              + MAX_PRODUCTS_PER_WAREHOUSE
              + " distinct products");
    }
  }
}
