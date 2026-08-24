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
}