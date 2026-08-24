package com.fulfilment.application.monolith.warehouses.domain.exceptions;

import com.fulfilment.application.monolith.common.exceptions.ApplicationException;
import jakarta.ws.rs.core.Response;

public class WarehouseAlreadyArchivedException extends ApplicationException {

  public WarehouseAlreadyArchivedException(String message) {
    super(message);
  }

  @Override
  public Response.Status status() {
    return Response.Status.CONFLICT;
  }
}