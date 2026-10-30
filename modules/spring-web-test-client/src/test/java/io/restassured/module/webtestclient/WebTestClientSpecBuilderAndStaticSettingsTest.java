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

import io.restassured.config.LogConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.module.webtestclient.setup.BasePathController;
import io.restassured.module.webtestclient.setup.GreetingController;
import io.restassured.module.webtestclient.setup.MultiPartController;
import io.restassured.module.webtestclient.setup.PostController;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSpecBuilder;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.module.webtestclient.RestAssuredWebTestClient.given;
import static io.restassured.module.webtestclient.config.RestAssuredWebTestClientConfig.config;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A specification built with {@link WebTestClientRequestSpecBuilder} gets the static settings of
 * {@link RestAssuredWebTestClient} when it's used, once.
 */
class WebTestClientSpecBuilderAndStaticSettingsTest {
    private final List<ClientRequest> requests = new CopyOnWriteArrayList<>();
    private final ExchangeFilterFunction recordRequests = (request, next) -> {
        requests.add(request);
        return next.exchange(request);
    };
    private final ByteArrayOutputStream log = new ByteArrayOutputStream();
    private final LogConfig blacklistingSecret = logConfig().defaultStream(new PrintStream(log, true)).blacklistHeader("Secret");

    @AfterEach
    void reset() {
        RestAssuredWebTestClient.reset();
    }

    @Test
    void static_headers_and_cookies_are_sent_once_with_a_built_specification() {
        staticSettingsWithHeaders();
        WebTestClientRequestSpecification spec = new WebTestClientRequestSpecBuilder().addHeader("X-Spec", "1").build();

        given().spec(spec).get("/greeting/{name}", "John");

        assertThat(requests).singleElement().satisfies(request -> {
            assertThat(request.headers().get("X-Static")).containsExactly("1");
            assertThat(request.headers().get("X-Spec")).containsExactly("1");
            assertThat(request.cookies().get("static")).containsExactly("1");
        });
    }

    @Test
    void static_headers_and_cookies_are_sent_once_with_a_specification_made_by_given() {
        staticSettingsWithHeaders();

        given().spec(RestAssuredWebTestClient.with().header("X", "1")).get("/greeting/{name}", "John");

        assertThat(requests).singleElement().satisfies(request -> {
            assertThat(request.headers().get("X-Static")).containsExactly("1");
            assertThat(request.headers().get("X")).containsExactly("1");
            assertThat(request.cookies().get("static")).containsExactly("1");
        });
    }

    @Test
    void a_static_multipart_part_is_sent_once_with_a_specification_made_by_given() {
        RestAssuredWebTestClient.webTestClient(WebTestClient.bindToController(new MultiPartController()).build());
        RestAssuredWebTestClient.requestSpecification = new WebTestClientRequestSpecBuilder().addMultiPart("file", "file.txt", "content".getBytes(UTF_8)).build();

        String parts = given().spec(RestAssuredWebTestClient.with().header("X", "1")).post("/multipart").then().statusCode(200).extract().asString();

        assertThat(parts.lines()).singleElement().asString().startsWith("file | file.txt | ");
    }

    @Test
    void a_built_specification_logs_with_the_static_log_config() {
        RestAssuredWebTestClient.standaloneSetup(new GreetingController());
        RestAssuredWebTestClient.config = config().logConfig(blacklistingSecret);
        WebTestClientRequestSpecification specification = new WebTestClientRequestSpecBuilder().log(LogDetail.HEADERS).build();

        given().spec(specification).header("Secret", "password").get("/greeting/{name}", "John");

        assertThat(log.toString()).contains("Secret=[ BLACKLISTED ]").doesNotContain("password");
    }

    @Test
    void a_built_specification_logs_with_the_log_config_of_the_client() {
        RestAssuredWebTestClientClient client = RestAssuredWebTestClientClient.builder().standaloneSetup(new GreetingController())
                .config(config().logConfig(blacklistingSecret)).build();
        WebTestClientRequestSpecification specification = new WebTestClientRequestSpecBuilder().log(LogDetail.HEADERS).build();

        given(client).spec(specification).header("Secret", "password").get("/greeting/{name}", "John");

        assertThat(log.toString()).contains("Secret=[ BLACKLISTED ]").doesNotContain("password");
    }

    @Test
    void log_takes_a_log_config_set_after_it() {
        RestAssuredWebTestClient.standaloneSetup(new GreetingController());

        given().log().headers().config(config().logConfig(blacklistingSecret)).header("Secret", "password").get("/greeting/{name}", "John");

        assertThat(log.toString()).contains("Secret=[ BLACKLISTED ]").doesNotContain("password");
    }

    @Test
    void a_built_specification_takes_the_web_test_client_and_base_path_of_the_static_settings_when_it_is_used() {
        RestAssuredWebTestClient.standaloneSetup(new PostController());
        RestAssuredWebTestClient.basePath = "/other";
        WebTestClientRequestSpecification spec = new WebTestClientRequestSpecBuilder().build();
        RestAssuredWebTestClient.standaloneSetup(new BasePathController());
        RestAssuredWebTestClient.basePath = "/my-path";

        assertThat(given().spec(spec).get("/greetingPath").statusCode()).isEqualTo(200);
    }

    @Test
    void a_built_specification_sent_on_its_own_needs_the_web_test_client_on_the_builder() {
        RestAssuredWebTestClient.standaloneSetup(new GreetingController());

        assertThatThrownBy(() -> new WebTestClientRequestSpecBuilder().build().get("/greeting/{name}", "John"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("new WebTestClientRequestSpecBuilder().\n\t\tsetWebTestClient(..)");
        assertThat(new WebTestClientRequestSpecBuilder().setWebTestClient(WebTestClient.bindToController(new GreetingController()).build()).build()
                .get("/greeting/{name}", "John").statusCode()).isEqualTo(200);
    }

    private void staticSettingsWithHeaders() {
        RestAssuredWebTestClient.standaloneSetup(new GreetingController(), recordRequests);
        RestAssuredWebTestClient.requestSpecification = new WebTestClientRequestSpecBuilder().addHeader("X-Static", "1").addCookie("static", "1").build();
    }
}
