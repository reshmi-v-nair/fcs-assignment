package com.fulfilment.application.monolith.stores;

import com.fulfilment.application.monolith.stores.events.StoreCreatedEvent;
import com.fulfilment.application.monolith.stores.events.StoreUpdatedEvent;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Legacy-system sync ({@code LegacyStoreManagerGateway}) is triggered indirectly, by firing {@link
 * StoreCreatedEvent}/{@link StoreUpdatedEvent} CDI events that {@code LegacyStoreSyncObserver}
 * observes with {@code @Observes(during = TransactionPhase.AFTER_SUCCESS)} - the container defers
 * actually invoking the gateway until this method's transaction commits, so a rolled-back change is
 * never propagated downstream.
 */
@Path("store")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class StoreResource {

  private static final int UNPROCESSABLE_ENTITY = 422;

  @Inject Event<StoreCreatedEvent> storeCreatedEvent;

  @Inject Event<StoreUpdatedEvent> storeUpdatedEvent;

  private static final Logger LOGGER = Logger.getLogger(StoreResource.class.getName());

  @GET
  public List<Store> get() {
    return Store.listAll(Sort.by("name"));
  }

  @GET
  @Path("{id}")
  public Store getSingle(Long id) {
    Store entity = Store.findById(id);
    if (entity == null) {
      LOGGER.warnf("Store with id of %d does not exist.", id);
      throw new WebApplicationException(
          "Store with id of " + id + " does not exist.", Response.Status.NOT_FOUND.getStatusCode());
    }
    return entity;
  }

  @POST
  @Transactional
  public Response create(Store store) {
    if (store.id != null) {
      LOGGER.warn("Rejecting store creation: id was invalidly set on request.");
      throw new WebApplicationException("Id was invalidly set on request.", UNPROCESSABLE_ENTITY);
    }

    store.persist();

    LOGGER.infof("Created store %d (%s)", store.id, store.name);
    storeCreatedEvent.fire(new StoreCreatedEvent(store));

    return Response.ok(store).status(201).build();
  }

  @PUT
  @Path("{id}")
  @Transactional
  public Store update(Long id, Store updatedStore) {
    if (updatedStore.name == null) {
      LOGGER.warnf("Rejecting update of store %d: name was not set on request.", id);
      throw new WebApplicationException("Store Name was not set on request.", UNPROCESSABLE_ENTITY);
    }

    Store entity = Store.findById(id);

    if (entity == null) {
      LOGGER.warnf("Store with id of %d does not exist.", id);
      throw new WebApplicationException(
          "Store with id of " + id + " does not exist.", Response.Status.NOT_FOUND.getStatusCode());
    }

    entity.name = updatedStore.name;
    entity.quantityProductsInStock = updatedStore.quantityProductsInStock;

    LOGGER.infof("Updated store %d (%s)", id, entity.name);
    storeUpdatedEvent.fire(new StoreUpdatedEvent(updatedStore));

    return entity;
  }

  @PATCH
  @Path("{id}")
  @Transactional
  public Store patch(Long id, Store updatedStore) {
    if (updatedStore.name == null) {
      LOGGER.warnf("Rejecting patch of store %d: name was not set on request.", id);
      throw new WebApplicationException("Store Name was not set on request.", UNPROCESSABLE_ENTITY);
    }

    Store entity = Store.findById(id);

    if (entity == null) {
      LOGGER.warnf("Store with id of %d does not exist.", id);
      throw new WebApplicationException(
          "Store with id of " + id + " does not exist.", Response.Status.NOT_FOUND.getStatusCode());
    }

    if (entity.name != null) {
      entity.name = updatedStore.name;
    }

    if (entity.quantityProductsInStock != 0) {
      entity.quantityProductsInStock = updatedStore.quantityProductsInStock;
    }

    LOGGER.infof("Patched store %d (%s)", id, entity.name);
    storeUpdatedEvent.fire(new StoreUpdatedEvent(updatedStore));

    return entity;
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(Long id) {
    Store entity = Store.findById(id);
    if (entity == null) {
      LOGGER.warnf("Store with id of %d does not exist.", id);
      throw new WebApplicationException(
          "Store with id of " + id + " does not exist.", Response.Status.NOT_FOUND.getStatusCode());
    }
    entity.delete();
    LOGGER.infof("Deleted store %d", id);
    return Response.status(204).build();
  }
}
