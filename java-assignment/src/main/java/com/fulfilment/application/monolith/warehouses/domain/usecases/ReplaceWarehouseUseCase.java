package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.validation.WarehouseValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(ReplaceWarehouseUseCase.class.getName());

  private final WarehouseStore warehouseStore;
  private final WarehouseValidator warehouseValidator;

  public ReplaceWarehouseUseCase(
      WarehouseStore warehouseStore, WarehouseValidator warehouseValidator) {
    this.warehouseStore = warehouseStore;
    this.warehouseValidator = warehouseValidator;
  }

  @Override
  @Transactional
  public void replace(Warehouse newWarehouse) {
    Warehouse existing = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (existing == null) {
      throw new WarehouseNotFoundException(
          "No warehouse found with business unit code " + newWarehouse.businessUnitCode);
    }
    warehouseValidator.validateNotAlreadyArchived(existing);

    Location location = warehouseValidator.resolveLocationOrThrow(newWarehouse.location);
    warehouseValidator.validateWarehouseCountWithinLimit(location, existing.businessUnitCode);
    warehouseValidator.validateCapacityWithinLocationMax(newWarehouse.capacity, location);
    warehouseValidator.validateCapacityCanAccommodateStock(newWarehouse.capacity, existing.stock);
    warehouseValidator.validateStockMatches(newWarehouse.stock, existing.stock);

    existing.archivedAt = LocalDateTime.now();
    warehouseStore.update(existing);

    newWarehouse.createdAt = LocalDateTime.now();
    newWarehouse.archivedAt = null;
    warehouseStore.create(newWarehouse);

    LOGGER.infof(
        "Replaced warehouse %s: archived old record, created new active record",
        newWarehouse.businessUnitCode);
  }
}
