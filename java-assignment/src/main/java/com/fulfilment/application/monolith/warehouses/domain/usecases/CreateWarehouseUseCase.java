package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.validation.WarehouseValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(CreateWarehouseUseCase.class.getName());

  private final WarehouseStore warehouseStore;
  private final WarehouseValidator warehouseValidator;

  public CreateWarehouseUseCase(
      WarehouseStore warehouseStore, WarehouseValidator warehouseValidator) {
    this.warehouseStore = warehouseStore;
    this.warehouseValidator = warehouseValidator;
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    warehouseValidator.validateBusinessUnitCodeIsAvailable(warehouse.businessUnitCode);

    Location location = warehouseValidator.resolveLocationOrThrow(warehouse.location);
    warehouseValidator.validateWarehouseCountWithinLimit(location, null);
    warehouseValidator.validateCapacityWithinLocationMax(warehouse.capacity, location);
    warehouseValidator.validateStockWithinCapacity(warehouse.stock, warehouse.capacity);

    warehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(warehouse);
    LOGGER.infof(
        "Created warehouse %s at location %s", warehouse.businessUnitCode, warehouse.location);
  }
}
