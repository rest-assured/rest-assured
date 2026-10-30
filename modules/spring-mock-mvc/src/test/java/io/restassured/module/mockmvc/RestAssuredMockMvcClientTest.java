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

package io.restassured.module.mockmvc;

import io.restassured.RestAssured;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.module.mockmvc.http.BasePathController;
import io.restassured.module.mockmvc.http.GreetingController;
import io.restassured.module.mockmvc.http.PostController;
import io.restassured.module.mockmvc.internal.MockMvcRequestSpecificationImpl;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecBuilder;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.commons.io.output.WriterOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.FutureTask;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

/**
 * What each setting of {@link RestAssuredMockMvcClient.Builder} does to the requests of the client, and which settings a
 * request uses: a client named in {@link RestAssuredMockMvc#given(RestAssuredMockMvcClient)}, then the client that
 * {@link RestAssuredMockMvc#useClient(RestAssuredMockMvcClient)} bound to the thread, then the static settings.
 */
class RestAssuredMockMvcClientTest {
    private final List<String> run = new CopyOnWriteArrayList<>();
    private final RestAssuredMockMvcClient client = RestAssuredMockMvcClient.builder().standaloneSetup(new GreetingController())
            .requestSpecification(greet("bound")).build();

    @AfterEach
    void reset() {
        RestAssuredMockMvc.reset();
        RestAssured.reset();
    }

    // The settings of a client

    @Test
    void the_builder_starts_from_the_values_that_reset_sets() {
        RestAssuredMockMvcClient empty = RestAssuredMockMvcClient.builder().build();

        assertThat(empty.config().getLogConfig().isUserConfigured()).isFalse();
        assertThat(empty.resultHandlers()).isEmpty();
        assertThat(empty.postProcessors()).isEmpty();
        assertThatThrownBy(() -> given(empty).get("/greeting"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("RestAssuredMockMvcClient.builder().\n\t\tmockMvc(..)");
        assertThat(given(empty.toBuilder().standaloneSetup(new BasePathController()).build()).get("/").statusCode()).isEqualTo(200);
    }

    @Test
    void each_setting_reaches_the_request() {
        RestAssuredMockMvcClient client = RestAssuredMockMvcClient.builder().standaloneSetup(new BasePathController()).basePath("/my-path")
                .resultHandlers(recording("result handler")).postProcessors(recordingPostProcessor())
                .requestSpecification(new MockMvcRequestSpecBuilder().addHeader("X-Default", "1").build())
                .responseSpecification(new ResponseSpecBuilder().expectStatusCode(200).build())
                .authentication(RestAssuredMockMvc.principal((Principal) () -> "user"))
                .build();

        MockHttpServletRequest request = given(client).get("/greetingPath").mvcResult().getRequest();

        assertThat(request.getHeader("X-Default")).isEqualTo("1");
        assertThat(request.getUserPrincipal().getName()).isEqualTo("user");
        assertThat(run).containsExactly("post processor", "result handler");
        assertThatThrownBy(() -> given(client.toBuilder().responseSpecification(new ResponseSpecBuilder().expectStatusCode(201).build()).build())
                .get("/greetingPath")).isInstanceOf(AssertionError.class).hasMessageContaining("Expected status code <201> but was <200>");
    }

    @Test
    void each_way_to_set_up_the_mock_mvc_instance() {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new GreetingController()).build();

        assertThat(given(client.toBuilder().mockMvc(mockMvc).build()).get("/greeting").statusCode()).isEqualTo(200);
        assertThat(given(client.toBuilder().standaloneSetup(MockMvcBuilders.standaloneSetup(new GreetingController())).build())
                .get("/greeting").statusCode()).isEqualTo(200);
        // The last one wins
        assertThat(given(client.toBuilder().mockMvc(mockMvc).standaloneSetup(new PostController()).build()).get("/greeting").statusCode())
                .isEqualTo(404);
    }

    @Test
    void logging_if_validation_fails_is_enabled_in_the_config_and_in_the_request_specification_set_before() {
        StringWriter log = new StringWriter();
        MockMvcRequestSpecification specification = new MockMvcRequestSpecBuilder()
                .setConfig(config().logConfig(new LogConfig(new PrintStream(new WriterOutputStream(log, Charset.defaultCharset()), true), true))).build();
        RestAssuredMockMvcClient client = this.client.toBuilder().requestSpecification(specification)
                .enableLoggingOfRequestAndResponseIfValidationFails().build();

        assertThatThrownBy(() -> given(client).get("/greeting").then().statusCode(500)).isInstanceOf(AssertionError.class);

        assertThat(log.toString()).contains("Request method:\tGET");
        // The builder changed its own copy
        assertThat(((MockMvcRequestSpecificationImpl) specification).getRestAssuredMockMvcConfig().getLogConfig()
                .isLoggingOfRequestAndResponseIfValidationFailsEnabled()).isFalse();
    }

    @Test
    void the_lists_of_a_client_cannot_be_changed() {
        ResultHandler handler = recording("result handler");
        RestAssuredMockMvcClient client = RestAssuredMockMvcClient.builder().resultHandlers(handler).build();

        assertThat(client.resultHandlers()).containsExactly(handler);
        assertThatThrownBy(() -> client.resultHandlers().add(handler)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> client.postProcessors().add(recordingPostProcessor())).isInstanceOf(UnsupportedOperationException.class);
    }

    // Changes that don't reach a client

    @Test
    void the_specifications_given_to_the_builder_are_copied() {
        MockMvcRequestSpecification requestSpecification = greet("before");
        ResponseSpecification responseSpecification = new ResponseSpecBuilder().expectStatusCode(200).build();
        RestAssuredMockMvcClient client = this.client.toBuilder().requestSpecification(requestSpecification)
                .responseSpecification(responseSpecification).build();

        requestSpecification.header("X-After", "1").param("name", "after");
        responseSpecification.statusCode(500);
        MockMvcResponse response = given(client).get("/greeting");

        assertThat(content(response)).isEqualTo("Hello, before!");
        assertThat(response.mvcResult().getRequest().getHeader("X-After")).isNull();
    }

    @Test
    void a_client_from_the_static_fields_takes_their_values_once() {
        RestAssuredMockMvc.standaloneSetup(new GreetingController());
        RestAssuredMockMvc.resultHandlers(recording("static"));
        RestAssuredMockMvc.requestSpecification = greet("static");
        RestAssuredMockMvcClient client = RestAssuredMockMvc.client();

        RestAssuredMockMvc.requestSpecification.header("X-Later", "1");
        RestAssuredMockMvc.resultHandlers(recording("later"));
        RestAssuredMockMvc.reset();
        RestAssuredMockMvc.standaloneSetup(new PostController());
        MockMvcResponse response = given(client).get("/greeting");

        assertThat(content(response)).isEqualTo("Hello, static!");
        assertThat(response.mvcResult().getRequest().getHeader("X-Later")).isNull();
        assertThat(run).containsExactly("static");
    }

    @Test
    void a_client_from_to_builder_is_independent() {
        RestAssuredMockMvcClient.Builder builder = client.toBuilder();

        RestAssuredMockMvcClient second = builder.requestSpecification(greet("second")).resultHandlers(recording("second")).build();
        builder.requestSpecification(greet("third"));

        assertThat(content(given(client).get("/greeting"))).isEqualTo("Hello, bound!");
        assertThat(content(given(second).get("/greeting"))).isEqualTo("Hello, second!");
        assertThat(run).containsExactly("second");
    }

    @Test
    void a_built_specification_takes_the_mock_mvc_and_base_path_of_the_client() {
        RestAssuredMockMvc.standaloneSetup(new PostController());
        RestAssuredMockMvc.basePath = "/other";
        RestAssuredMockMvcClient client = RestAssuredMockMvcClient.builder().standaloneSetup(new BasePathController()).basePath("/my-path").build();

        assertThat(given(client).spec(new MockMvcRequestSpecBuilder().build()).get("/greetingPath").statusCode()).isEqualTo(200);
    }

    // then() doesn't read the static fields of RestAssured

    @Test
    void then_validates_without_the_static_root_path_and_response_specification_of_rest_assured() {
        RestAssured.rootPath = "unknown";
        RestAssured.responseSpecification = new ResponseSpecBuilder().expectStatusCode(500).build();
        RestAssuredMockMvc.standaloneSetup(new GreetingController());

        given(client).get("/greeting").then().body("content", equalTo("Hello, bound!"));
        // The static API reads them, as before
        assertThatThrownBy(() -> given().get("/greeting").then().body("content", equalTo("Hello, World!"))).isInstanceOf(AssertionError.class);
    }

    // Which client a request uses

    @Test
    void every_entry_point_and_send_shortcut_uses_the_bound_client() {
        RestAssuredMockMvc.standaloneSetup(new GreetingController());
        RestAssuredMockMvc.useClient(client);

        assertThat(content(given().get("/greeting"))).isEqualTo("Hello, bound!");
        assertThat(content(RestAssuredMockMvc.with().get("/greeting"))).isEqualTo("Hello, bound!");
        assertThat(content(RestAssuredMockMvc.when().get("/greeting"))).isEqualTo("Hello, bound!");
        assertThat(content(RestAssuredMockMvc.get("/greeting"))).isEqualTo("Hello, bound!");
    }

    @Test
    void a_named_client_wins_over_the_bound_one_which_wins_over_the_static_settings() {
        RestAssuredMockMvc.standaloneSetup(new GreetingController());
        RestAssuredMockMvc.requestSpecification = greet("static");
        assertThat(content(given().get("/greeting"))).isEqualTo("Hello, static!");

        RestAssuredMockMvc.useClient(client);

        assertThat(content(given().get("/greeting"))).isEqualTo("Hello, bound!");
        assertThat(content(given(client.toBuilder().requestSpecification(greet("named")).build()).get("/greeting"))).isEqualTo("Hello, named!");
    }

    @Test
    void the_client_is_bound_to_the_current_thread_only() throws Exception {
        RestAssuredMockMvc.standaloneSetup(new GreetingController());
        RestAssuredMockMvc.useClient(client);

        FutureTask<String> otherThread = new FutureTask<>(() -> content(given().get("/greeting")));
        new Thread(otherThread).start();

        assertThat(otherThread.get()).isEqualTo("Hello, World!");
        assertThat(content(given().get("/greeting"))).isEqualTo("Hello, bound!");
    }

    @Test
    void reset_client_and_reset_unbind_the_client() {
        RestAssuredMockMvc.useClient(client);
        RestAssuredMockMvc.resetClient();
        RestAssuredMockMvc.standaloneSetup(new GreetingController());
        assertThat(content(given().get("/greeting"))).isEqualTo("Hello, World!");

        RestAssuredMockMvc.useClient(client);
        RestAssuredMockMvc.reset();

        assertThatThrownBy(() -> given().get("/greeting")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void a_client_must_be_given() {
        assertThatThrownBy(() -> RestAssuredMockMvc.useClient(null))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("RestAssuredMockMvcClient cannot be null");
        assertThatThrownBy(() -> given((RestAssuredMockMvcClient) null))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("RestAssuredMockMvcClient cannot be null");
    }

    private static MockMvcRequestSpecification greet(String name) {
        return new MockMvcRequestSpecBuilder().addParam("name", name).build();
    }

    private static String content(MockMvcResponse response) {
        return response.path("content");
    }

    private ResultHandler recording(String name) {
        return result -> run.add(name);
    }

    private RequestPostProcessor recordingPostProcessor() {
        return request -> {
            run.add("post processor");
            return request;
        };
    }
}
