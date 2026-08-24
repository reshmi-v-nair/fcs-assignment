package com.fulfilment.application.monolith.common.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Generic fallback mapper for any exception not covered by a more specific mapper (e.g. {@link
 * ApplicationExceptionMapper}). Centralizes the {exceptionType, code, error} JSON error shape
 * previously duplicated as an inner class in each REST resource.
 */
@Provider
public class RestExceptionMapper implements ExceptionMapper<Exception> {

  private static final Logger LOGGER = Logger.getLogger(RestExceptionMapper.class.getName());

  @Inject ObjectMapper objectMapper;

  @Override
  public Response toResponse(Exception exception) {
    LOGGER.error("Failed to handle request", exception);

    int code = 500;
    if (exception instanceof WebApplicationException) {
      code = ((WebApplicationException) exception).getResponse().getStatus();
    }

    return Response.status(code).entity(toErrorJson(objectMapper, exception, code)).build();
  }

  static ObjectNode toErrorJson(ObjectMapper objectMapper, Exception exception, int code) {
    ObjectNode exceptionJson = objectMapper.createObjectNode();
    exceptionJson.put("exceptionType", exception.getClass().getName());
    exceptionJson.put("code", code);

    if (exception.getMessage() != null) {
      exceptionJson.put("error", exception.getMessage());
    }

    return exceptionJson;
  }
}