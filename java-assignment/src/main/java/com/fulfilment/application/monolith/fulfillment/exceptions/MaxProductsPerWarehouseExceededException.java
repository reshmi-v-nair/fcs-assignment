package com.fulfilment.application.monolith.fulfillment.exceptions;

import com.fulfilment.application.monolith.common.exceptions.ApplicationException;
import jakarta.ws.rs.core.Response;

public class MaxProductsPerWarehouseExceededException extends ApplicationException {

  public MaxProductsPerWarehouseExceededException(String message) {
    super(message);
  }

  @Override
  public Response.Status status() {
    return Response.Status.CONFLICT;
  }
}