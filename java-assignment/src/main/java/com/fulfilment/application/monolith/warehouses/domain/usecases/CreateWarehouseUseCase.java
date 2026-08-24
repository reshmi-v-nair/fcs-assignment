package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.BusinessUnitCodeAlreadyExistsException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.InvalidLocationException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.MaxWarehousesExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseCapacityExceededException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(CreateWarehouseUseCase.class.getName());

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new BusinessUnitCodeAlreadyExistsException(
          "A warehouse with business unit code "
              + warehouse.businessUnitCode
              + " already exists");
    }

    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new InvalidLocationException("Location " + warehouse.location + " does not exist");
    }

    long activeWarehousesAtLocation =
        warehouseStore.getAll().stream()
            .filter(w -> w.location.equals(location.identification) && w.archivedAt == null)
            .count();
    if (activeWarehousesAtLocation >= location.maxNumberOfWarehouses) {
      throw new MaxWarehousesExceededException(
          "Location "
              + location.identification
              + " has already reached its maximum number of warehouses ("
              + location.maxNumberOfWarehouses
              + ")");
    }

    if (warehouse.capacity > location.maxCapacity) {
      throw new WarehouseCapacityExceededException(
          "Warehouse capacity "
              + warehouse.capacity
              + " exceeds the maximum capacity ("
              + location.maxCapacity
              + ") allowed at location "
              + location.identification);
    }

    if (warehouse.stock > warehouse.capacity) {
      throw new WarehouseCapacityExceededException(
          "Warehouse capacity " + warehouse.capacity + " cannot handle the informed stock ("
              + warehouse.stock
              + ")");
    }

    warehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(warehouse);
    LOGGER.infof("Created warehouse %s at location %s", warehouse.businessUnitCode, warehouse.location);
  }
}