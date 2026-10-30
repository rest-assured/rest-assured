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
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.http.ContentType;
import io.restassured.internal.http.client.FakeHttpClients;
import io.restassured.parsing.Parser;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static io.restassured.config.SessionConfig.sessionConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

/**
 * A specification built with {@link RequestSpecBuilder} or {@link ResponseSpecBuilder} gets the static settings when it's
 * used, once, and <code>given(req, resp)</code> merges like <code>given().spec(req).expect().spec(resp)</code>.
 */
class SpecBuildersAndStaticSettingsTest {
    private static final RestAssuredConfig FAKE = config().httpClient(httpClientConfig().client(FakeHttpClients.Other.class));

    private final List<String> sessionIdNames = new CopyOnWriteArrayList<>();
    private final Filter recordSessionIdName = (requestSpec, responseSpec, ctx) -> {
        sessionIdNames.add(requestSpec.getConfig().getSessionConfig().sessionIdName());
        return ctx.next(requestSpec, responseSpec);
    };

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
    void static_headers_and_cookies_are_sent_once_with_a_built_specification() {
        // Issue #1977
        RestAssured.requestSpecification = new RequestSpecBuilder().addHeader("X-Static", "1").addCookie("static", "1").build();
        RequestSpecification spec = new RequestSpecBuilder().addHeader("X-Spec", "1").build();

        given(spec).get("/path");
        given().spec(spec).get("/path");
        given(spec, new ResponseSpecBuilder().build()).get("/path");

        assertThat(FakeHttpClients.sent).hasSize(3).allSatisfy(request -> {
            assertThat(request.headers()).filteredOn(header -> !header.name().startsWith("Accept") && !header.name().startsWith("Content"))
                    .containsExactly(new HttpHeader("Cookie", "static=1"), new HttpHeader("X-Static", "1"), new HttpHeader("X-Spec", "1"));
        });
    }

    @Test
    void a_built_specification_takes_the_static_config_and_filters_when_it_is_used() {
        List<String> staticFilterRuns = new CopyOnWriteArrayList<>();
        RestAssured.config = FAKE.sessionConfig(sessionConfig().sessionIdName("BUILT"));
        RestAssured.filters((requestSpec, responseSpec, ctx) -> {
            staticFilterRuns.add("ran");
            return ctx.next(requestSpec, responseSpec);
        });
        RequestSpecification spec = new RequestSpecBuilder().addFilter(recordSessionIdName).build();
        RestAssured.config = FAKE.sessionConfig(sessionConfig().sessionIdName("USED"));
        RestAssured.replaceFiltersWith(List.of());

        given(spec).get("/path");

        assertThat(sessionIdNames).containsExactly("USED");
        assertThat(staticFilterRuns).isEmpty();
    }

    @Test
    void merging_a_response_specification_keeps_the_expectations_that_it_does_not_set() {
        ResponseSpecification contentType = new ResponseSpecBuilder().expectContentType(ContentType.TEXT).build();

        assertThatThrownBy(() -> expect().statusCode(201).spec(contentType).when().get("/path"))
                .isInstanceOf(AssertionError.class).hasMessageContaining("Expected status code <201> but was <200>");
        RestAssured.responseSpecification = new ResponseSpecBuilder().expectStatusCode(201).build();
        assertThatThrownBy(() -> expect().spec(contentType).when().get("/path"))
                .isInstanceOf(AssertionError.class).hasMessageContaining("Expected status code <201> but was <200>");
    }

    @Test
    void given_with_a_request_and_a_response_specification_applies_the_static_response_specification() {
        RestAssured.responseSpecification = new ResponseSpecBuilder().expectStatusCode(201).build();

        assertThatThrownBy(() -> given(new RequestSpecBuilder().build(), new ResponseSpecBuilder().build()).get("/path"))
                .isInstanceOf(AssertionError.class).hasMessageContaining("Expected status code <201> but was <200>");
    }

    @Test
    void a_built_response_specification_uses_the_static_parsers_and_root_path_when_it_is_used() {
        FakeHttpClients.response = request -> FakeHttpClients.ok("{\"a\": {\"b\": 1}, \"c\": {\"b\": 2}}");
        ResponseSpecification spec = new ResponseSpecBuilder().expectBody("b", equalTo(1)).build();
        ResponseSpecification specWithRootPath = new ResponseSpecBuilder().rootPath("c").expectBody("b", equalTo(2)).build();
        // The response is text/plain
        RestAssured.registerParser("text/plain", Parser.JSON);
        RestAssured.rootPath = "a";

        RestAssured.get("/path").then().spec(spec).spec(specWithRootPath);
        expect().spec(spec).spec(specWithRootPath).when().get("/path");
        given(new RequestSpecBuilder().build(), spec).get("/path");
        given(new RequestSpecBuilder().build(), specWithRootPath).get("/path");
    }
}
