package com.fulfilment.application.monolith.common.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.hibernate.exception.ConstraintViolationException;
import org.jboss.logging.Logger;

/**
 * Generic fallback mapper for any exception not covered by a more specific mapper (e.g. {@link
 * ApplicationExceptionMapper}). Centralizes the {exceptionType, code, error} JSON error shape
 * previously duplicated as an inner class in each REST resource.
 */
@Provider
public class RestExceptionMapper implements ExceptionMapper<Exception> {

  private static final Logger LOGGER = Logger.getLogger(RestExceptionMapper.class.getName());

  private static final int MAX_CAUSE_DEPTH = 20;

  @Inject ObjectMapper objectMapper;

  @Override
  public Response toResponse(Exception exception) {
    LOGGER.error("Failed to handle request", exception);

    if (exception instanceof WebApplicationException) {
      int code = ((WebApplicationException) exception).getResponse().getStatus();
      return Response.status(code)
          .entity(toErrorJson(objectMapper, exception.getMessage(), code, exception))
          .build();
    }

    // A database-level FK/unique constraint violation surfaces here (e.g. from the @Transactional
    // interceptor's commit-time flush, not from the resource method itself) as a wrapped/rollback
    // exception, not directly as a ConstraintViolationException - so the cause chain is checked
    // rather than the exception's own type. Reported as 409, with a generic message that doesn't
    // leak the underlying table/constraint names to the client.
    if (findCause(exception, ConstraintViolationException.class) != null) {
      int code = Response.Status.CONFLICT.getStatusCode();
      return Response.status(code)
          .entity(
              toErrorJson(
                  objectMapper,
                  "The request conflicts with existing data (e.g. a referenced record still"
                      + " exists).",
                  code,
                  exception))
          .build();
    }

    int code = 500;
    return Response.status(code)
        .entity(toErrorJson(objectMapper, exception.getMessage(), code, exception))
        .build();
  }

  private static Throwable findCause(Throwable exception, Class<? extends Throwable> type) {
    Throwable current = exception;
    for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
      if (type.isInstance(current)) {
        return current;
      }
      current = current.getCause();
    }
    return null;
  }

  static ObjectNode toErrorJson(ObjectMapper objectMapper, Exception exception, int code) {
    return toErrorJson(objectMapper, exception.getMessage(), code, exception);
  }

  private static ObjectNode toErrorJson(
      ObjectMapper objectMapper, String message, int code, Exception exception) {
    ObjectNode exceptionJson = objectMapper.createObjectNode();
    exceptionJson.put("exceptionType", exception.getClass().getName());
    exceptionJson.put("code", code);

    if (message != null) {
      exceptionJson.put("error", message);
    }

    return exceptionJson;
  }
}
