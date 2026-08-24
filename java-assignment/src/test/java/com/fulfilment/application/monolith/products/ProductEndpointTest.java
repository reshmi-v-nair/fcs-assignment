package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ProductEndpointTest {

  private static final String PATH = "product";

  @Test
  public void testCrudProduct() {
    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));

    // Create and delete a fresh product with no fulfillment assignments referencing it (the
    // seeded products can't be deleted - see rejectsDeleteOfProductWithFulfillmentAssignment):
    String createBody = "{\"name\":\"CRUD-DELETE-TEST\"}";
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(createBody)
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id")
            .toString();

    given().when().delete(PATH + "/" + id).then().statusCode(204);

    given().when().get(PATH).then().statusCode(200).body(not(containsString("CRUD-DELETE-TEST")));
  }

  @Test
  void rejectsDeleteOfProductWithFulfillmentAssignment() {
    // Product 1 (TONSTAD) is referenced by seeded fulfillment assignments (import.sql); deleting
    // it must be reported as a clean 409 Conflict, not an unhandled 500 from the underlying
    // database foreign-key constraint violation.
    given().when().delete(PATH + "/1").then().statusCode(409);
  }

  @Test
  void rejectsCreateWithIdSet() {
    String body = "{\"id\":1,\"name\":\"SHOULD-FAIL\"}";

    given().contentType(ContentType.JSON).body(body).when().post(PATH).then().statusCode(422);
  }

  @Test
  void getByUnknownIdReturns404() {
    given().when().get(PATH + "/999999").then().statusCode(404);
  }

  @Test
  void rejectsUpdateOfUnknownId() {
    String body = "{\"name\":\"WHATEVER\"}";
    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .put(PATH + "/999999")
        .then()
        .statusCode(404);
  }

  @Test
  void rejectsUpdateWithMissingName() {
    String createBody = "{\"name\":\"UPDATE-MISSING-NAME-TEST\"}";
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(createBody)
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id")
            .toString();

    String updateBody = "{\"description\":\"no name here\"}";
    given()
        .contentType(ContentType.JSON)
        .body(updateBody)
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(422);
  }

  @Test
  void rejectsDeleteOfUnknownId() {
    given().when().delete(PATH + "/999999").then().statusCode(404);
  }

  @Test
  void updatesProductFields() {
    String createBody = "{\"name\":\"UPDATE-FIELDS-TEST\",\"stock\":5}";
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(createBody)
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id")
            .toString();

    String updateBody =
        "{\"name\":\"UPDATE-FIELDS-TEST-RENAMED\",\"description\":\"a description\",\"stock\":9}";
    given()
        .contentType(ContentType.JSON)
        .body(updateBody)
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body(containsString("UPDATE-FIELDS-TEST-RENAMED"), containsString("a description"));
  }
}
