package com.fulfilment.application.monolith.warehouses.domain.validation;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.BusinessUnitCodeAlreadyExistsException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.InvalidLocationException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.MaxWarehousesExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseAlreadyArchivedException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseCapacityExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseStockMismatchException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Objects;

/**
 * Business-rule validations shared by {@link
 * com.fulfilment.application.monolith.warehouses.domain.usecases.CreateWarehouseUseCase}, {@link
 * com.fulfilment.application.monolith.warehouses.domain.usecases.ReplaceWarehouseUseCase} and
 * {@link com.fulfilment.application.monolith.warehouses.domain.usecases.ArchiveWarehouseUseCase},
 * kept separate from those use cases' orchestration (fetching, persisting, logging) so each rule is
 * independently testable and reusable.
 */
@ApplicationScoped
public class WarehouseValidator {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public WarehouseValidator(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  public void validateBusinessUnitCodeIsAvailable(String businessUnitCode) {
    if (warehouseStore.findByBusinessUnitCode(businessUnitCode) != null) {
      throw new BusinessUnitCodeAlreadyExistsException(
          "A warehouse with business unit code " + businessUnitCode + " already exists");
    }
  }

  public Location resolveLocationOrThrow(String locationIdentifier) {
    Location location = locationResolver.resolveByIdentifier(locationIdentifier);
    if (location == null) {
      throw new InvalidLocationException("Location " + locationIdentifier + " does not exist");
    }
    return location;
  }

  /**
   * @param excludingBusinessUnitCode a warehouse to leave out of the active count (the one being
   *     replaced, when called from {@code ReplaceWarehouseUseCase}), or {@code null} when creating
   *     a brand-new warehouse.
   */
  public void validateWarehouseCountWithinLimit(
      Location location, String excludingBusinessUnitCode) {
    long activeWarehousesAtLocation =
        warehouseStore.getAll().stream()
            .filter(
                w ->
                    w.location.equals(location.identification)
                        && w.archivedAt == null
                        && !w.businessUnitCode.equals(excludingBusinessUnitCode))
            .count();
    if (activeWarehousesAtLocation >= location.maxNumberOfWarehouses) {
      throw new MaxWarehousesExceededException(
          "Location "
              + location.identification
              + " has already reached its maximum number of warehouses ("
              + location.maxNumberOfWarehouses
              + ")");
    }
  }

  public void validateCapacityWithinLocationMax(Integer capacity, Location location) {
    if (capacity > location.maxCapacity) {
      throw new WarehouseCapacityExceededException(
          "Warehouse capacity "
              + capacity
              + " exceeds the maximum capacity ("
              + location.maxCapacity
              + ") allowed at location "
              + location.identification);
    }
  }

  public void validateStockWithinCapacity(Integer stock, Integer capacity) {
    if (stock > capacity) {
      throw new WarehouseCapacityExceededException(
          "Warehouse capacity " + capacity + " cannot handle the informed stock (" + stock + ")");
    }
  }

  public void validateCapacityCanAccommodateStock(Integer capacity, Integer requiredStock) {
    if (capacity < requiredStock) {
      throw new WarehouseCapacityExceededException(
          "New warehouse capacity ("
              + capacity
              + ") cannot accommodate the stock of the replaced warehouse ("
              + requiredStock
              + ")");
    }
  }

  public void validateStockMatches(Integer newStock, Integer existingStock) {
    if (!Objects.equals(newStock, existingStock)) {
      throw new WarehouseStockMismatchException(
          "New warehouse stock ("
              + newStock
              + ") must match the stock of the replaced warehouse ("
              + existingStock
              + ")");
    }
  }

  public void validateNotAlreadyArchived(Warehouse warehouse) {
    if (warehouse.archivedAt != null) {
      throw new WarehouseAlreadyArchivedException(
          "Warehouse " + warehouse.businessUnitCode + " is already archived");
    }
  }
}
