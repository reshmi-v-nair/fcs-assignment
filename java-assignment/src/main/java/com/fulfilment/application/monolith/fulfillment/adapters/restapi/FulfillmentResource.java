package com.fulfilment.application.monolith.fulfillment.adapters.restapi;

import com.fulfilment.application.monolith.fulfillment.FulfillmentAssignment;
import com.fulfilment.application.monolith.fulfillment.FulfillmentAssignmentService;
import com.fulfilment.application.monolith.fulfillment.adapters.database.FulfillmentAssignmentRepository;
import com.fulfilment.application.monolith.stores.Store;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.jboss.logging.Logger;

@Path("fulfillment")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class FulfillmentResource {

  private static final Logger LOGGER = Logger.getLogger(FulfillmentResource.class.getName());

  private final FulfillmentAssignmentService fulfillmentAssignmentService;
  private final FulfillmentAssignmentRepository fulfillmentAssignmentRepository;

  public FulfillmentResource(
      FulfillmentAssignmentService fulfillmentAssignmentService,
      FulfillmentAssignmentRepository fulfillmentAssignmentRepository) {
    this.fulfillmentAssignmentService = fulfillmentAssignmentService;
    this.fulfillmentAssignmentRepository = fulfillmentAssignmentRepository;
  }

  @GET
  public List<FulfillmentAssignment> list(
      @QueryParam("storeId") Long storeId,
      @QueryParam("warehouseBusinessUnitCode") String warehouseBusinessUnitCode) {
    if (warehouseBusinessUnitCode != null) {
      return fulfillmentAssignmentRepository.listByWarehouseBusinessUnitCode(
          warehouseBusinessUnitCode);
    }
    if (storeId != null) {
      Store store = Store.findById(storeId);
      if (store == null) {
        LOGGER.warnf("Store with id of %d does not exist.", storeId);
        throw new WebApplicationException(
            "Store with id of " + storeId + " does not exist.",
            Response.Status.NOT_FOUND.getStatusCode());
      }
      return fulfillmentAssignmentRepository.listByStore(store);
    }
    return fulfillmentAssignmentRepository.listAll();
  }

  @POST
  @Transactional
  public Response create(FulfillmentAssignmentRequest request) {
    FulfillmentAssignment assignment =
        fulfillmentAssignmentService.assign(
            request.storeId, request.productId, request.warehouseBusinessUnitCode);
    LOGGER.infof(
        "Created fulfillment assignment %d (store=%d, product=%d, warehouse=%s)",
        assignment.id, request.storeId, request.productId, request.warehouseBusinessUnitCode);
    return Response.ok(assignment).status(201).build();
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(Long id) {
    FulfillmentAssignment entity = fulfillmentAssignmentRepository.findById(id);
    if (entity == null) {
      LOGGER.warnf("Fulfillment assignment with id of %d does not exist.", id);
      throw new WebApplicationException(
          "Fulfillment assignment with id of " + id + " does not exist.",
          Response.Status.NOT_FOUND.getStatusCode());
    }
    fulfillmentAssignmentRepository.delete(entity);
    LOGGER.infof("Deleted fulfillment assignment %d", id);
    return Response.status(204).build();
  }
}
