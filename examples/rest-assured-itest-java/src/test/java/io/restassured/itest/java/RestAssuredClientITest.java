/*
 * Copyright 2026 the original author or authors.
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

package io.restassured.itest.java;

import io.restassured.RestAssured;
import io.restassured.RestAssuredClient;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.itest.java.support.WithJetty;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.restassured.RestAssured.basic;
import static io.restassured.RestAssured.get;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

/**
 * A {@link RestAssuredClient} sends with its own settings while the static fields of {@link RestAssured} hold others.
 */
public class RestAssuredClientITest extends WithJetty {

    @Test
    void the_settings_of_a_client_reach_the_server() {
        List<String> filtersRun = new CopyOnWriteArrayList<>();
        Filter recording = (request, response, context) -> {
            filtersRun.add(request.getURI());
            return context.next(request, response);
        };
        try (RestAssuredClient client = RestAssuredClient.builder().baseUri("http://127.0.0.1").port(8080).basePath("/secured")
                .authentication(basic("jetty", "jetty")).filters(recording)
                .requestSpecification(new RequestSpecBuilder().addQueryParam("n", "1").build()).build()) {
            // A closed port
            RestAssured.port = 1;
            RestAssured.basePath = "/wrong";

            client.get("/hello").then().statusCode(200);
            client.given().basePath("").get("/requestUrl").then().body(equalTo("http://127.0.0.1:8080/requestUrl?n=1"));
        }

        assertThat(filtersRun).containsExactly("http://127.0.0.1:8080/secured/hello?n=1", "http://127.0.0.1:8080/requestUrl?n=1");
        assertThatThrownBy(() -> get("/hello")).isInstanceOf(Exception.class);
    }

    @Test
    void then_validates_with_the_root_path_and_response_specification_of_the_client() {
        try (RestAssuredClient rooted = RestAssuredClient.builder().rootPath("greeting").build();
             RestAssuredClient expectingJohn = RestAssuredClient.builder()
                     .responseSpecification(new ResponseSpecBuilder().expectBody("greeting.firstName", equalTo("John")).build()).build()) {
            RestAssured.rootPath = "other";
            RestAssured.responseSpecification = new ResponseSpecBuilder().expectStatusCode(404).build();

            rooted.get("/greetJSON?firstName=John&lastName=Doe").then().body("firstName", equalTo("John")).body("lastName", equalTo("Doe"));
            expectingJohn.get("/greetJSON?firstName=John&lastName=Doe").then().statusCode(200);
            assertThatThrownBy(() -> expectingJohn.get("/greetJSON?firstName=Jane&lastName=Doe"))
                    .isInstanceOf(AssertionError.class).hasMessageContaining("JSON path greeting.firstName doesn't match.");
        }
    }

    @Test
    void a_client_keeps_its_own_cookies_apart_from_those_of_the_static_api() {
        RestAssuredConfig keepCookies = config().httpClient(httpClientConfig().reuseHttpClientInstance().keepResponseCookies(true));
        RestAssured.config = keepCookies;
        try (RestAssuredClient snapshot = RestAssured.client();
             RestAssuredClient built = RestAssuredClient.builder().config(keepCookies).build()) {

            get("/setCookies");
            assertThat(cookieNames(get("/cookie").asString())).containsExactlyInAnyOrder("key1", "key2", "key3");
            assertThat(snapshot.get("/cookie").asString()).doesNotContain("key1");
            snapshot.get("/setCookies");
            assertThat(cookieNames(snapshot.get("/cookie").asString())).containsExactlyInAnyOrder("key1", "key2", "key3");
            assertThat(built.get("/cookie").asString()).doesNotContain("key1");
        }
    }

    @Test
    void a_closed_client_fails_and_leaves_the_static_api_alone() {
        RestAssuredClient client = RestAssured.client();
        client.get("/greetJSON?firstName=John&lastName=Doe").then().statusCode(200);

        client.close();

        assertThatThrownBy(() -> client.get("/greetJSON")).isInstanceOf(IllegalStateException.class).hasMessage("This RestAssuredClient is closed");
        get("/greetJSON?firstName=John&lastName=Doe").then().statusCode(200);
    }

    private static List<String> cookieNames(String body) {
        return Arrays.asList(body.split(", "));
    }
}
