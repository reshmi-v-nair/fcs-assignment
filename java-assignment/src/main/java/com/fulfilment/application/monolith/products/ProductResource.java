package com.fulfilment.application.monolith.products;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.jboss.logging.Logger;

@Path("product")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class ProductResource {

  private static final int UNPROCESSABLE_ENTITY = 422;

  private static final Logger LOGGER = Logger.getLogger(ProductResource.class.getName());

  @Inject ProductRepository productRepository;

  @GET
  public List<Product> get() {
    return productRepository.listAll(Sort.by("name"));
  }

  @GET
  @Path("{id}")
  public Product getSingle(Long id) {
    Product entity = productRepository.findById(id);
    if (entity == null) {
      LOGGER.warnf("Product with id of %d does not exist.", id);
      throw new WebApplicationException(
          "Product with id of " + id + " does not exist.",
          Response.Status.NOT_FOUND.getStatusCode());
    }
    return entity;
  }

  @POST
  @Transactional
  public Response create(Product product) {
    if (product.id != null) {
      LOGGER.warn("Rejecting product creation: id was invalidly set on request.");
      throw new WebApplicationException("Id was invalidly set on request.", UNPROCESSABLE_ENTITY);
    }

    productRepository.persist(product);
    LOGGER.infof("Created product %d (%s)", product.id, product.name);
    return Response.ok(product).status(201).build();
  }

  @PUT
  @Path("{id}")
  @Transactional
  public Product update(Long id, Product product) {
    if (product.name == null) {
      LOGGER.warnf("Rejecting update of product %d: name was not set on request.", id);
      throw new WebApplicationException(
          "Product Name was not set on request.", UNPROCESSABLE_ENTITY);
    }

    Product entity = productRepository.findById(id);

    if (entity == null) {
      LOGGER.warnf("Product with id of %d does not exist.", id);
      throw new WebApplicationException(
          "Product with id of " + id + " does not exist.",
          Response.Status.NOT_FOUND.getStatusCode());
    }

    entity.name = product.name;
    entity.description = product.description;
    entity.price = product.price;
    entity.stock = product.stock;

    productRepository.persist(entity);

    LOGGER.infof("Updated product %d (%s)", id, entity.name);
    return entity;
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(Long id) {
    Product entity = productRepository.findById(id);
    if (entity == null) {
      LOGGER.warnf("Product with id of %d does not exist.", id);
      throw new WebApplicationException(
          "Product with id of " + id + " does not exist.",
          Response.Status.NOT_FOUND.getStatusCode());
    }
    productRepository.delete(entity);
    LOGGER.infof("Deleted product %d", id);
    return Response.status(204).build();
  }
}
