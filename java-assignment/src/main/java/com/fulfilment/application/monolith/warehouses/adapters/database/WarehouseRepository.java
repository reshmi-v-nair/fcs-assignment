package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  private static final Logger LOGGER = Logger.getLogger(WarehouseRepository.class.getName());

  @Override
  public List<Warehouse> getAll() {
    return this.listAll().stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    DbWarehouse dbWarehouse = DbWarehouse.fromWarehouse(warehouse);
    persist(dbWarehouse);
    warehouse.id = dbWarehouse.id;
  }

  @Override
  public void update(Warehouse warehouse) {
    // Replacing a warehouse preserves history by keeping archived rows around under the same
    // businessUnitCode, so lookups must be scoped to the currently-active row for that code
    // (archivedAt is null) to avoid ambiguously matching a past archived record instead.
    DbWarehouse dbWarehouse = findActiveByBusinessUnitCode(warehouse.businessUnitCode);
    if (dbWarehouse == null) {
      LOGGER.errorf(
          "No active warehouse found with businessUnitCode %s during update; this should have"
              + " been caught by the calling use case's existence check.",
          warehouse.businessUnitCode);
      throw new IllegalStateException(
          "No active warehouse found with businessUnitCode " + warehouse.businessUnitCode);
    }
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.archivedAt = warehouse.archivedAt;
    // dbWarehouse is a managed entity within the enclosing @Transactional context, so Hibernate's
    // dirty-checking flushes these changes on commit without an explicit persist() call.
  }

  @Override
  public void remove(Warehouse warehouse) {
    DbWarehouse dbWarehouse = findActiveByBusinessUnitCode(warehouse.businessUnitCode);
    if (dbWarehouse != null) {
      delete(dbWarehouse);
    }
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse dbWarehouse = findActiveByBusinessUnitCode(buCode);
    return dbWarehouse == null ? null : dbWarehouse.toWarehouse();
  }

  private DbWarehouse findActiveByBusinessUnitCode(String buCode) {
    return find("businessUnitCode = ?1 and archivedAt is null", buCode).firstResult();
  }

  @Override
  public Warehouse getById(Long id) {
    DbWarehouse dbWarehouse = findById(id);
    return dbWarehouse == null ? null : dbWarehouse.toWarehouse();
  }
}
