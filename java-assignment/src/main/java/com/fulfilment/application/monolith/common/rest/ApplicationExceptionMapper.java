package com.fulfilment.application.monolith.common.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfilment.application.monolith.common.exceptions.ApplicationException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Single mapper for the whole {@link ApplicationException} hierarchy (warehouse validation errors,
 * fulfillment constraint violations, etc.), so each new business-rule exception only needs to
 * declare its {@link Response.Status} rather than wiring up its own mapper.
 */
@Provider
public class ApplicationExceptionMapper implements ExceptionMapper<ApplicationException> {

  private static final Logger LOGGER = Logger.getLogger(ApplicationExceptionMapper.class.getName());

  @Inject ObjectMapper objectMapper;

  @Override
  public Response toResponse(ApplicationException exception) {
    int code = exception.status().getStatusCode();
    LOGGER.warnf(exception, "Rejecting request: %s", exception.getMessage());

    return Response.status(code)
        .entity(RestExceptionMapper.toErrorJson(objectMapper, exception, code))
        .build();
  }
}
