package org.oneocean;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
public class ExampleWsServerResourceTest {

    @Test
    public void testWsConnectionsEndpoint() {
        given()
          .when().get("/ws-server/connections")
          .then()
             .statusCode(200)
             .body(containsString("Active connections"));
    }

}