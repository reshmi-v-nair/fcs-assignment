package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.jboss.logging.Logger;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  private static final Logger LOGGER = Logger.getLogger(WarehouseResourceImpl.class.getName());

  private final WarehouseStore warehouseStore;
  private final CreateWarehouseOperation createWarehouseOperation;
  private final ReplaceWarehouseOperation replaceWarehouseOperation;
  private final ArchiveWarehouseOperation archiveWarehouseOperation;

  public WarehouseResourceImpl(
      WarehouseStore warehouseStore,
      CreateWarehouseOperation createWarehouseOperation,
      ReplaceWarehouseOperation replaceWarehouseOperation,
      ArchiveWarehouseOperation archiveWarehouseOperation) {
    this.warehouseStore = warehouseStore;
    this.createWarehouseOperation = createWarehouseOperation;
    this.replaceWarehouseOperation = replaceWarehouseOperation;
    this.archiveWarehouseOperation = archiveWarehouseOperation;
  }

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    return warehouseStore.getAll().stream()
        .filter(warehouse -> warehouse.archivedAt == null)
        .map(this::toWarehouseResponse)
        .toList();
  }

  @Override
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    var warehouse = toWarehouseModel(data);
    createWarehouseOperation.create(warehouse);
    return toWarehouseResponse(warehouse);
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    var warehouse = warehouseStore.getById(parseId(id));
    if (warehouse == null) {
      throw new WarehouseNotFoundException("No warehouse found with id " + id);
    }
    return toWarehouseResponse(warehouse);
  }

  @Override
  public void archiveAWarehouseUnitByID(String id) {
    var warehouse = warehouseStore.getById(parseId(id));
    if (warehouse == null) {
      throw new WarehouseNotFoundException("No warehouse found with id " + id);
    }
    archiveWarehouseOperation.archive(warehouse);
  }

  @Override
  public Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull Warehouse data) {
    var warehouse = toWarehouseModel(data);
    if (data.getBusinessUnitCode() != null && !businessUnitCode.equals(data.getBusinessUnitCode())) {
      LOGGER.warnf(
          "Business unit code in request body (%s) differs from path (%s); path value wins",
          data.getBusinessUnitCode(), businessUnitCode);
    }
    warehouse.businessUnitCode = businessUnitCode;
    replaceWarehouseOperation.replace(warehouse);
    return toWarehouseResponse(warehouse);
  }

  private Long parseId(String id) {
    try {
      return Long.valueOf(id);
    } catch (NumberFormatException e) {
      throw new WebApplicationException("Invalid warehouse id: " + id, 400);
    }
  }

  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    response.setId(warehouse.id == null ? null : String.valueOf(warehouse.id));
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);

    return response;
  }

  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse toWarehouseModel(
      Warehouse data) {
    var warehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    warehouse.businessUnitCode = data.getBusinessUnitCode();
    warehouse.location = data.getLocation();
    warehouse.capacity = data.getCapacity();
    warehouse.stock = data.getStock();
    return warehouse;
  }
}