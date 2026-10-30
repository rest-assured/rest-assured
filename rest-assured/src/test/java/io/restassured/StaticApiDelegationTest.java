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

package io.restassured;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.common.spi.http.HttpHeader;
import io.restassured.common.spi.http.HttpResponse;
import io.restassured.config.RestAssuredConfig;
import io.restassured.internal.http.client.FakeHttpClients;
import io.restassured.response.Response;
import io.restassured.specification.RequestSender;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.List;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

/**
 * The static API reads the static fields when a request is started, and <code>then()</code> reads the static root path
 * and response specification when it's called, as before {@link RestAssuredClient}.
 */
class StaticApiDelegationTest {
    private static final RestAssuredConfig FAKE = config().httpClient(httpClientConfig().client(FakeHttpClients.Other.class));

    @BeforeEach
    void fakeClient() {
        RestAssured.config = FAKE;
    }

    @AfterEach
    void reset() {
        FakeHttpClients.reset();
        RestAssured.reset();
    }

    @Test
    void each_given_reads_the_static_fields_when_it_is_called() {
        RestAssured.baseURI = "http://first.example.org";
        RequestSpecification first = given();
        RestAssured.baseURI = "http://second.example.org";
        RestAssured.port = 1234;
        RestAssured.requestSpecification = new RequestSpecBuilder().addHeader("X-Second", "1").build();

        first.get("/path");
        given().get("/path");

        assertThat(FakeHttpClients.sent.get(0).uri()).isEqualTo(URI.create("http://first.example.org/path"));
        assertThat(FakeHttpClients.sent.get(0).headers()).noneMatch(header -> header.name().equals("X-Second"));
        assertThat(FakeHttpClients.sent.get(1).uri()).isEqualTo(URI.create("http://second.example.org:1234/path"));
        assertThat(FakeHttpClients.sent.get(1).headers()).contains(new HttpHeader("X-Second", "1"));
    }

    @Test
    void every_entry_point_and_send_shortcut_reads_the_static_fields_when_it_is_called() {
        List<Supplier<Response>> requests = List.of(
                () -> given().get("/path"),
                () -> RestAssured.with().get("/path"),
                () -> RestAssured.when().get("/path"),
                () -> RestAssured.expect().when().get("/path"),
                () -> given(new RequestSpecBuilder().build()).get("/path"),
                () -> given(new RequestSpecBuilder().build(), new ResponseSpecBuilder().build()).get("/path"),
                () -> RestAssured.get("/path"));

        for (int i = 0; i < requests.size(); i++) {
            RestAssured.baseURI = "http://host" + i + ".example.org";
            requests.get(i).get();
            assertThat(FakeHttpClients.sent.get(i).uri()).isEqualTo(URI.create("http://host" + i + ".example.org/path"));
        }
    }

    @Test
    void then_reads_the_static_root_path_and_response_specification_when_it_is_called() {
        FakeHttpClients.response = request -> json("{\"a\": {\"b\": 1}, \"c\": {\"b\": 2}}");
        RestAssured.rootPath = "a";
        Response response = RestAssured.get("/path");

        RestAssured.rootPath = "c";
        response.then().body("b", equalTo(2));
        RestAssured.responseSpecification = new ResponseSpecBuilder().expectStatusCode(201).build();
        assertThatThrownBy(() -> response.then().body("b", equalTo(2)))
                .isInstanceOf(AssertionError.class).hasMessageContaining("Expected status code <201> but was <200>");
    }

    @Test
    void given_with_a_request_and_a_response_specification_sends_them_as_they_are() {
        RequestSpecification request = given().header("X-Request", "1");
        RestAssured.requestSpecification = new RequestSpecBuilder().addHeader("X-Static", "1").build();
        RequestSender sender = given(request, new ResponseSpecBuilder().expectStatusCode(200).build());

        sender.get("/path");

        assertThat(FakeHttpClients.sent.get(0).headers()).contains(new HttpHeader("X-Request", "1"))
                .noneMatch(header -> header.name().equals("X-Static"));
    }

    private static HttpResponse json(String body) {
        byte[] bytes = body.getBytes(UTF_8);
        return new HttpResponse("HTTP/1.1", 200, "OK", List.of(new HttpHeader("Content-Type", "application/json")), bytes.length,
                new ByteArrayInputStream(bytes), null);
    }
}
