package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.InvalidLocationException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.MaxWarehousesExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseAlreadyArchivedException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseCapacityExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseStockMismatchException;
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
public class ReplaceWarehouseUseCaseTest {

  @Mock private WarehouseStore warehouseStore;
  @Mock private LocationResolver locationResolver;

  private ReplaceWarehouseUseCase useCase;

  private static final Location ZWOLLE_001 = new Location("ZWOLLE-001", 1, 40);

  @BeforeEach
  void setUp() {
    useCase = new ReplaceWarehouseUseCase(warehouseStore, locationResolver);
  }

  private Warehouse warehouse(String buCode, String location, int capacity, int stock) {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = buCode;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }

  @Test
  void replacesWarehouseWhenAllValidationsPass() {
    Warehouse existing = warehouse("MWH.001", "ZWOLLE-001", 40, 10);
    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-001", 40, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(ZWOLLE_001);
    when(warehouseStore.getAll()).thenReturn(List.of(existing));

    useCase.replace(replacement);

    assertNotNull(existing.archivedAt);
    assertNull(replacement.archivedAt);
    assertNotNull(replacement.createdAt);
    verify(warehouseStore).update(existing);
    verify(warehouseStore).create(replacement);
  }

  @Test
  void rejectsWhenBusinessUnitCodeNotFound() {
    Warehouse replacement = warehouse("MWH.999", "ZWOLLE-001", 40, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.999")).thenReturn(null);

    assertThrows(WarehouseNotFoundException.class, () -> useCase.replace(replacement));
  }

  @Test
  void rejectsWhenExistingWarehouseAlreadyArchived() {
    Warehouse existing = warehouse("MWH.001", "ZWOLLE-001", 40, 10);
    existing.archivedAt = java.time.LocalDateTime.now();
    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-001", 40, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

    assertThrows(WarehouseAlreadyArchivedException.class, () -> useCase.replace(replacement));
  }

  @Test
  void rejectsInvalidLocation() {
    Warehouse existing = warehouse("MWH.001", "ZWOLLE-001", 40, 10);
    Warehouse replacement = warehouse("MWH.001", "UNKNOWN-999", 40, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
    when(locationResolver.resolveByIdentifier("UNKNOWN-999")).thenReturn(null);

    assertThrows(InvalidLocationException.class, () -> useCase.replace(replacement));
  }

  @Test
  void rejectsWhenMaxWarehousesReachedByOtherWarehouses() {
    Warehouse existing = warehouse("MWH.001", "ZWOLLE-001", 40, 10);
    Warehouse other = warehouse("MWH.002", "ZWOLLE-001", 40, 5);
    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-001", 40, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
        .thenReturn(new Location("ZWOLLE-001", 1, 40));
    when(warehouseStore.getAll()).thenReturn(List.of(existing, other));

    assertThrows(MaxWarehousesExceededException.class, () -> useCase.replace(replacement));
  }

  @Test
  void rejectsCapacityAboveLocationMax() {
    Warehouse existing = warehouse("MWH.001", "ZWOLLE-001", 40, 10);
    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-001", 41, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(ZWOLLE_001);
    when(warehouseStore.getAll()).thenReturn(List.of(existing));

    assertThrows(WarehouseCapacityExceededException.class, () -> useCase.replace(replacement));
  }

  @Test
  void rejectsCapacityBelowExistingStock() {
    Warehouse existing = warehouse("MWH.001", "ZWOLLE-001", 40, 35);
    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-001", 30, 35);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(ZWOLLE_001);
    when(warehouseStore.getAll()).thenReturn(List.of(existing));

    assertThrows(WarehouseCapacityExceededException.class, () -> useCase.replace(replacement));
  }

  @Test
  void rejectsStockMismatch() {
    Warehouse existing = warehouse("MWH.001", "ZWOLLE-001", 40, 10);
    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-001", 40, 20);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(ZWOLLE_001);
    when(warehouseStore.getAll()).thenReturn(List.of(existing));

    assertThrows(WarehouseStockMismatchException.class, () -> useCase.replace(replacement));
  }
}
