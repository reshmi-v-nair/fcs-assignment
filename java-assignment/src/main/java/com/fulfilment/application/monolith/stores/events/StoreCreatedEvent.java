package com.fulfilment.application.monolith.stores.events;

import com.fulfilment.application.monolith.stores.Store;

/** Fired by {@code StoreResource} when a new {@link Store} is persisted. */
public class StoreCreatedEvent {

  public final Store store;

  public StoreCreatedEvent(Store store) {
    this.store = store;
  }
}
