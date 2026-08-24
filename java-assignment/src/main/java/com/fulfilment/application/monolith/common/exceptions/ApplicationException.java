package com.fulfilment.application.monolith.common.exceptions;

import jakarta.ws.rs.core.Response;

/**
 * Base type for domain/business-rule violations that should be translated into a specific HTTP
 * status by {@link com.fulfilment.application.monolith.common.rest.ApplicationExceptionMapper},
 * instead of falling back to a generic 500.
 */
public abstract class ApplicationException extends RuntimeException {

  protected ApplicationException(String message) {
    super(message);
  }

  public abstract Response.Status status();
}
