package com.fulfilment.application.monolith.fulfillment;

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
import com.fulfilment.application.monolith.stores.Store;
import java.util.List;

@Path("fulfillment")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class FulfillmentResource {

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
        throw new WebApplicationException("Store with id of " + storeId + " does not exist.", 404);
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
    return Response.ok(assignment).status(201).build();
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(Long id) {
    FulfillmentAssignment entity = fulfillmentAssignmentRepository.findById(id);
    if (entity == null) {
      throw new WebApplicationException(
          "Fulfillment assignment with id of " + id + " does not exist.", 404);
    }
    fulfillmentAssignmentRepository.delete(entity);
    return Response.status(204).build();
  }
}