package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.InvalidLocationException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.MaxWarehousesExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseAlreadyArchivedException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseCapacityExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseStockMismatchException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(ReplaceWarehouseUseCase.class.getName());

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  @Transactional
  public void replace(Warehouse newWarehouse) {
    Warehouse existing = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (existing == null) {
      throw new WarehouseNotFoundException(
          "No warehouse found with business unit code " + newWarehouse.businessUnitCode);
    }
    if (existing.archivedAt != null) {
      throw new WarehouseAlreadyArchivedException(
          "Warehouse " + newWarehouse.businessUnitCode + " is already archived");
    }

    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw new InvalidLocationException("Location " + newWarehouse.location + " does not exist");
    }

    long activeWarehousesAtLocationExcludingCurrent =
        warehouseStore.getAll().stream()
            .filter(
                w ->
                    w.location.equals(location.identification)
                        && w.archivedAt == null
                        && !w.businessUnitCode.equals(existing.businessUnitCode))
            .count();
    if (activeWarehousesAtLocationExcludingCurrent >= location.maxNumberOfWarehouses) {
      throw new MaxWarehousesExceededException(
          "Location "
              + location.identification
              + " has already reached its maximum number of warehouses ("
              + location.maxNumberOfWarehouses
              + ")");
    }

    if (newWarehouse.capacity > location.maxCapacity) {
      throw new WarehouseCapacityExceededException(
          "Warehouse capacity "
              + newWarehouse.capacity
              + " exceeds the maximum capacity ("
              + location.maxCapacity
              + ") allowed at location "
              + location.identification);
    }

    if (newWarehouse.capacity < existing.stock) {
      throw new WarehouseCapacityExceededException(
          "New warehouse capacity ("
              + newWarehouse.capacity
              + ") cannot accommodate the stock of the replaced warehouse ("
              + existing.stock
              + ")");
    }

    if (!Objects.equals(newWarehouse.stock, existing.stock)) {
      throw new WarehouseStockMismatchException(
          "New warehouse stock ("
              + newWarehouse.stock
              + ") must match the stock of the replaced warehouse ("
              + existing.stock
              + ")");
    }

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
