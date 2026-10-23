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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.restassured.authentication.OAuthSignature;
import io.restassured.config.RedirectConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.restassured.RestAssured.given;
import static io.restassured.config.RestAssuredConfig.config;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Where an OAuth 2 access token is sent for each {@link OAuthSignature}.
 */
class OAuth2SignatureTest {

    private final List<String> requests = new CopyOnWriteArrayList<>();
    private HttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/token", this::respond);
        server.createContext("/same-host", exchange -> redirect(exchange, url("localhost", "/token")));
        server.createContext("/other-host", exchange -> redirect(exchange, url("127.0.0.1", "/token")));
        server.start();
    }

    @AfterEach
    void stopServer() {
        RestAssured.reset();
        server.stop(0);
    }

    @Test
    void query_string_signature_adds_the_url_encoded_token_as_access_token_query_parameter() {
        given().auth().oauth2("a+b/c=", OAuthSignature.QUERY_STRING).queryParam("q", "a").when().get(url("localhost", "/token")).then().statusCode(200);

        assertThat(requests).containsExactly("localhost /token?q=a&access_token=a%2Bb%2Fc%3D Authorization=null");
    }

    @Test
    void query_string_signature_adds_the_token_to_a_redirect_to_the_same_host_only() {
        given().auth().oauth2("token", OAuthSignature.QUERY_STRING).when().get(url("localhost", "/same-host")).then().statusCode(200);
        given().auth().oauth2("token", OAuthSignature.QUERY_STRING).when().get(url("localhost", "/other-host")).then().statusCode(200);

        assertThat(requests).containsExactly(
                "localhost /same-host?access_token=token Authorization=null", "localhost /token?access_token=token Authorization=null",
                "localhost /other-host?access_token=token Authorization=null", "127.0.0.1 /token Authorization=null");
    }

    @Test
    void query_string_signature_adds_the_token_to_a_redirect_to_another_host_when_cross_host_stripping_is_disabled() {
        given().
                config(config().redirect(new RedirectConfig().stripSensitiveHeadersOnCrossHostRedirect(false))).
                auth().oauth2("token", OAuthSignature.QUERY_STRING).
        when().
                get(url("localhost", "/other-host")).
        then().
                statusCode(200);

        assertThat(requests).containsExactly("localhost /other-host?access_token=token Authorization=null", "127.0.0.1 /token?access_token=token Authorization=null");
    }

    @Test
    void header_signature_of_an_oauth2_scheme_sends_a_bearer_token() {
        RestAssured.authentication = RestAssured.oauth2("token", OAuthSignature.HEADER);

        given().when().get(url("localhost", "/token")).then().statusCode(200);

        assertThat(requests).containsExactly("localhost /token Authorization=Bearer token");
    }

    private String url(String host, String path) {
        return "http://" + host + ":" + server.getAddress().getPort() + path;
    }

    private void record(HttpExchange exchange) {
        String host = exchange.getRequestHeaders().getFirst("Host").replaceFirst(":\\d+$", "");
        requests.add(host + " " + exchange.getRequestURI() + " Authorization=" + exchange.getRequestHeaders().getFirst("Authorization"));
    }

    private void respond(HttpExchange exchange) throws IOException {
        record(exchange);
        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }

    private void redirect(HttpExchange exchange, String location) throws IOException {
        record(exchange);
        exchange.getResponseHeaders().add("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }
}
