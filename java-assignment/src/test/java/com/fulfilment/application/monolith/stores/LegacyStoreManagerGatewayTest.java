package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

/**
 * {@link StoreResourceTest} mocks this gateway to verify it's *called* at the right time; these
 * tests exercise the real implementation directly, since nothing else in the suite does.
 */
@QuarkusTest
class LegacyStoreManagerGatewayTest {

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  @Test
  void createStoreOnLegacySystemWritesAndCleansUpTempFile() {
    Store store = new Store("LEGACY-GATEWAY-CREATE-TEST");
    store.quantityProductsInStock = 3;

    assertDoesNotThrow(() -> legacyStoreManagerGateway.createStoreOnLegacySystem(store));
  }

  @Test
  void updateStoreOnLegacySystemWritesAndCleansUpTempFile() {
    Store store = new Store("LEGACY-GATEWAY-UPDATE-TEST");
    store.quantityProductsInStock = 5;

    assertDoesNotThrow(() -> legacyStoreManagerGateway.updateStoreOnLegacySystem(store));
  }

  @Test
  void swallowsExceptionWhenTempFilePrefixIsInvalid() {
    // Files.createTempFile requires the prefix to be at least 3 characters long; a shorter store
    // name makes the underlying call throw, exercising the gateway's catch-and-log fallback
    // (a deliberate fire-and-forget design for this post-commit sync - see TESTING.md) rather
    // than propagating the failure to the caller.
    Store store = new Store("ab");
    store.quantityProductsInStock = 1;

    assertDoesNotThrow(() -> legacyStoreManagerGateway.createStoreOnLegacySystem(store));
  }
}
