package io.restassured.module.webtestclient;

import io.restassured.module.webtestclient.setup.PatchController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.File;
import java.io.IOException;

public class PatchTest {
    @BeforeAll
    public static void configureWebTestClientInstance() {
        RestAssuredWebTestClient.webTestClient(WebTestClient.bindToController(new PatchController()).build());
    }

    @AfterAll
    public static void restRestAssured() {
        RestAssuredWebTestClient.reset();
    }

    @Test
    public void
    can_supply_multipart_file_as_parameter_for_patch() throws IOException {
        File file = new File("rest-assured.txt");
        RestAssuredWebTestClient.given()
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .multiPart(file)
                .when()
                .patch("/multipartFileUpload")
                .then()
                .statusCode(200)
                .log().all();
        file.delete();
    }
}
