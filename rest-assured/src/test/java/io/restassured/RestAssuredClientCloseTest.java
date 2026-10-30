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
import io.restassured.internal.http.client.FakeHttpClients.FakeClient;
import io.restassured.response.Response;
import io.restassured.specification.RequestSender;
import io.restassured.specification.RequestSpecification;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.config.ConnectionConfig.connectionConfig;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The HTTP client instances that a {@link RestAssuredClient} owns, and what {@link RestAssuredClient#close()} does, with
 * fake clients: one that is shared by requests with the same settings, like the JDK client, and one that is created for
 * each request, like Apache HttpClient 5.
 */
class RestAssuredClientCloseTest {
    private static final String URL = "http://localhost/path";
    private static final RestAssuredConfig SHARED = config().httpClient(httpClientConfig().client(FakeHttpClients.Other.class));
    private static final RestAssuredConfig PER_REQUEST = config().httpClient(httpClientConfig().client(FakeHttpClients.Apache5.class));

    @AfterEach
    void reset() {
        FakeHttpClients.reset();
        RestAssured.reset();
    }

    @Test
    void a_client_keeps_one_per_request_client_for_each_setting_until_it_is_closed() {
        RestAssuredClient client = RestAssuredClient.builder().config(PER_REQUEST).build();

        given(client).get(URL).asString();
        given(client).get(URL).asString();
        RestAssured.useClient(client);
        given(new RequestSpecBuilder().setConfig(PER_REQUEST).build(), new ResponseSpecBuilder().build()).get(URL).asString();
        RestAssured.resetClient();
        given(client).config(withConnectTimeout(PER_REQUEST, 1)).get(URL).asString();

        assertThat(FakeHttpClients.created).hasSize(2).noneMatch(created -> created.closed);
        client.close();
        assertThat(FakeHttpClients.created).allMatch(created -> created.closes.get() == 1);
    }

    @Test
    void the_static_api_creates_a_per_request_client_for_each_request() {
        given().config(PER_REQUEST).get(URL).asString();
        given(new RequestSpecBuilder().setConfig(PER_REQUEST).build(), new ResponseSpecBuilder().build()).get(URL).asString();

        assertThat(FakeHttpClients.created).hasSize(2).allMatch(created -> created.closed);
    }

    @Test
    void each_client_has_its_own_instances() {
        RestAssuredClient first = RestAssuredClient.builder().config(SHARED).build();
        RestAssuredClient second = first.toBuilder().build();

        given(first).get(URL).asString();
        given(second).get(URL).asString();
        first.close();

        assertThat(FakeHttpClients.created).hasSize(2);
        assertThat(FakeHttpClients.created.get(0).closed).isTrue();
        assertThat(FakeHttpClients.created.get(1).closed).isFalse();
        given(second).get(URL).asString();
        assertThat(FakeHttpClients.created).hasSize(2);
    }

    @Test
    void an_instance_that_a_response_uses_is_closed_once_when_the_response_is_done() {
        RestAssuredClient client = RestAssuredClient.builder().config(SHARED).build();
        Response unread = given(client).get(URL);
        given(client).config(withConnectTimeout(SHARED, 1)).get(URL).asString();
        FakeClient used = FakeHttpClients.created.get(0);
        FakeClient unused = FakeHttpClients.created.get(1);

        client.close();
        assertThat(used.closed).isFalse();
        assertThat(unused.closes).hasValue(1);
        assertThat(unread.asString()).isEqualTo("ok");
        client.close();

        assertThat(used.closes).hasValue(1);
        assertThat(unused.closes).hasValue(1);
    }

    @Test
    void after_close_starting_a_request_and_sending_a_specification_that_the_client_created_fail() {
        RestAssuredClient client = RestAssuredClient.builder().config(SHARED).build();
        RequestSpecification created = given(client);
        RestAssured.useClient(client);
        RequestSender pair = given(new RequestSpecBuilder().build(), new ResponseSpecBuilder().build());
        RestAssured.resetClient();

        client.close();

        List<ThrowingCallable> calls = List.of(() -> given(client), () -> created.get(URL), () -> pair.get(URL));
        for (ThrowingCallable call : calls) {
            assertThatThrownBy(call).isExactlyInstanceOf(IllegalStateException.class).hasMessage("This RestAssuredClient is closed");
        }
        assertThat(FakeHttpClients.created).isEmpty();
    }

    @Test
    void a_closed_client_still_gives_its_settings_and_a_builder_for_a_new_client() {
        RestAssuredClient client = RestAssuredClient.builder().config(SHARED).basePath("/api").build();
        client.close();

        RestAssuredClient copy = client.toBuilder().build();
        given(copy).get("/path");

        assertThat(client.config()).isSameAs(SHARED);
        assertThat(client.filters()).isEmpty();
        assertThat(FakeHttpClients.sent.get(0).uri().getPath()).isEqualTo("/api/path");
    }

    @Test
    void a_specification_merged_into_one_of_the_static_api_is_sent_with_the_static_api() {
        RestAssuredClient client = RestAssuredClient.builder().config(SHARED).build();
        RequestSpecification fromClient = given(client).header("X-Client", "1");
        client.close();

        given().spec(fromClient).get(URL);

        assertThat(FakeHttpClients.sent.get(0).headers()).contains(new HttpHeader("X-Client", "1"));
    }

    @Test
    void closing_a_client_leaves_the_instances_of_the_static_api_alone() {
        // Settings that no other test uses, so that the static API creates its instance here
        RestAssuredConfig config = withConnectTimeout(SHARED, 123457);
        given().config(config).get(URL).asString();
        FakeClient global = FakeHttpClients.created.get(0);
        RestAssuredClient client = RestAssuredClient.builder().config(config).build();
        given(client).get(URL).asString();

        client.close();
        given().config(config).get(URL).asString();

        assertThat(global.closed).isFalse();
        assertThat(FakeHttpClients.created).hasSize(2);
        assertThat(FakeHttpClients.created.get(1).closed).isTrue();
    }

    @Test
    void closing_idle_connections_after_each_response_gives_each_request_its_own_instance() {
        RestAssuredClient client = RestAssuredClient.builder().config(PER_REQUEST.connectionConfig(connectionConfig().closeIdleConnectionsAfterEachResponse())).build();

        given(client).get(URL).asString();
        given(client).get(URL).asString();

        assertThat(FakeHttpClients.created).hasSize(2).allMatch(created -> created.closes.get() == 1);
    }

    @Test
    void kept_cookies_belong_to_the_client_that_kept_them() {
        FakeHttpClients.response = request -> request.uri().getPath().equals("/login")
                ? response("ok", new HttpHeader("Set-Cookie", "session=1"))
                : response(request.headers().stream().filter(header -> header.name().equals("Cookie")).map(HttpHeader::value).findFirst().orElse("none"));
        RestAssuredConfig keepCookies = SHARED.httpClient(SHARED.getHttpClientConfig().reuseHttpClientInstance().keepResponseCookies(true));
        RestAssured.config = keepCookies;
        RestAssuredClient client = RestAssuredClient.builder().config(keepCookies).build();
        RestAssuredClient other = client.toBuilder().build();

        given(client).get("http://localhost/login");

        assertThat(given(client).get("http://localhost/cookies").asString()).isEqualTo("session=1");
        assertThat(given(other).get("http://localhost/cookies").asString()).isEqualTo("none");
        assertThat(given().get("http://localhost/cookies").asString()).isEqualTo("none");
        given().get("http://localhost/login");
        assertThat(given(RestAssured.client()).get("http://localhost/cookies").asString()).isEqualTo("none");
        assertThat(given().get("http://localhost/cookies").asString()).isEqualTo("session=1");
    }

    private static RestAssuredConfig withConnectTimeout(RestAssuredConfig config, int seconds) {
        return config.httpClient(config.getHttpClientConfig().connectTimeout(Duration.ofSeconds(seconds)));
    }

    private static HttpResponse response(String body, HttpHeader... headers) {
        byte[] bytes = body.getBytes(UTF_8);
        List<HttpHeader> all = new ArrayList<>(List.of(headers));
        all.add(new HttpHeader("Content-Type", "text/plain"));
        return new HttpResponse("HTTP/1.1", 200, "OK", all, bytes.length, new ByteArrayInputStream(bytes), null);
    }
}
