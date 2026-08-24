package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.BusinessUnitCodeAlreadyExistsException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.InvalidLocationException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.MaxWarehousesExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseCapacityExceededException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateWarehouseUseCaseTest {

  @Mock private WarehouseStore warehouseStore;
  @Mock private LocationResolver locationResolver;

  private CreateWarehouseUseCase useCase;

  private static final Location ZWOLLE_001 = new Location("ZWOLLE-001", 1, 40);

  @BeforeEach
  void setUp() {
    useCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);
  }

  private Warehouse newWarehouse(String buCode, String location, int capacity, int stock) {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = buCode;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }

  @Test
  void createsWarehouseWhenAllValidationsPass() {
    Warehouse warehouse = newWarehouse("MWH.100", "ZWOLLE-001", 40, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(ZWOLLE_001);
    when(warehouseStore.getAll()).thenReturn(List.of());

    useCase.create(warehouse);

    assertNotNull(warehouse.createdAt);
    verify(warehouseStore).create(warehouse);
  }

  @Test
  void rejectsDuplicateBusinessUnitCode() {
    Warehouse warehouse = newWarehouse("MWH.001", "ZWOLLE-001", 40, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(new Warehouse());

    assertThrows(BusinessUnitCodeAlreadyExistsException.class, () -> useCase.create(warehouse));
  }

  @Test
  void rejectsInvalidLocation() {
    Warehouse warehouse = newWarehouse("MWH.100", "UNKNOWN-999", 40, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("UNKNOWN-999")).thenReturn(null);

    assertThrows(InvalidLocationException.class, () -> useCase.create(warehouse));
  }

  @Test
  void rejectsWhenMaxWarehousesAtLocationReached() {
    Warehouse warehouse = newWarehouse("MWH.100", "ZWOLLE-001", 40, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(ZWOLLE_001);
    Warehouse existingActive = newWarehouse("MWH.001", "ZWOLLE-001", 40, 5);
    when(warehouseStore.getAll()).thenReturn(List.of(existingActive));

    assertThrows(MaxWarehousesExceededException.class, () -> useCase.create(warehouse));
  }

  @Test
  void ignoresArchivedWarehousesWhenCountingMaxWarehouses() {
    Warehouse warehouse = newWarehouse("MWH.100", "ZWOLLE-001", 40, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(ZWOLLE_001);
    Warehouse archived = newWarehouse("MWH.001", "ZWOLLE-001", 40, 5);
    archived.archivedAt = java.time.LocalDateTime.now();
    when(warehouseStore.getAll()).thenReturn(List.of(archived));

    useCase.create(warehouse);

    verify(warehouseStore).create(warehouse);
  }

  @Test
  void rejectsCapacityAboveLocationMax() {
    Warehouse warehouse = newWarehouse("MWH.100", "ZWOLLE-001", 41, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(ZWOLLE_001);
    when(warehouseStore.getAll()).thenReturn(List.of());

    assertThrows(WarehouseCapacityExceededException.class, () -> useCase.create(warehouse));
  }

  @Test
  void rejectsStockAboveCapacity() {
    Warehouse warehouse = newWarehouse("MWH.100", "ZWOLLE-001", 40, 41);
    when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(ZWOLLE_001);
    when(warehouseStore.getAll()).thenReturn(List.of());

    assertThrows(WarehouseCapacityExceededException.class, () -> useCase.create(warehouse));
  }
}