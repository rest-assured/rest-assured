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

import io.restassured.module.webtestclient.setup.BasePathController;
import io.restassured.module.webtestclient.setup.GreetingController;
import io.restassured.module.webtestclient.setup.PostController;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSpecBuilder;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.restassured.module.webtestclient.RestAssuredWebTestClient.given;
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
