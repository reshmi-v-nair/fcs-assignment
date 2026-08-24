package com.fulfilment.application.monolith.warehouses.domain.exceptions;

import com.fulfilment.application.monolith.common.exceptions.ApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Covers every rule where a warehouse's capacity is insufficient for what's being asked of it:
 * capacity above the location's maximum, capacity below the informed/existing stock, or capacity
 * unable to accommodate the stock of a warehouse being replaced.
 */
public class WarehouseCapacityExceededException extends ApplicationException {

  public WarehouseCapacityExceededException(String message) {
    super(message);
  }

  @Override
  public Response.Status status() {
    return Response.Status.BAD_REQUEST;
  }
}
