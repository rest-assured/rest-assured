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

package io.restassured.internal.filter;

import io.restassured.RestAssured;
import io.restassured.RestAssuredClient;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.CsrfConfig;
import io.restassured.config.LogConfig;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.session.SessionFilter;
import io.restassured.internal.filter.RecordingServer.Reply;
import io.restassured.internal.filter.RecordingServer.Request;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static io.restassured.config.CsrfConfig.csrfConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the behavior of {@link CsrfFilter} (including the named-argument construction in
 * {@code RequestSpecificationImpl}) against a real HTTP server.
 */
class CsrfFilterTest {

    private static final String FORM_PAGE = "<html><head><title>Csrf</title></head><body>" +
            "<form action=\"x\" method=\"POST\"><input type=\"hidden\" name=\"_csrf\" value=\"tok\"/></form></body></html>";
    private static final String META_PAGE = "<html><head><title>Csrf</title><meta name=\"_csrf_header\" content=\"htok\"/></head>" +
            "<body><p>hello</p></body></html>";

    private RecordingServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new RecordingServer();
        RestAssured.baseURI = "http://127.0.0.1";
        RestAssured.port = server.port();
        server.route("GET /csrf-page", r -> Reply.html(FORM_PAGE).withCookie("PAGE=p1"));
        server.route("GET /meta-page", r -> Reply.html(META_PAGE));
        for (String method : new String[]{"GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"}) {
            server.route(method + " /resource", r -> Reply.text("done"));
        }
    }

    @AfterEach
    void stopServer() {
        server.close();
        RestAssured.reset();
    }

    @Test
    void post_fetches_token_page_and_sends_token_as_form_param() {
        given().csrf("/csrf-page").cookie("mine", "m1").formParam("a", "b").when().post("/resource").then().statusCode(200);

        assertThat(server.requestLines()).containsExactly("GET /csrf-page", "POST /resource");
        assertThat(server.lastRequestTo("GET /csrf-page").cookies()).isEqualTo("mine=m1");
        Request post = server.lastRequestTo("POST /resource");
        assertThat(post.body()).isEqualTo("a=b&_csrf=tok");
        assertThat(post.cookies()).contains("mine=m1").contains("PAGE=p1");
        assertThat(post.header("X-CSRF-TOKEN")).isNull();
    }

    @Test
    void sends_token_from_meta_tag_as_header() {
        given().csrf("/meta-page").when().post("/resource").then().statusCode(200);

        assertThat(server.requestLines()).containsExactly("GET /meta-page", "POST /resource");
        Request post = server.lastRequestTo("POST /resource");
        assertThat(post.header("X-CSRF-TOKEN")).isEqualTo("htok");
        assertThat(post.body()).isEmpty();
    }

    @Test
    void sends_token_as_custom_header_name() {
        given().config(config().csrfConfig(csrfConfig().csrfTokenPath("/meta-page").csrfHeaderName("X-Mine"))).
                when().put("/resource").then().statusCode(200);

        assertThat(server.lastRequestTo("PUT /resource").header("X-Mine")).isEqualTo("htok");
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD"})
    void does_not_fetch_token_for_safe_methods(String method) {
        given().csrf("/csrf-page").when().request(method, "/resource").then().statusCode(200);

        assertThat(server.requestLines()).containsExactly(method + " /resource");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUT", "PATCH", "DELETE", "OPTIONS"})
    void fetches_token_for_other_methods(String method) {
        given().csrf("/meta-page").when().request(method, "/resource").then().statusCode(200);

        assertThat(server.requestLines()).containsExactly("GET /meta-page", method + " /resource");
        assertThat(server.lastRequestTo(method + " /resource").header("X-CSRF-TOKEN")).isEqualTo("htok");
    }

    @Test
    void does_not_apply_token_page_cookies_when_configured_not_to() {
        given().config(config().csrfConfig(csrfConfig().csrfTokenPath("/csrf-page").automaticallyApplyCookies(false))).
                when().post("/resource").then().statusCode(200);

        assertThat(server.lastRequestTo("POST /resource").cookies()).isNull();
        assertThat(server.lastRequestTo("POST /resource").body()).isEqualTo("_csrf=tok");
    }

    @Test
    void stores_token_page_cookies_in_cookie_filter_and_session_in_session_filter() {
        server.route("GET /csrf-page", r -> Reply.html(FORM_PAGE).withCookie("JSESSIONID=sid").withCookie("other=o1"));
        CookieFilter cookieFilter = new CookieFilter();
        SessionFilter sessionFilter = new SessionFilter();

        given().csrf("/csrf-page").filter(cookieFilter).filter(sessionFilter).when().post("/resource").then().statusCode(200);
        assertThat(sessionFilter.getSessionId()).isEqualTo("sid");

        // The cookie filter now sends the stored cookies on a request without csrf
        given().filter(cookieFilter).when().get("/resource").then().statusCode(200);
        assertThat(server.lastRequestTo("GET /resource").cookies()).contains("JSESSIONID=sid").contains("other=o1");
    }

    @Test
    void throws_illegal_argument_exception_when_token_is_missing() {
        server.route("GET /empty-page", r -> Reply.html("<html><body><p>nothing</p></body></html>"));

        assertThatThrownBy(() -> given().csrf("/empty-page").when().post("/resource"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Couldn't find a the CSRF token in response. Expecting either an input field with name \"_csrf\" " +
                        "or a meta tag with name \"_csrf_header\". Response was:\n")
                .hasMessageContaining("<p>nothing</p>");
        assertThat(server.requestLines()).containsExactly("GET /empty-page");
    }

    @Test
    void fetches_token_page_with_the_defaults_of_the_client_bound_to_the_thread() {
        RestAssured.requestSpecification = withHeader("static");
        RestAssured.useClient(clientWithHeader("bound"));

        given().csrf("/csrf-page").when().post("/resource").then().statusCode(200);

        assertThat(server.requestLines()).containsExactly("GET /csrf-page", "POST /resource");
        assertThat(server.requests).extracting(r -> r.header("X-Defaults")).containsOnly("bound");
    }

    @Test
    void fetches_token_page_with_the_defaults_of_the_client_named_in_given() {
        RestAssured.requestSpecification = withHeader("static");
        RestAssured.useClient(clientWithHeader("bound"));

        given(clientWithHeader("named")).csrf("/csrf-page").when().post("/resource").then().statusCode(200);

        assertThat(server.requestLines()).containsExactly("GET /csrf-page", "POST /resource");
        assertThat(server.requests).extracting(r -> r.header("X-Defaults")).containsOnly("named");
    }

    @Test
    void fetches_token_page_through_the_proxy_of_the_request() throws IOException {
        try (RecordingServer proxy = new RecordingServer()) {
            proxy.route("GET /csrf-page", r -> Reply.html(FORM_PAGE));
            proxy.route("POST /resource", r -> Reply.text("done"));

            given().proxy("127.0.0.1", proxy.port()).csrf("/csrf-page").when().post("/resource").then().statusCode(200);

            assertThat(proxy.requestLines()).containsExactly("GET /csrf-page", "POST /resource");
            assertThat(server.requestLines()).isEmpty();
        }
    }

    @Test
    void fetches_token_page_from_the_base_uri_of_the_request() throws IOException {
        try (RecordingServer other = new RecordingServer()) {
            RestAssured.port = other.port();
            server.route("GET /api/csrf-page", r -> Reply.html(FORM_PAGE));
            server.route("POST /api/resource", r -> Reply.text("done"));

            given().baseUri("http://127.0.0.1:" + server.port() + "/api").csrf("/csrf-page").when().post("/resource").then().statusCode(200);

            assertThat(server.requestLines()).containsExactly("GET /api/csrf-page", "POST /api/resource");
            assertThat(other.requests).isEmpty();
        }
    }

    @Test
    void filter_used_directly_with_disabled_csrf_config_passes_request_through() {
        CsrfFilter filter = new CsrfFilter();
        CsrfConfig csrfConfig = new CsrfConfig();
        filter.setCsrfConfig(csrfConfig);

        given().filter(filter).when().post("/resource").then().statusCode(200);

        assertThat(filter.getCsrfConfig()).isSameAs(csrfConfig);
        assertThat(server.requestLines()).containsExactly("POST /resource");
    }

    @Test
    void logs_only_status_of_token_request_when_log_detail_is_status() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        given().config(config().csrfConfig(csrfConfig().csrfTokenPath("/csrf-page").loggingEnabled(LogDetail.STATUS, logTo(out)))).
                when().post("/resource").then().statusCode(200);

        assertThat(out.toString(StandardCharsets.UTF_8)).isEqualTo(String.format("HTTP/1.1 200 OK%n"));
    }

    @Test
    void logs_only_request_of_token_request_when_log_detail_is_params() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        given().config(config().csrfConfig(csrfConfig().csrfTokenPath("/csrf-page").loggingEnabled(LogDetail.PARAMS, logTo(out)))).
                when().post("/resource").then().statusCode(200);

        assertThat(out.toString(StandardCharsets.UTF_8)).startsWith("Request params:").doesNotContain("HTTP/1.1");
    }

    @Test
    void logs_request_and_response_of_token_request_when_log_detail_is_all() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        given().config(config().csrfConfig(csrfConfig().csrfTokenPath("/csrf-page").loggingEnabled(LogDetail.ALL, logTo(out)))).
                when().post("/resource").then().statusCode(200);

        String log = out.toString(StandardCharsets.UTF_8);
        assertThat(log).startsWith(String.format("Request method:\tGET%nRequest URI:\thttp://127.0.0.1:%d/csrf-page%n", server.port()));
        assertThat(log).contains("HTTP/1.1 200 OK").contains("value=\"tok\"");
    }

    private RestAssuredClient clientWithHeader(String value) {
        return RestAssuredClient.builder().baseUri("http://127.0.0.1").port(server.port()).requestSpecification(withHeader(value)).build();
    }

    private static RequestSpecification withHeader(String value) {
        return new RequestSpecBuilder().addHeader("X-Defaults", value).build();
    }

    private static LogConfig logTo(ByteArrayOutputStream out) {
        return new LogConfig(new PrintStream(out, true, StandardCharsets.UTF_8), true);
    }
}
