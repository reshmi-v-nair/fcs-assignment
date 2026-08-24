package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.fulfillment.exceptions.MaxProductsPerWarehouseExceededException;
import com.fulfilment.application.monolith.fulfillment.exceptions.MaxWarehousesPerProductPerStoreExceededException;
import com.fulfilment.application.monolith.fulfillment.exceptions.MaxWarehousesPerStoreExceededException;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import java.util.Set;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

@ApplicationScoped
public class FulfillmentAssignmentService {

  private static final int MAX_WAREHOUSES_PER_PRODUCT_PER_STORE = 2;
  private static final int MAX_WAREHOUSES_PER_STORE = 3;
  private static final int MAX_PRODUCTS_PER_WAREHOUSE = 5;

  private static final Logger LOGGER =
      Logger.getLogger(FulfillmentAssignmentService.class.getName());

  private final FulfillmentAssignmentRepository fulfillmentAssignmentRepository;
  private final ProductRepository productRepository;
  private final WarehouseStore warehouseStore;

  public FulfillmentAssignmentService(
      FulfillmentAssignmentRepository fulfillmentAssignmentRepository,
      ProductRepository productRepository,
      WarehouseStore warehouseStore) {
    this.fulfillmentAssignmentRepository = fulfillmentAssignmentRepository;
    this.productRepository = productRepository;
    this.warehouseStore = warehouseStore;
  }

  @Transactional
  public FulfillmentAssignment assign(
      Long storeId, Long productId, String warehouseBusinessUnitCode) {
    Store store = Store.findById(storeId);
    if (store == null) {
      throw new WebApplicationException("Store with id of " + storeId + " does not exist.", 404);
    }

    Product product = productRepository.findById(productId);
    if (product == null) {
      throw new WebApplicationException(
          "Product with id of " + productId + " does not exist.", 404);
    }

    if (warehouseStore.findByBusinessUnitCode(warehouseBusinessUnitCode) == null) {
      throw new WarehouseNotFoundException(
          "No active warehouse found with business unit code " + warehouseBusinessUnitCode);
    }

    if (fulfillmentAssignmentRepository.exists(store, product, warehouseBusinessUnitCode)) {
      LOGGER.infof(
          "Fulfillment assignment already exists for store=%d, product=%d, warehouse=%s; no-op",
          storeId, productId, warehouseBusinessUnitCode);
      return fulfillmentAssignmentRepository.listByStoreAndProduct(store, product).stream()
          .filter(a -> a.warehouseBusinessUnitCode.equals(warehouseBusinessUnitCode))
          .findFirst()
          .orElseThrow();
    }

    Set<String> warehousesForProductAtStore =
        fulfillmentAssignmentRepository.listByStoreAndProduct(store, product).stream()
            .map(a -> a.warehouseBusinessUnitCode)
            .collect(Collectors.toSet());
    if (warehousesForProductAtStore.size() >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new MaxWarehousesPerProductPerStoreExceededException(
          "Product "
              + productId
              + " at store "
              + storeId
              + " is already fulfilled by the maximum of "
              + MAX_WAREHOUSES_PER_PRODUCT_PER_STORE
              + " warehouses");
    }

    Set<String> warehousesForStore =
        fulfillmentAssignmentRepository.listByStore(store).stream()
            .map(a -> a.warehouseBusinessUnitCode)
            .collect(Collectors.toSet());
    if (!warehousesForStore.contains(warehouseBusinessUnitCode)
        && warehousesForStore.size() >= MAX_WAREHOUSES_PER_STORE) {
      throw new MaxWarehousesPerStoreExceededException(
          "Store "
              + storeId
              + " is already fulfilled by the maximum of "
              + MAX_WAREHOUSES_PER_STORE
              + " warehouses");
    }

    Set<Long> productsForWarehouse =
        fulfillmentAssignmentRepository
            .listByWarehouseBusinessUnitCode(warehouseBusinessUnitCode)
            .stream()
            .map(a -> a.product.id)
            .collect(Collectors.toSet());
    if (!productsForWarehouse.contains(productId)
        && productsForWarehouse.size() >= MAX_PRODUCTS_PER_WAREHOUSE) {
      throw new MaxProductsPerWarehouseExceededException(
          "Warehouse "
              + warehouseBusinessUnitCode
              + " already stocks the maximum of "
              + MAX_PRODUCTS_PER_WAREHOUSE
              + " distinct products");
    }

    var assignment = new FulfillmentAssignment(store, product, warehouseBusinessUnitCode);
    fulfillmentAssignmentRepository.persist(assignment);
    LOGGER.infof(
        "Assigned warehouse %s as fulfillment unit for product %d at store %d",
        warehouseBusinessUnitCode, productId, storeId);
    return assignment;
  }
}
