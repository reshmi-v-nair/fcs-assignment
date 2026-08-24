package com.fulfilment.application.monolith.fulfillment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

/**
 * REST-level tests for the bonus Warehouse-Store-Product fulfillment association feature. Seed data
 * (import.sql): store 1 (TONSTAD) is fulfilled for product 1 by warehouses MWH.001 and MWH.012, and
 * for product 2 by MWH.012; store 2 (KALLAX) is fulfilled for product 3 by MWH.023.
 */
@QuarkusTest
public class FulfillmentResourceTest {

  private static final String PATH = "fulfillment";

  private String createProduct(String name) {
    String body = "{\"name\":\"" + name + "\"}";
    return given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("product")
        .then()
        .statusCode(201)
        .extract()
        .path("id")
        .toString();
  }

  private void createWarehouse(String buCode, String location, int capacity, int stock) {
    String body =
        String.format(
            "{\"businessUnitCode\":\"%s\",\"location\":\"%s\",\"capacity\":%d,\"stock\":%d}",
            buCode, location, capacity, stock);
    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("warehouse")
        .then()
        .statusCode(200);
  }

  private void assign(long storeId, long productId, String buCode, int expectedStatus) {
    String body =
        String.format(
            "{\"storeId\":%d,\"productId\":%d,\"warehouseBusinessUnitCode\":\"%s\"}",
            storeId, productId, buCode);
    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(expectedStatus);
  }

  @Test
  void assignsNewFulfillmentSuccessfully() {
    // store 2 (KALLAX) already has MWH.023; adding MWH.001 for product 1 is a new distinct
    // warehouse for that store, still within the max-3-warehouses-per-store limit.
    assign(2, 1, "MWH.001", 201);
  }

  @Test
  void reassigningTheSameTripleIsIdempotent() {
    // Already seeded: store 2, product 3, MWH.023.
    assign(2, 3, "MWH.023", 201);
  }

  @Test
  void rejectsMaxWarehousesPerProductPerStore() {
    // product 1 at store 1 is already fulfilled by MWH.001 and MWH.012 (the max of 2).
    assign(1, 1, "MWH.023", 409);
  }

  @Test
  void rejectsMaxWarehousesPerStore() {
    // Bring store 1 to its 3rd distinct warehouse (still within the max of 3).
    assign(1, 2, "MWH.023", 201);

    createWarehouse("MWH.930", "AMSTERDAM-002", 50, 10);
    String freshProductId = createProduct("STORE-CAP-TEST-PRODUCT");

    // store 1 already has 3 distinct warehouses (MWH.001, MWH.012, MWH.023); a 4th is rejected
    // even though this product has no prior assignments at this store.
    assign(1, Long.parseLong(freshProductId), "MWH.930", 409);
  }

  @Test
  void rejectsMaxProductsPerWarehouse() {
    createWarehouse("MWH.931", "VETSBY-001", 90, 10);

    for (int i = 1; i <= 5; i++) {
      String productId = createProduct("WAREHOUSE-CAP-PRODUCT-" + i);
      // store 3 (BESTÅ) has no prior assignments, so it never approaches the per-store or
      // per-product-per-store limits here - every assignment uses the same single warehouse.
      assign(3, Long.parseLong(productId), "MWH.931", 201);
    }

    String sixthProductId = createProduct("WAREHOUSE-CAP-PRODUCT-6");
    assign(3, Long.parseLong(sixthProductId), "MWH.931", 409);
  }

  @Test
  void rejectsUnknownStore() {
    assign(999999, 1, "MWH.001", 404);
  }

  @Test
  void rejectsUnknownProduct() {
    assign(2, 999999, "MWH.001", 404);
  }

  @Test
  void rejectsUnknownWarehouse() {
    assign(2, 1, "MWH.999999", 404);
  }

  @Test
  void listsAllWhenNoFilterProvided() {
    given().when().get(PATH).then().statusCode(200).body(containsString("MWH.001"));
  }

  @Test
  void listsByWarehouseBusinessUnitCode() {
    given()
        .when()
        .get(PATH + "?warehouseBusinessUnitCode=MWH.023")
        .then()
        .statusCode(200)
        .body(containsString("MWH.023"));
  }

  @Test
  void listsByStoreId() {
    given().when().get(PATH + "?storeId=1").then().statusCode(200).body(containsString("MWH.001"));
  }

  @Test
  void rejectsListByUnknownStoreId() {
    given().when().get(PATH + "?storeId=999999").then().statusCode(404);
  }

  @Test
  void deletesFulfillmentAssignment() {
    String body = "{\"storeId\":2,\"productId\":2,\"warehouseBusinessUnitCode\":\"MWH.023\"}";
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id")
            .toString();

    given().when().delete(PATH + "/" + id).then().statusCode(204);
  }

  @Test
  void rejectsDeleteOfUnknownFulfillmentAssignment() {
    given().when().delete(PATH + "/999999").then().statusCode(404);
  }
}
