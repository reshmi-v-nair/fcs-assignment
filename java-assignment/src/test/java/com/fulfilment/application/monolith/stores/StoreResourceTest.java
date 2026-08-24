package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StoreResourceTest {

  private static final String PATH = "store";

  @InjectMock LegacyStoreManagerGateway legacyStoreManagerGateway;

  @Test
  void listsSeededStores() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"));
  }

  @Test
  void getByUnknownIdReturns404() {
    given().when().get(PATH + "/999999").then().statusCode(404);
  }

  @Test
  void createsStoreAndSyncsToLegacySystemAfterCommit() {
    String body = "{\"name\":\"NEW-STORE\",\"quantityProductsInStock\":7}";

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body(containsString("NEW-STORE"));

    verify(legacyStoreManagerGateway).createStoreOnLegacySystem(any(Store.class));
  }

  @Test
  void rejectsCreateWithIdSet() {
    String body = "{\"id\":1,\"name\":\"SHOULD-FAIL\",\"quantityProductsInStock\":1}";

    given().contentType(ContentType.JSON).body(body).when().post(PATH).then().statusCode(422);
  }

  @Test
  void updatesStoreAndSyncsToLegacySystemAfterCommit() {
    String createBody = "{\"name\":\"UPDATE-ME\",\"quantityProductsInStock\":1}";
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

    String updateBody = "{\"name\":\"UPDATED\",\"quantityProductsInStock\":2}";
    given()
        .contentType(ContentType.JSON)
        .body(updateBody)
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body(containsString("UPDATED"));

    verify(legacyStoreManagerGateway).updateStoreOnLegacySystem(any(Store.class));
  }

  @Test
  void rejectsUpdateOfUnknownId() {
    String updateBody = "{\"name\":\"WHATEVER\",\"quantityProductsInStock\":2}";
    given()
        .contentType(ContentType.JSON)
        .body(updateBody)
        .when()
        .put(PATH + "/999999")
        .then()
        .statusCode(404);
  }

  @Test
  void rejectsUpdateWithMissingName() {
    String createBody = "{\"name\":\"UPDATE-MISSING-NAME\",\"quantityProductsInStock\":1}";
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

    String updateBody = "{\"quantityProductsInStock\":5}";
    given()
        .contentType(ContentType.JSON)
        .body(updateBody)
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(422);
  }

  @Test
  void patchUpdatesStoreFields() {
    String createBody = "{\"name\":\"PATCH-ME\",\"quantityProductsInStock\":1}";
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

    String patchBody = "{\"name\":\"PATCHED\",\"quantityProductsInStock\":3}";
    given()
        .contentType(ContentType.JSON)
        .body(patchBody)
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body(containsString("PATCHED"));

    verify(legacyStoreManagerGateway).updateStoreOnLegacySystem(any(Store.class));
  }

  @Test
  void patchDoesNotOverwriteStockWhenExistingStockIsZero() {
    // patch() only overwrites quantityProductsInStock when the entity's CURRENT value is
    // non-zero (a fragile "was this field set" heuristic - see TESTING.md/QUESTIONS.md); this
    // pins down that documented behavior for the zero-stock branch.
    String createBody = "{\"name\":\"PATCH-ZERO-STOCK\",\"quantityProductsInStock\":0}";
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

    String patchBody = "{\"name\":\"PATCH-ZERO-STOCK-RENAMED\",\"quantityProductsInStock\":99}";
    given()
        .contentType(ContentType.JSON)
        .body(patchBody)
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body(
            containsString("PATCH-ZERO-STOCK-RENAMED"),
            containsString("\"quantityProductsInStock\":0"));
  }

  @Test
  void rejectsPatchWithMissingName() {
    String createBody = "{\"name\":\"PATCH-MISSING-NAME\",\"quantityProductsInStock\":1}";
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

    String patchBody = "{\"quantityProductsInStock\":3}";
    given()
        .contentType(ContentType.JSON)
        .body(patchBody)
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(422);
  }

  @Test
  void deletesStore() {
    String createBody = "{\"name\":\"DELETE-ME\",\"quantityProductsInStock\":1}";
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
    given().when().get(PATH + "/" + id).then().statusCode(404);
  }

  @Test
  void rejectsDeleteOfUnknownId() {
    given().when().delete(PATH + "/999999").then().statusCode(404);
  }

  @Test
  void rejectsDeleteOfStoreWithFulfillmentAssignment() {
    // Store 1 (TONSTAD) is referenced by seeded fulfillment assignments (import.sql); deleting it
    // must be reported as a clean 409 Conflict, not an unhandled 500 from the underlying database
    // foreign-key constraint violation.
    given().when().delete(PATH + "/1").then().statusCode(409);
  }
}
