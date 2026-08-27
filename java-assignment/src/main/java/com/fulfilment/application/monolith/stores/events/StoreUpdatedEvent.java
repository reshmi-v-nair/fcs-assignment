package com.fulfilment.application.monolith.stores.events;

import com.fulfilment.application.monolith.stores.Store;

/** Fired by {@code StoreResource} when an existing {@link Store} is updated or patched. */
public class StoreUpdatedEvent {

  public final Store store;

  public StoreUpdatedEvent(Store store) {
    this.store = store;
  }
}
