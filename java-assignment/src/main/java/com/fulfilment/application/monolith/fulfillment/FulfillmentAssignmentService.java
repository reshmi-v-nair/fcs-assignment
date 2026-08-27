package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.fulfillment.adapters.database.FulfillmentAssignmentRepository;
import com.fulfilment.application.monolith.fulfillment.validation.FulfillmentAssignmentValidator;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import org.jboss.logging.Logger;

@ApplicationScoped
public class FulfillmentAssignmentService {

  private static final Logger LOGGER =
      Logger.getLogger(FulfillmentAssignmentService.class.getName());

  private final FulfillmentAssignmentRepository fulfillmentAssignmentRepository;
  private final ProductRepository productRepository;
  private final WarehouseStore warehouseStore;
  private final FulfillmentAssignmentValidator fulfillmentAssignmentValidator;

  public FulfillmentAssignmentService(
      FulfillmentAssignmentRepository fulfillmentAssignmentRepository,
      ProductRepository productRepository,
      WarehouseStore warehouseStore,
      FulfillmentAssignmentValidator fulfillmentAssignmentValidator) {
    this.fulfillmentAssignmentRepository = fulfillmentAssignmentRepository;
    this.productRepository = productRepository;
    this.warehouseStore = warehouseStore;
    this.fulfillmentAssignmentValidator = fulfillmentAssignmentValidator;
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

    fulfillmentAssignmentValidator.validate(store, product, warehouseBusinessUnitCode);

    var assignment = new FulfillmentAssignment(store, product, warehouseBusinessUnitCode);
    fulfillmentAssignmentRepository.persist(assignment);
    LOGGER.infof(
        "Assigned warehouse %s as fulfillment unit for product %d at store %d",
        warehouseBusinessUnitCode, productId, storeId);
    return assignment;
  }
}
