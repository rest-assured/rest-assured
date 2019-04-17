/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.restassured.module.webtestclient;

import io.restassured.http.ContentType;
import io.restassured.module.webtestclient.setup.HeaderController;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static io.restassured.module.webtestclient.RestAssuredWebTestClient.given;
import static org.hamcrest.Matchers.equalTo;

public class AcceptTest {
    @Before
    public void
    given_rest_assured_is_initialized_with_controller() {
        RestAssuredWebTestClient.standaloneSetup(new HeaderController());
    }

    @After
    public void
    rest_assured_is_reset_after_each_test() {
        RestAssuredWebTestClient.reset();
    }

    @Test
    public void
    adds_accept_by_content_type() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/headers")
                .then()
                .status(HttpStatus.OK)
                .body("Accept", equalTo("application/json, application/javascript, text/javascript, text/json"));
    }

    @Test
    public void
    adds_accept_by_string_value() {
        given()
                .accept("application/json, application/javascript")
                .when()
                .get("/headers")
                .then()
                .status(HttpStatus.OK)
                .body("Accept", equalTo("application/json, application/javascript"));
    }

    @Test
    public void
    adds_accept_by_media_type() {
        given()
                .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED)
                .when()
                .get("/headers")
                .then()
                .status(HttpStatus.OK)
                .body("Accept", equalTo("application/json, application/x-www-form-urlencoded"));
    }
}
