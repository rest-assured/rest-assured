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

package io.restassured.module.webtestclient;

import io.restassured.RestAssured;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.module.webtestclient.internal.WebTestClientRequestSpecificationImpl;
import io.restassured.module.webtestclient.response.WebTestClientResponse;
import io.restassured.module.webtestclient.setup.BasePathController;
import io.restassured.module.webtestclient.setup.GreetingController;
import io.restassured.module.webtestclient.setup.PostController;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSpecBuilder;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.commons.io.output.WriterOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.FutureTask;

import static io.restassured.module.webtestclient.RestAssuredWebTestClient.given;
import static io.restassured.module.webtestclient.config.RestAssuredWebTestClientConfig.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

/**
 * What each setting of {@link RestAssuredWebTestClientClient.Builder} does to the requests of the client, and which
 * settings a request uses: a client named in {@link RestAssuredWebTestClient#given(RestAssuredWebTestClientClient)},
 * then the client that {@link RestAssuredWebTestClient#useClient(RestAssuredWebTestClientClient)} bound to the thread,
 * then the static settings.
 */
class RestAssuredWebTestClientClientTest {
    private final List<ClientRequest> requests = new CopyOnWriteArrayList<>();
    private final ExchangeFilterFunction recordRequests = (request, next) -> {
        requests.add(request);
        return next.exchange(request);
    };
    private final RestAssuredWebTestClientClient client = RestAssuredWebTestClientClient.builder().standaloneSetup(new GreetingController())
            .requestSpecification(greet("bound")).build();

    @AfterEach
    void reset() {
        RestAssuredWebTestClient.reset();
        RestAssured.reset();
    }

    // The settings of a client

    @Test
    void the_builder_starts_from_the_values_that_reset_sets() {
        RestAssuredWebTestClientClient empty = RestAssuredWebTestClientClient.builder().build();

        assertThat(empty.config().getLogConfig().isUserConfigured()).isFalse();
        assertThatThrownBy(() -> given(empty).get("/greeting"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("RestAssuredWebTestClientClient.builder().\n\t\twebTestClient(..)");
        assertThat(given(empty.toBuilder().standaloneSetup(new BasePathController()).build()).get("/").statusCode()).isEqualTo(200);
    }

    @Test
    void each_setting_reaches_the_request() {
        RestAssuredWebTestClientClient client = RestAssuredWebTestClientClient.builder().standaloneSetup(new BasePathController(), recordRequests)
                .basePath("/my-path")
                .requestSpecification(new WebTestClientRequestSpecBuilder().addHeader("X-Default", "1").build())
                .responseSpecification(new ResponseSpecBuilder().expectStatusCode(200).build())
                .build();

        given(client).get("/greetingPath");

        assertThat(requests).singleElement().satisfies(request -> assertThat(request.headers().get("X-Default")).containsExactly("1"));
        assertThatThrownBy(() -> given(client.toBuilder().responseSpecification(new ResponseSpecBuilder().expectStatusCode(201).build()).build())
                .get("/greetingPath")).isInstanceOf(AssertionError.class).hasMessageContaining("Expected status code <201> but was <200>");
    }

    @Test
    void each_way_to_set_up_the_web_test_client() {
        WebTestClient webTestClient = WebTestClient.bindToController(new GreetingController()).build();

        assertThat(given(client.toBuilder().webTestClient(webTestClient).build()).get("/greeting").statusCode()).isEqualTo(200);
        assertThat(given(client.toBuilder().standaloneSetup(WebTestClient.bindToController(new GreetingController()).configureClient()).build())
                .get("/greeting").statusCode()).isEqualTo(200);
        // The last one wins
        assertThat(given(client.toBuilder().webTestClient(webTestClient).standaloneSetup(new PostController()).build()).get("/greeting").statusCode())
                .isEqualTo(404);
    }

    @Test
    void logging_if_validation_fails_is_enabled_in_the_config_and_in_the_request_specification_set_before() {
        StringWriter log = new StringWriter();
        WebTestClientRequestSpecification specification = new WebTestClientRequestSpecBuilder()
                .setConfig(config().logConfig(new LogConfig(new PrintStream(new WriterOutputStream(log, Charset.defaultCharset()), true), true))).build();
        RestAssuredWebTestClientClient client = this.client.toBuilder().requestSpecification(specification)
                .enableLoggingOfRequestAndResponseIfValidationFails().build();

        assertThatThrownBy(() -> given(client).get("/greeting").then().statusCode(500)).isInstanceOf(AssertionError.class);

        assertThat(log.toString()).contains("Request method:\tGET");
        // The builder changed its own copy
        assertThat(((WebTestClientRequestSpecificationImpl) specification).getRestAssuredWebTestClientConfig().getLogConfig()
                .isLoggingOfRequestAndResponseIfValidationFailsEnabled()).isFalse();
    }

    // Changes that don't reach a client

    @Test
    void the_specifications_given_to_the_builder_are_copied() {
        WebTestClientRequestSpecification requestSpecification = greet("before");
        ResponseSpecification responseSpecification = new ResponseSpecBuilder().expectStatusCode(200).build();
        RestAssuredWebTestClientClient client = this.client.toBuilder().requestSpecification(requestSpecification)
                .responseSpecification(responseSpecification).build();

        requestSpecification.queryParam("name", "after");
        responseSpecification.statusCode(500);

        assertThat(content(given(client).get("/greeting"))).isEqualTo("Hello, before!");
    }

    @Test
    void a_client_from_the_static_fields_takes_their_values_once() {
        RestAssuredWebTestClient.standaloneSetup(new GreetingController());
        RestAssuredWebTestClient.requestSpecification = greet("static");
        RestAssuredWebTestClientClient client = RestAssuredWebTestClient.client();

        RestAssuredWebTestClient.requestSpecification.queryParam("name", "later");
        RestAssuredWebTestClient.reset();
        RestAssuredWebTestClient.standaloneSetup(new PostController());

        assertThat(content(given(client).get("/greeting"))).isEqualTo("Hello, static!");
    }

    @Test
    void a_client_from_to_builder_is_independent() {
        RestAssuredWebTestClientClient.Builder builder = client.toBuilder();

        RestAssuredWebTestClientClient second = builder.requestSpecification(greet("second")).build();
        builder.requestSpecification(greet("third"));

        assertThat(content(given(client).get("/greeting"))).isEqualTo("Hello, bound!");
        assertThat(content(given(second).get("/greeting"))).isEqualTo("Hello, second!");
    }

    @Test
    void a_built_specification_takes_the_web_test_client_and_base_path_of_the_client() {
        RestAssuredWebTestClient.standaloneSetup(new PostController());
        RestAssuredWebTestClient.basePath = "/other";
        RestAssuredWebTestClientClient client = RestAssuredWebTestClientClient.builder().standaloneSetup(new BasePathController()).basePath("/my-path").build();

        assertThat(given(client).spec(new WebTestClientRequestSpecBuilder().build()).get("/greetingPath").statusCode()).isEqualTo(200);
    }

    // then() doesn't read the static fields of RestAssured

    @Test
    void then_validates_without_the_static_root_path_and_response_specification_of_rest_assured() {
        RestAssured.rootPath = "unknown";
        RestAssured.responseSpecification = new ResponseSpecBuilder().expectStatusCode(500).build();
        RestAssuredWebTestClient.standaloneSetup(new GreetingController());

        given(client).get("/greeting").then().body("content", equalTo("Hello, bound!"));
        // The static API reads them, as before
        assertThatThrownBy(() -> given().get("/greeting").then().body("content", equalTo("Hello, World!"))).isInstanceOf(AssertionError.class);
    }

    // Which client a request uses

    @Test
    void every_entry_point_and_send_shortcut_uses_the_bound_client() {
        RestAssuredWebTestClient.standaloneSetup(new GreetingController());
        RestAssuredWebTestClient.useClient(client);

        assertThat(content(given().get("/greeting"))).isEqualTo("Hello, bound!");
        assertThat(content(RestAssuredWebTestClient.with().get("/greeting"))).isEqualTo("Hello, bound!");
        assertThat(content(RestAssuredWebTestClient.get("/greeting"))).isEqualTo("Hello, bound!");
    }

    @Test
    void a_named_client_wins_over_the_bound_one_which_wins_over_the_static_settings() {
        RestAssuredWebTestClient.standaloneSetup(new GreetingController());
        RestAssuredWebTestClient.requestSpecification = greet("static");
        assertThat(content(given().get("/greeting"))).isEqualTo("Hello, static!");

        RestAssuredWebTestClient.useClient(client);

        assertThat(content(given().get("/greeting"))).isEqualTo("Hello, bound!");
        assertThat(content(given(client.toBuilder().requestSpecification(greet("named")).build()).get("/greeting"))).isEqualTo("Hello, named!");
    }

    @Test
    void the_client_is_bound_to_the_current_thread_only() throws Exception {
        RestAssuredWebTestClient.standaloneSetup(new GreetingController());
        RestAssuredWebTestClient.useClient(client);

        FutureTask<String> otherThread = new FutureTask<>(() -> content(given().get("/greeting")));
        new Thread(otherThread).start();

        assertThat(otherThread.get()).isEqualTo("Hello, World!");
        assertThat(content(given().get("/greeting"))).isEqualTo("Hello, bound!");
    }

    @Test
    void reset_client_and_reset_unbind_the_client() {
        RestAssuredWebTestClient.useClient(client);
        RestAssuredWebTestClient.resetClient();
        RestAssuredWebTestClient.standaloneSetup(new GreetingController());
        assertThat(content(given().get("/greeting"))).isEqualTo("Hello, World!");

        RestAssuredWebTestClient.useClient(client);
        RestAssuredWebTestClient.reset();

        assertThatThrownBy(() -> given().get("/greeting")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void a_client_must_be_given() {
        assertThatThrownBy(() -> RestAssuredWebTestClient.useClient(null))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("RestAssuredWebTestClientClient cannot be null");
        assertThatThrownBy(() -> given((RestAssuredWebTestClientClient) null))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("RestAssuredWebTestClientClient cannot be null");
    }

    private static WebTestClientRequestSpecification greet(String name) {
        return new WebTestClientRequestSpecBuilder().addQueryParam("name", name).build();
    }

    private static String content(WebTestClientResponse response) {
        return response.path("content");
    }
}
