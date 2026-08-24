package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class FulfillmentAssignmentRepository implements PanacheRepository<FulfillmentAssignment> {

  public List<FulfillmentAssignment> listByStore(Store store) {
    return list("store", store);
  }

  public List<FulfillmentAssignment> listByStoreAndProduct(Store store, Product product) {
    return list("store = ?1 and product = ?2", store, product);
  }

  public List<FulfillmentAssignment> listByWarehouseBusinessUnitCode(String buCode) {
    return list("warehouseBusinessUnitCode", buCode);
  }

  public boolean exists(Store store, Product product, String buCode) {
    return count(
            "store = ?1 and product = ?2 and warehouseBusinessUnitCode = ?3",
            store,
            product,
            buCode)
        > 0;
  }
}
