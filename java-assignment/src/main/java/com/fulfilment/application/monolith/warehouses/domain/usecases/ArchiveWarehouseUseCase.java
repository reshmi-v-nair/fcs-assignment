package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.validation.WarehouseValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(ArchiveWarehouseUseCase.class.getName());

  private final WarehouseStore warehouseStore;
  private final WarehouseValidator warehouseValidator;

  public ArchiveWarehouseUseCase(
      WarehouseStore warehouseStore, WarehouseValidator warehouseValidator) {
    this.warehouseStore = warehouseStore;
    this.warehouseValidator = warehouseValidator;
  }

  @Override
  @Transactional
  public void archive(Warehouse warehouse) {
    warehouseValidator.validateNotAlreadyArchived(warehouse);

    warehouse.archivedAt = LocalDateTime.now();
    warehouseStore.update(warehouse);
    LOGGER.infof("Archived warehouse %s", warehouse.businessUnitCode);
  }
}
