package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * Associates a Warehouse (referenced by its natural businessUnitCode, not a JPA relation to
 * DbWarehouse - see java-assignment/QUESTIONS.md / TESTING.md for the reasoning) as a fulfillment
 * unit for a given Product at a given Store.
 */
@Entity
@Table(
    name = "fulfillment_assignment",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_fulfillment_store_product_warehouse",
            columnNames = {"store_id", "product_id", "warehouseBusinessUnitCode"}))
@Cacheable
public class FulfillmentAssignment extends PanacheEntity {

  @ManyToOne(optional = false)
  public Store store;

  @ManyToOne(optional = false)
  public Product product;

  @Column(nullable = false)
  public String warehouseBusinessUnitCode;

  public LocalDateTime createdAt;

  public FulfillmentAssignment() {}

  public FulfillmentAssignment(Store store, Product product, String warehouseBusinessUnitCode) {
    this.store = store;
    this.product = product;
    this.warehouseBusinessUnitCode = warehouseBusinessUnitCode;
    this.createdAt = LocalDateTime.now();
  }
}
