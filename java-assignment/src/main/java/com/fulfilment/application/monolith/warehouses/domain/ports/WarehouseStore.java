package com.fulfilment.application.monolith.warehouses.domain.ports;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import java.util.List;

public interface WarehouseStore {

  List<Warehouse> getAll();

  void create(Warehouse warehouse);

  void update(Warehouse warehouse);

  void remove(Warehouse warehouse);

  Warehouse findByBusinessUnitCode(String buCode);

  // Named getById (not findById) to avoid colliding with PanacheRepositoryBase's own
  // findById(Long) method, which WarehouseRepository also implements with a different return type.
  Warehouse getById(Long id);
}
