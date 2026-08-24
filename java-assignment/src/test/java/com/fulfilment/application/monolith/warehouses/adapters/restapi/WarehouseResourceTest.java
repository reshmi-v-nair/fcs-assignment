package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

/**
 * REST-level tests for the Warehouse endpoints. Unlike {@link WarehouseEndpointIT} (which only runs
 * under the native profile via failsafe), this uses {@code @QuarkusTest} so it runs under plain
 * {@code mvn test}. Requires a Postgres instance reachable via Quarkus Dev Services.
 */
@QuarkusTest
public class WarehouseResourceTest {

  private static final String PATH = "warehouse";

  @Test
  void listsOnlySeededActiveWarehouses() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  void createsWarehouseSuccessfully() {
    String body =
        "{\"businessUnitCode\":\"MWH.900\",\"location\":\"AMSTERDAM-002\",\"capacity\":50,\"stock\":20}";

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(200)
        .body(containsString("MWH.900"));
  }

  @Test
  void rejectsDuplicateBusinessUnitCode() {
    String body =
        "{\"businessUnitCode\":\"MWH.001\",\"location\":\"AMSTERDAM-002\",\"capacity\":50,\"stock\":20}";

    given().contentType(ContentType.JSON).body(body).when().post(PATH).then().statusCode(409);
  }

  @Test
  void rejectsInvalidLocation() {
    String body =
        "{\"businessUnitCode\":\"MWH.901\",\"location\":\"NOWHERE-999\",\"capacity\":50,\"stock\":20}";

    given().contentType(ContentType.JSON).body(body).when().post(PATH).then().statusCode(400);
  }

  @Test
  void rejectsCapacityAboveLocationMax() {
    String body =
        "{\"businessUnitCode\":\"MWH.902\",\"location\":\"AMSTERDAM-002\",\"capacity\":9999,\"stock\":20}";

    given().contentType(ContentType.JSON).body(body).when().post(PATH).then().statusCode(400);
  }

  @Test
  void getByIdReturnsSeededWarehouse() {
    given().when().get(PATH + "/1").then().statusCode(200).body(containsString("MWH.001"));
  }

  @Test
  void getByUnknownIdReturns404() {
    given().when().get(PATH + "/999999").then().statusCode(404);
  }

  @Test
  void archivesWarehouseAndRemovesItFromList() {
    String body =
        "{\"businessUnitCode\":\"MWH.910\",\"location\":\"HELMOND-001\",\"capacity\":45,\"stock\":5}";
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post(PATH)
            .then()
            .statusCode(200)
            .extract()
            .path("id")
            .toString();

    given().when().delete(PATH + "/" + id).then().statusCode(204);

    given().when().get(PATH).then().statusCode(200).body(not(containsString("MWH.910")));
  }

  @Test
  void rejectsArchivingAlreadyArchivedWarehouse() {
    String body =
        "{\"businessUnitCode\":\"MWH.911\",\"location\":\"HELMOND-001\",\"capacity\":45,\"stock\":5}";
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post(PATH)
            .then()
            .statusCode(200)
            .extract()
            .path("id")
            .toString();

    given().when().delete(PATH + "/" + id).then().statusCode(204);
    // Archiving an already-archived warehouse is a conflict with its current state, not a
    // not-found - WarehouseAlreadyArchivedException maps to 409, consistently with the other
    // warehouse domain exceptions (see ApplicationExceptionMapper).
    given().when().delete(PATH + "/" + id).then().statusCode(409);
  }

  @Test
  void replacesWarehouseSuccessfully() {
    String createBody =
        "{\"businessUnitCode\":\"MWH.920\",\"location\":\"EINDHOVEN-001\",\"capacity\":50,\"stock\":15}";
    given().contentType(ContentType.JSON).body(createBody).when().post(PATH).then().statusCode(200);

    String replaceBody = "{\"location\":\"EINDHOVEN-001\",\"capacity\":60,\"stock\":15}";
    given()
        .contentType(ContentType.JSON)
        .body(replaceBody)
        .when()
        .post(PATH + "/MWH.920/replacement")
        .then()
        .statusCode(200)
        .body(containsString("MWH.920"), containsString("60"));
  }

  @Test
  void rejectsReplaceWithStockMismatch() {
    String createBody =
        "{\"businessUnitCode\":\"MWH.921\",\"location\":\"EINDHOVEN-001\",\"capacity\":50,\"stock\":15}";
    given().contentType(ContentType.JSON).body(createBody).when().post(PATH).then().statusCode(200);

    String replaceBody = "{\"location\":\"EINDHOVEN-001\",\"capacity\":60,\"stock\":16}";
    given()
        .contentType(ContentType.JSON)
        .body(replaceBody)
        .when()
        .post(PATH + "/MWH.921/replacement")
        .then()
        .statusCode(400);
  }

  @Test
  void rejectsReplaceOfUnknownBusinessUnitCode() {
    String replaceBody = "{\"location\":\"EINDHOVEN-001\",\"capacity\":60,\"stock\":15}";
    given()
        .contentType(ContentType.JSON)
        .body(replaceBody)
        .when()
        .post(PATH + "/MWH.999999/replacement")
        .then()
        .statusCode(404);
  }
}
