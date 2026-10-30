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
import io.restassured.config.SessionConfig;
import io.restassured.http.Method;
import io.restassured.internal.http.client.FakeHttpClients;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.FutureTask;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static io.restassured.config.SessionConfig.sessionConfig;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

/**
 * Which settings a request uses: a client named in {@link RestAssured#given(RestAssuredClient)}, then the client that
 * {@link RestAssured#useClient(RestAssuredClient)} bound to the thread, then the static settings.
 */
class UseClientTest {
    private static final RestAssuredConfig FAKE = config().httpClient(httpClientConfig().client(FakeHttpClients.Other.class));
    private static final URI STATIC = URI.create("http://static.example.org/path");
    private static final URI BOUND = URI.create("http://bound.example.org/path");

    private final RestAssuredClient client = RestAssuredClient.builder().config(FAKE).baseUri("http://bound.example.org").rootPath("a").build();

    @BeforeEach
    void staticSettings() {
        RestAssured.config = FAKE;
        RestAssured.baseURI = "http://static.example.org";
        RestAssured.rootPath = "c";
        FakeHttpClients.response = request -> json("{\"a\": {\"b\": 1}, \"c\": {\"b\": 2}}");
    }

    @AfterEach
    void reset() {
        FakeHttpClients.reset();
        RestAssured.reset();
    }

    @Test
    void every_entry_point_and_send_shortcut_uses_the_bound_client() {
        RestAssured.useClient(client);
        List<Supplier<Response>> requests = List.of(
                () -> given().get("/path"),
                () -> RestAssured.with().get("/path"),
                () -> RestAssured.when().get("/path"),
                () -> RestAssured.expect().when().get("/path"),
                () -> given(new RequestSpecBuilder().build()).get("/path"),
                () -> RestAssured.get("/path"),
                () -> RestAssured.request(Method.GET, "/path"));

        for (Supplier<Response> request : requests) {
            request.get().then().body("b", equalTo(1));
        }

        assertThat(FakeHttpClients.sent).hasSize(requests.size()).allMatch(sent -> sent.uri().equals(BOUND));
    }

    @Test
    void built_specifications_take_the_settings_of_the_bound_client_and_none_of_the_static_ones() {
        List<String> used = new CopyOnWriteArrayList<>();
        RestAssured.config = FAKE.sessionConfig(sessionConfig().sessionIdName("STATIC"));
        RestAssured.filters((requestSpec, responseSpec, ctx) -> {
            used.add("static filter");
            return ctx.next(requestSpec, responseSpec);
        });
        RestAssured.requestSpecification = new RequestSpecBuilder().addHeader("X-Static", "1").build();
        RequestSpecification request = new RequestSpecBuilder().build();
        // The root path of the client
        ResponseSpecification response = new ResponseSpecBuilder().expectBody("b", equalTo(1)).build();
        RestAssured.useClient(client.toBuilder().filters((requestSpec, responseSpec, ctx) -> {
            used.add(requestSpec.getConfig().getSessionConfig().sessionIdName());
            return ctx.next(requestSpec, responseSpec);
        }).build());

        given(request, response).get("/path");
        given(request).get("/path").then().spec(response);

        assertThat(used).containsExactly(SessionConfig.DEFAULT_SESSION_ID_NAME, SessionConfig.DEFAULT_SESSION_ID_NAME);
        assertThat(FakeHttpClients.sent).allSatisfy(sent -> {
            assertThat(sent.uri()).isEqualTo(BOUND);
            assertThat(sent.headers()).noneMatch(header -> header.name().equals("X-Static"));
        });
    }

    @Test
    void reset_client_brings_back_the_static_settings() {
        RestAssured.useClient(client);
        RestAssured.resetClient();

        given().get("/path").then().body("b", equalTo(2));

        assertThat(FakeHttpClients.sent.get(0).uri()).isEqualTo(STATIC);
    }

    @Test
    void a_client_must_be_given_to_bind_one() {
        assertThatThrownBy(() -> RestAssured.useClient(null))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("RestAssuredClient cannot be null");
    }

    @Test
    void the_client_is_bound_to_the_current_thread_only() throws Exception {
        RestAssured.useClient(client);

        FutureTask<Void> otherThread = new FutureTask<>(() -> {
            given().get("/path");
            given(client).get("/path");
            return null;
        });
        new Thread(otherThread).start();
        otherThread.get();
        given().get("/path");

        assertThat(FakeHttpClients.sent).extracting(sent -> sent.uri()).containsExactly(STATIC, BOUND, BOUND);
    }

    @Test
    void a_named_client_wins_over_the_bound_one() {
        RestAssuredClient other = client.toBuilder().baseUri("http://other.example.org").build();
        RestAssured.useClient(client);

        given(other).get("/path");

        assertThat(FakeHttpClients.sent.get(0).uri()).isEqualTo(URI.create("http://other.example.org/path"));
    }

    @Test
    void reset_unbinds_the_client_of_the_current_thread_only() throws Exception {
        RestAssured.useClient(client);

        FutureTask<Void> otherThread = new FutureTask<>(RestAssured::reset, null);
        new Thread(otherThread).start();
        otherThread.get();
        given().get("/path");
        RestAssured.reset();
        RestAssured.config = FAKE;
        given().get("/path");

        assertThat(FakeHttpClients.sent).extracting(sent -> sent.uri()).containsExactly(BOUND, URI.create("http://localhost:8080/path"));
    }

    @Test
    void the_static_settings_are_read_and_copied_as_they_are_while_a_client_is_bound() {
        RestAssured.useClient(client);

        assertThat(RestAssured.config()).isSameAs(FAKE);
        given(RestAssured.client()).get("/path");

        assertThat(FakeHttpClients.sent.get(0).uri()).isEqualTo(STATIC);
    }

    private static HttpResponse json(String body) {
        byte[] bytes = body.getBytes(UTF_8);
        return new HttpResponse("HTTP/1.1", 200, "OK", List.of(new HttpHeader("Content-Type", "application/json")), bytes.length,
                new ByteArrayInputStream(bytes), null);
    }
}
