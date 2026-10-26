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

import com.sun.net.httpserver.HttpServer;
import io.restassured.authentication.AuthenticationScheme;
import io.restassured.authentication.ContextAuthenticationScheme;
import io.restassured.builder.RequestSpecBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

/**
 * Requests authenticated through an {@link io.restassured.authentication.AuthenticationContext} are sent the same way as
 * with the built-in schemes.
 */
class ContextAuthenticationSchemeTest {

    private HttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        // Asks for Basic authentication, like a server that doesn't accept preemptive credentials
        server.createContext("/challenge", exchange -> {
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            if (authorization == null) {
                exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"test\"");
                exchange.sendResponseHeaders(401, -1);
                exchange.close();
                return;
            }
            respond(exchange, authorization);
        });
        server.createContext("/headers", exchange -> respond(exchange,
                exchange.getRequestHeaders().getFirst("Authorization") + " " + exchange.getRequestHeaders().getFirst("X-Api-Key")));
        server.start();
    }

    @AfterEach
    void stopServer() {
        RestAssured.reset();
        server.stop(0);
    }

    @Test
    void custom_scheme_can_set_a_header() {
        RestAssured.authentication = (ContextAuthenticationScheme) context -> context.header("X-Api-Key", "secret");

        given().when().get(url("/headers")).then().body(equalTo("null secret"));
    }

    @Test
    void plain_authentication_scheme_lambda_can_set_a_header_and_answer_a_basic_challenge() {
        AuthenticationScheme scheme = context -> {
            context.header("X-Api-Key", "secret");
            context.basic("user", "pass");
        };

        RestAssured.authentication = scheme;

        given().when().get(url("/headers")).then().body(equalTo("null secret"));
        given(new RequestSpecBuilder().setAuth(scheme).build()).when().get(url("/challenge")).then().statusCode(200).body(equalTo("Basic dXNlcjpwYXNz"));
    }

    @Test
    void custom_scheme_set_on_a_request_spec_builder_is_used() {
        ContextAuthenticationScheme scheme = context -> context.header("X-Api-Key", "secret");

        given(new RequestSpecBuilder().setAuth(scheme).build()).when().get(url("/headers")).then().body(equalTo("null secret"));
    }

    @Test
    void custom_scheme_can_answer_a_basic_challenge() {
        RestAssured.authentication = (ContextAuthenticationScheme) context -> context.basic("user", "pass");

        given().when().get(url("/challenge")).then().statusCode(200).body(equalTo("Basic dXNlcjpwYXNz"));
    }

    @Test
    void built_in_schemes_send_the_same_through_the_context() {
        assertSameThroughContext(RestAssured.basic("user", "pass"), "/challenge");
        assertSameThroughContext(RestAssured.preemptive().basic("user", "pass"), "/headers");
        assertSameThroughContext(RestAssured.oauth2("token"), "/headers");
    }

    @Test
    void oauth_scheme_signs_through_the_context() {
        AuthenticationScheme oauth = RestAssured.oauth("key", "secret", "access", "token");
        RestAssured.authentication = (ContextAuthenticationScheme) oauth::authenticate;

        given().when().get(url("/headers")).then().body(allOf(startsWith("OAuth "), containsString("oauth_consumer_key=\"key\""), containsString("oauth_token=\"access\"")));
    }

    private void assertSameThroughContext(AuthenticationScheme scheme, String path) {
        RestAssured.authentication = scheme;
        String direct = given().when().get(url(path)).then().statusCode(200).extract().asString();
        RestAssured.authentication = (ContextAuthenticationScheme) scheme::authenticate;

        given().when().get(url(path)).then().statusCode(200).body(equalTo(direct));
    }

    private String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
