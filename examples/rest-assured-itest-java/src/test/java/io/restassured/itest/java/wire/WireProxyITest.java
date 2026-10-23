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

package io.restassured.itest.java.wire;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.specification.ProxySpecification.host;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

/**
 * {@link WireServer} acts as the proxy here: it records the proxied request with its absolute target and answers it
 * itself. The targets are {@code .invalid} hosts, which can't be resolved, so a request that bypassed the proxy fails.
 */
public class WireProxyITest extends WireTest {
    private static final String TARGET = "http://target.invalid/path";
    private static final String PROXY_LOGIN_CHALLENGE = "Basic realm=\"proxy\"";
    // The JDK's default proxy selector also falls back to proxyHost and socksProxyHost
    private static final List<String> PROXY_PROPERTIES = List.of("http.proxyHost", "http.proxyPort", "http.nonProxyHosts", "https.proxyHost", "https.proxyPort",
            "proxyHost", "proxyPort", "socksProxyHost", "socksProxyPort");

    // Proxy configured in REST Assured

    @Test public void
    proxy_with_host_and_port_sends_the_absolute_uri_to_the_proxy() {
        given().
                proxy("localhost", server.port()).
                queryParam("q", "a b").
        when().
                get(TARGET).
        then().
                statusCode(200).
                body(equalTo("ok"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.requestLine()).isEqualTo("GET http://target.invalid/path?q=a%20b HTTP/1.1");
        assertThat(request.headers("Host")).containsExactly("target.invalid");
        assertThat(request.hasHeader("Proxy-Authorization")).isFalse();
    }

    @Test public void
    proxy_keeps_the_port_of_the_target_in_the_request_target_and_host_header() {
        given().
                proxy("localhost", server.port()).
        when().
                get("http://target.invalid:8081/path").
        then().
                statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.target()).isEqualTo("http://target.invalid:8081/path");
        assertThat(request.headers("Host")).containsExactly("target.invalid:8081");
    }

    @Test public void
    proxy_sends_the_body_and_content_type_unchanged() {
        given().
                proxy("localhost", server.port()).
                contentType("application/json").
                body("{\"a\":1}").
        when().
                post(TARGET).
        then().
                statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.requestLine()).isEqualTo("POST http://target.invalid/path HTTP/1.1");
        assertThat(request.header("Content-Type")).isEqualTo("application/json");
        assertThat(request.bodyAsString(UTF_8)).isEqualTo("{\"a\":1}");
    }

    @Test public void
    proxy_with_only_a_port_uses_127_0_0_1() {
        given().
                proxy(server.port()).
        when().
                get(TARGET).
        then().
                statusCode(200);

        assertThat(server.onlyRequest().target()).isEqualTo(TARGET);
    }

    @Test public void
    proxy_given_as_a_uri() {
        given().
                proxy(URI.create("http://localhost:" + server.port())).
        when().
                get(TARGET).
        then().
                statusCode(200);

        assertThat(server.onlyRequest().target()).isEqualTo(TARGET);
    }

    @Test public void
    proxy_given_as_a_uri_string() {
        given().
                proxy("http://localhost:" + server.port()).
        when().
                get(TARGET).
        then().
                statusCode(200);

        assertThat(server.onlyRequest().target()).isEqualTo(TARGET);
    }

    @Test public void
    proxy_given_as_host_port_and_scheme() {
        given().
                proxy("localhost", server.port(), "http").
        when().
                get(TARGET).
        then().
                statusCode(200);

        assertThat(server.onlyRequest().target()).isEqualTo(TARGET);
    }

    @Test public void
    proxy_given_as_a_proxy_specification() {
        given().
                proxy(host("localhost").withPort(server.port()).withScheme("http")).
        when().
                get(TARGET).
        then().
                statusCode(200);

        assertThat(server.onlyRequest().target()).isEqualTo(TARGET);
    }

    @Test public void
    proxy_given_in_a_request_spec_builder() {
        given().
                spec(new RequestSpecBuilder().setProxy("localhost", server.port()).build()).
        when().
                get(TARGET).
        then().
                statusCode(200);

        assertThat(server.onlyRequest().target()).isEqualTo(TARGET);
    }

    @Test public void
    proxy_set_statically_as_a_field() {
        RestAssured.proxy = host("localhost").withPort(server.port());

        given().
        when().
                get(TARGET).
        then().
                statusCode(200);

        assertThat(server.onlyRequest().target()).isEqualTo(TARGET);
    }

    @Test public void
    proxy_set_statically_with_host_and_port() {
        RestAssured.proxy("localhost", server.port());

        given().
        when().
                get(TARGET).
        then().
                statusCode(200);

        assertThat(server.onlyRequest().target()).isEqualTo(TARGET);
    }

    @Test public void
    proxy_with_a_base_uri_without_a_port_targets_the_default_http_port() {
        RestAssured.baseURI = "http://target.invalid";

        given().
                proxy("localhost", server.port()).
        when().
                get("/path").
        then().
                statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.target()).isEqualTo(TARGET);
        assertThat(request.headers("Host")).containsExactly("target.invalid");
    }

    @Test public void
    proxy_is_also_used_for_localhost() {
        given().
                proxy(server.port()).
        when().
                get(server.url("/path")).
        then().
                statusCode(200);

        assertThat(server.onlyRequest().target()).isEqualTo(server.url("/path"));
    }

    // Proxy login

    @Test public void
    proxy_login_sends_the_credentials_only_after_the_407_challenge() {
        server.respond("/path", proxyRequiringLogin("admin", "secret", Responder.ok()));

        given().
                proxy(host("localhost").withPort(server.port()).withAuth("admin", "secret")).
        when().
                get(TARGET).
        then().
                statusCode(200).
                body(equalTo("ok"));

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).hasSize(2);
        assertThat(requests).extracting(RecordedRequest::target).containsOnly(TARGET);
        assertThat(requests.get(0).hasHeader("Proxy-Authorization")).isFalse();
        assertThat(requests.get(1).headers("Proxy-Authorization")).containsExactly(basic("admin", "secret"));
        assertThat(requests).allSatisfy(request -> assertThat(request.hasHeader("Authorization")).isFalse());
    }

    @Test public void
    proxy_login_with_a_wrong_password_answers_the_challenge_once_and_returns_the_407() {
        server.respond("/path", proxyRequiringLogin("admin", "secret", Responder.ok()));

        given().
                proxy(host("localhost").withPort(server.port()).withAuth("admin", "wrong")).
        when().
                get(TARGET).
        then().
                statusCode(407).
                header("Proxy-Authenticate", PROXY_LOGIN_CHALLENGE);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).hasSize(2);
        assertThat(requests.get(1).header("Proxy-Authorization")).isEqualTo(basic("admin", "wrong"));
    }

    @Test public void
    proxy_without_login_returns_the_407() {
        server.respond("/path", proxyRequiringLogin("admin", "secret", Responder.ok()));

        given().
                proxy("localhost", server.port()).
        when().
                get(TARGET).
        then().
                statusCode(407);

        assertThat(server.onlyRequest().hasHeader("Proxy-Authorization")).isFalse();
    }

    @Test public void
    proxy_specification_with_the_scheme_set_after_the_login_loses_the_login() {
        server.respond("/path", proxyRequiringLogin("admin", "secret", Responder.ok()));

        given().
                proxy(host("localhost").withPort(server.port()).withAuth("admin", "secret").withScheme("http")).
        when().
                get(TARGET).
        then().
                statusCode(407);

        // Today's behavior: ProxySpecification.withScheme(..) drops the user name and password
        assertThat(server.onlyRequest().hasHeader("Proxy-Authorization")).isFalse();
    }

    @Test public void
    proxy_login_and_basic_login_to_the_target_are_answered_one_after_the_other() {
        String targetLogin = basic("user", "pass");
        server.respond("/path", proxyRequiringLogin("admin", "secret", (request, response) -> {
            if (targetLogin.equals(request.header("Authorization"))) {
                Responder.text(200, "authenticated").respond(request, response);
            } else {
                Responder.text(401, "unauthorized").withHeader("WWW-Authenticate", "Basic realm=\"target\"").respond(request, response);
            }
        }));

        given().
                proxy(host("localhost").withPort(server.port()).withAuth("admin", "secret")).
                auth().basic("user", "pass").
        when().
                get(TARGET).
        then().
                statusCode(200).
                body(equalTo("authenticated"));

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).extracting(r -> r.header("Proxy-Authorization")).containsExactly(null, basic("admin", "secret"), basic("admin", "secret"));
        assertThat(requests).extracting(r -> r.header("Authorization")).containsExactly(null, null, targetLogin);
    }

    @Test public void
    proxy_login_is_sent_again_after_a_redirect_to_another_host() {
        server.respond("/path", proxyRequiringLogin("admin", "secret", Responder.redirect(302, "http://other.invalid/target")));
        server.respond("/target", proxyRequiringLogin("admin", "secret", Responder.ok()));

        given().
                proxy(host("localhost").withPort(server.port()).withAuth("admin", "secret")).
        when().
                get(TARGET).
        then().
                statusCode(200);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).extracting(RecordedRequest::target).containsExactly(TARGET, TARGET, "http://other.invalid/target");
        assertThat(requests).extracting(r -> r.header("Proxy-Authorization")).containsExactly(null, basic("admin", "secret"), basic("admin", "secret"));
    }

    // Proxy configured only through JVM system properties

    @Test public void
    http_proxy_system_properties_are_used() {
        withProxyProperties(Map.of("http.proxyHost", "localhost", "http.proxyPort", String.valueOf(server.port())), () ->
                given().
                when().
                        get(TARGET).
                then().
                        statusCode(200));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.requestLine()).isEqualTo("GET http://target.invalid/path HTTP/1.1");
        assertThat(request.headers("Host")).containsExactly("target.invalid");
    }

    @Test public void
    http_proxy_system_properties_are_not_used_for_localhost_by_default() {
        withProxyProperties(Map.of("http.proxyHost", "proxy.invalid", "http.proxyPort", "3128"), () ->
                given().
                when().
                        get(server.url("/path")).
                then().
                        statusCode(200));

        assertThat(server.onlyRequest().target()).isEqualTo("/path");
    }

    @Test public void
    http_proxy_system_properties_are_used_for_localhost_when_non_proxy_hosts_is_empty() {
        withProxyProperties(Map.of("http.proxyHost", "localhost", "http.proxyPort", String.valueOf(server.port()), "http.nonProxyHosts", ""), () ->
                given().
                when().
                        get(server.url("127.0.0.1", "/path")).
                then().
                        statusCode(200));

        assertThat(server.onlyRequest().target()).isEqualTo(server.url("127.0.0.1", "/path"));
    }

    @Test public void
    http_proxy_system_properties_are_not_used_for_non_proxy_hosts() {
        withProxyProperties(Map.of("http.proxyHost", "localhost", "http.proxyPort", String.valueOf(server.port()), "http.nonProxyHosts", "*.invalid"), () ->
                assertThatThrownBy(() -> given().when().get(TARGET)).isInstanceOf(UnknownHostException.class));

        assertThat(server.requests()).isEmpty();
    }

    @Test public void
    proxy_configured_in_rest_assured_wins_over_the_system_properties() {
        withProxyProperties(Map.of("http.proxyHost", "proxy.invalid", "http.proxyPort", "3128"), () ->
                given().
                        proxy("localhost", server.port()).
                when().
                        get(TARGET).
                then().
                        statusCode(200));

        assertThat(server.onlyRequest().target()).isEqualTo(TARGET);
    }

    @Test public void
    https_proxy_system_properties_are_used_for_https_targets_through_connect() {
        // WireServer can't open a tunnel and fails the CONNECT with a 500, which HttpClient 4 returns as the response
        withProxyProperties(Map.of("https.proxyHost", "localhost", "https.proxyPort", String.valueOf(server.port())), () ->
                given().
                when().
                        get("https://target.invalid/path").
                then().
                        statusCode(500));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.method()).isEqualTo("CONNECT");
        assertThat(request.headers("Host")).containsExactly("target.invalid:443");
    }

    private static Responder proxyRequiringLogin(String user, String password, Responder whenLoggedIn) {
        String expected = basic(user, password);
        return (request, response) -> {
            if (expected.equals(request.header("Proxy-Authorization"))) {
                whenLoggedIn.respond(request, response);
            } else {
                Responder.text(407, "proxy login required").withHeader("Proxy-Authenticate", PROXY_LOGIN_CHALLENGE).respond(request, response);
            }
        };
    }

    private static String basic(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(UTF_8));
    }

    // Sets the proxy properties to the given values, clears the others (the environment may set some), and restores them all
    private static void withProxyProperties(Map<String, String> values, Runnable test) {
        Map<String, String> previous = new HashMap<>();
        PROXY_PROPERTIES.forEach(name -> previous.put(name, System.getProperty(name)));
        try {
            PROXY_PROPERTIES.forEach(name -> setOrClear(name, values.get(name)));
            test.run();
        } finally {
            previous.forEach(WireProxyITest::setOrClear);
        }
    }

    private static void setOrClear(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
