package com.fulfilment.application.monolith.stores.events;

import com.fulfilment.application.monolith.stores.LegacyStoreManagerGateway;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

/**
 * Observes Store change events only after the surrounding JTA transaction has committed
 * successfully ({@link TransactionPhase#AFTER_SUCCESS}), so the legacy system never observes a
 * Store change that was rolled back. Replaces a manual {@code
 * TransactionSynchronizationRegistry}-based callback in {@code StoreResource} with CDI's native
 * transactional-observer support - functionally equivalent, but the "run only after commit" rule is
 * enforced by the container rather than hand-written synchronization code.
 */
@ApplicationScoped
public class LegacyStoreSyncObserver {

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  void onStoreCreated(@Observes(during = TransactionPhase.AFTER_SUCCESS) StoreCreatedEvent event) {
    legacyStoreManagerGateway.createStoreOnLegacySystem(event.store);
  }

  void onStoreUpdated(@Observes(during = TransactionPhase.AFTER_SUCCESS) StoreUpdatedEvent event) {
    legacyStoreManagerGateway.updateStoreOnLegacySystem(event.store);
  }
}
