package com.fulfilment.application.monolith.warehouses.domain.exceptions;

import com.fulfilment.application.monolith.common.exceptions.ApplicationException;
import jakarta.ws.rs.core.Response;

public class InvalidLocationException extends ApplicationException {

  public InvalidLocationException(String message) {
    super(message);
  }

  @Override
  public Response.Status status() {
    return Response.Status.BAD_REQUEST;
  }
}