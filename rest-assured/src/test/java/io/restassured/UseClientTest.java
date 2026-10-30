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
import io.restassured.http.Method;
import io.restassured.internal.http.client.FakeHttpClients;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.RestAssuredConfig.config;
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
        client.close();
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
    void given_with_a_request_and_a_response_specification_sends_them_with_the_bound_client() {
        RestAssured.useClient(client);

        // The root path of the client: the specifications are sent as they are, with its HTTP clients and then()
        given(new RequestSpecBuilder().build(), new ResponseSpecBuilder().build()).get("/path").then().body("b", equalTo(1));
    }

    @Test
    void reset_client_brings_back_the_static_settings_and_leaves_a_client_that_was_passed_in_open() {
        RestAssured.useClient(client);
        RestAssured.resetClient();

        given().get("/path").then().body("b", equalTo(2));
        given(client).get("/path");

        assertThat(FakeHttpClients.sent).extracting(sent -> sent.uri()).containsExactly(STATIC, BOUND);
    }

    @Test
    void a_client_must_be_given_to_bind_one() {
        assertThatThrownBy(() -> RestAssured.useClient((RestAssuredClient) null))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("RestAssuredClient cannot be null");
        assertThatThrownBy(() -> RestAssured.useClient((RestAssuredClient.Builder) null))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Builder cannot be null");
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
    void a_client_built_by_use_client_is_closed_when_it_is_unbound() {
        List<Runnable> unbinds = List.of(RestAssured::resetClient, RestAssured::reset, () -> RestAssured.useClient(client),
                () -> RestAssured.useClient(RestAssuredClient.builder().config(FAKE)));

        for (Runnable unbind : unbinds) {
            RestAssured.useClient(RestAssuredClient.builder().config(FAKE));
            RequestSpecification fromOwned = given();
            unbind.run();

            assertThatThrownBy(() -> fromOwned.get("/path")).isExactlyInstanceOf(IllegalStateException.class).hasMessage("This RestAssuredClient is closed");
        }
        // Rebinding a client that was passed in leaves it open
        RestAssured.useClient(client);
        RestAssured.useClient(RestAssuredClient.builder().config(FAKE));
        given(client).get("/path");

        assertThat(FakeHttpClients.sent).extracting(sent -> sent.uri()).containsExactly(BOUND);
    }

    @Test
    void the_static_settings_are_read_and_copied_as_they_are_while_a_client_is_bound() {
        RestAssured.useClient(client);

        assertThat(RestAssured.config()).isSameAs(FAKE);
        try (RestAssuredClient snapshot = RestAssured.client()) {
            given(snapshot).get("/path");
        }

        assertThat(FakeHttpClients.sent.get(0).uri()).isEqualTo(STATIC);
    }

    @Test
    void a_bound_client_that_is_closed_fails_each_request() {
        client.close();
        RestAssured.useClient(client);

        List<ThrowingCallable> calls = List.of(RestAssured::given, RestAssured::with, RestAssured::when, RestAssured::expect,
                () -> given(new RequestSpecBuilder().build()), () -> given(new RequestSpecBuilder().build(), new ResponseSpecBuilder().build()),
                () -> RestAssured.get("/path"), () -> RestAssured.request("GET"));
        for (ThrowingCallable call : calls) {
            assertThatThrownBy(call).isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("The RestAssuredClient bound to this thread with RestAssured.useClient(..) is closed");
        }
        assertThatThrownBy(() -> given(client)).isExactlyInstanceOf(IllegalStateException.class).hasMessage("This RestAssuredClient is closed");
        assertThat(FakeHttpClients.sent).isEmpty();
    }

    private static HttpResponse json(String body) {
        byte[] bytes = body.getBytes(UTF_8);
        return new HttpResponse("HTTP/1.1", 200, "OK", List.of(new HttpHeader("Content-Type", "application/json")), bytes.length,
                new ByteArrayInputStream(bytes), null);
    }
}
