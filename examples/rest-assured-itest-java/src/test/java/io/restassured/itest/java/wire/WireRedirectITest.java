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

import io.restassured.common.exception.TooManyRedirectsException;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.ProtocolException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.config.RedirectConfig.redirectConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;

public class WireRedirectITest extends WireTest {
    private static final String JSON_BODY = "{\"a\":1}";

    static Stream<Arguments> redirectStatusesForGetAndHead() {
        return Stream.of(301, 302, 303, 307, 308).flatMap(status -> Stream.of("GET", "HEAD").map(method -> Arguments.of(status, method)));
    }

    static Stream<Arguments> redirectStatusesForMethodsWithABody(int... statuses) {
        return Stream.of("POST", "PUT", "DELETE", "PATCH").flatMap(method -> Arrays.stream(statuses).mapToObj(status -> Arguments.of(status, method)));
    }

    static Stream<Arguments> notFollowedByDefault() {
        return redirectStatusesForMethodsWithABody(301, 302, 307, 308);
    }

    static Stream<Arguments> becomeGetWhenFollowingAllMethods() {
        return redirectStatusesForMethodsWithABody(301, 302, 303);
    }

    static Stream<Arguments> repeatedWhenFollowingAllMethods() {
        return redirectStatusesForMethodsWithABody(307, 308);
    }

    @ParameterizedTest(name = "{0} after {1}")
    @MethodSource("redirectStatusesForGetAndHead")
    public void
    get_and_head_redirects_are_followed_by_default_with_the_same_method(int status, String method) {
        server.respond("/from", Responder.redirect(status, "/to"));
        server.respond("/to", Responder.text(200, "landed"));

        Response response = given().header("X-Custom", "c").request(method, server.url("/from"));

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).extracting(RecordedRequest::requestLine).containsExactly(method + " /from HTTP/1.1", method + " /to HTTP/1.1");
        RecordedRequest followUp = requests.get(1);
        assertThat(followUp.header("X-Custom")).isEqualTo("c");
        assertThat(followUp.hasHeader("Content-Type")).isFalse();
        if (method.equals("HEAD") && Runtime.version().feature() < 18) {
            // JDK differences table, not in the table (W5): the JDK client sends HEAD with Content-Length: 0 on Java 17
            assertThat(followUp.header("Content-Length")).isEqualTo("0");
        } else {
            assertThat(followUp.hasHeader("Content-Length")).isFalse();
        }
        assertThat(followUp.body()).isEmpty();
        response.then().statusCode(200).body(equalTo(method.equals("HEAD") ? "" : "landed"));
    }

    @ParameterizedTest(name = "{0} after {1}")
    @MethodSource("notFollowedByDefault")
    public void
    redirects_of_methods_with_a_body_are_not_followed_by_default(int status, String method) {
        server.respond("/from", Responder.redirect(status, "/to"));

        Response response = sendJson(given(), method);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.requestLine()).isEqualTo(method + " /from HTTP/1.1");
        assertThat(request.bodyAsString()).isEqualTo(JSON_BODY);
        response.then().statusCode(status).header("Location", "/to").body(emptyString());
    }

    @ParameterizedTest(name = "303 after {0}")
    @ValueSource(strings = {"POST", "PUT", "DELETE", "PATCH"})
    public void
    see_other_after_a_method_with_a_body_is_followed_by_default_with_a_get_without_body_that_still_sends_the_content_type(String method) {
        server.respond("/from", Responder.redirect(303, "/to"));
        server.respond("/to", Responder.text(200, "landed"));

        Response response = sendJson(given(), method);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).extracting(RecordedRequest::requestLine).containsExactly(method + " /from HTTP/1.1", "GET /to HTTP/1.1");
        RecordedRequest followUp = requests.get(1);
        // Today's behavior: the follow-up GET has no body but keeps the original Content-Type
        assertThat(followUp.headers("Content-Type")).containsExactly("application/json");
        assertThat(followUp.hasHeader("Content-Length")).isFalse();
        assertThat(followUp.hasHeader("Transfer-Encoding")).isFalse();
        assertThat(followUp.body()).isEmpty();
        response.then().statusCode(200).body(equalTo("landed"));
    }

    @ParameterizedTest(name = "{0} after {1}")
    @MethodSource("becomeGetWhenFollowingAllMethods")
    public void
    following_all_methods_turns_301_302_and_303_into_a_get_without_body_that_still_sends_the_content_type(int status, String method) {
        server.respond("/from", Responder.redirect(status, "/to"));
        server.respond("/to", Responder.text(200, "landed"));

        Response response = sendJson(given().config(config().redirect(redirectConfig().followRedirectsForAllMethods(true))), method);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).extracting(RecordedRequest::requestLine).containsExactly(method + " /from HTTP/1.1", "GET /to HTTP/1.1");
        RecordedRequest followUp = requests.get(1);
        // Today's behavior: the follow-up GET has no body but keeps the original Content-Type
        assertThat(followUp.headers("Content-Type")).containsExactly("application/json");
        assertThat(followUp.hasHeader("Content-Length")).isFalse();
        assertThat(followUp.body()).isEmpty();
        response.then().statusCode(200).body(equalTo("landed"));
    }

    @ParameterizedTest(name = "{0} after {1}")
    @MethodSource("repeatedWhenFollowingAllMethods")
    public void
    following_all_methods_repeats_the_method_and_body_on_307_and_308(int status, String method) {
        server.respond("/from", Responder.redirect(status, "/to"));
        server.respond("/to", Responder.text(200, "landed"));

        Response response = sendJson(given().config(config().redirect(redirectConfig().followRedirectsForAllMethods(true))), method);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).extracting(RecordedRequest::requestLine).containsExactly(method + " /from HTTP/1.1", method + " /to HTTP/1.1");
        RecordedRequest followUp = requests.get(1);
        assertThat(followUp.headers("Content-Type")).containsExactly("application/json");
        assertThat(followUp.headers("Content-Length")).containsExactly("7");
        assertThat(followUp.bodyAsString()).isEqualTo(JSON_BODY);
        response.then().statusCode(200).body(equalTo("landed"));
    }

    @ParameterizedTest(name = "after {0}")
    @ValueSource(ints = {307, 308})
    public void
    following_all_methods_repeats_a_delete_without_a_body_on_307_and_308(int status) {
        server.respond("/from", Responder.redirect(status, "/to"));

        given().config(config().redirect(redirectConfig().followRedirectsForAllMethods(true))).delete(server.url("/from")).then().statusCode(200);

        assertThat(server.requests()).extracting(RecordedRequest::requestLine).containsExactly("DELETE /from HTTP/1.1", "DELETE /to HTTP/1.1");
        assertThat(server.requests().get(1).body()).isEmpty();
    }

    @Test public void
    a_post_followed_by_a_303_sends_a_get_with_the_posts_content_type_and_charset() {
        server.respond("/from", Responder.redirect(303, "/to"));

        given().body("hello").post(server.url("/from")).then().statusCode(200);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests.get(0).headers("Content-Type")).containsExactly("text/plain; charset=ISO-8859-1");
        assertThat(requests.get(1).requestLine()).isEqualTo("GET /to HTTP/1.1");
        // Today's behavior: the GET still sends the POST's Content-Type
        assertThat(requests.get(1).headers("Content-Type")).containsExactly("text/plain; charset=ISO-8859-1");
        assertThat(requests.get(1).body()).isEmpty();
    }

    @Test public void
    a_form_post_followed_by_a_303_sends_a_get_with_the_form_content_type_and_no_form_body() {
        server.respond("/from", Responder.redirect(303, "/to"));

        given().formParam("a", "b").post(server.url("/from")).then().statusCode(200);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests.get(0).bodyAsString()).isEqualTo("a=b");
        assertThat(requests.get(1).requestLine()).isEqualTo("GET /to HTTP/1.1");
        // Today's behavior: the GET still sends the form's Content-Type
        assertThat(requests.get(1).headers("Content-Type")).containsExactly("application/x-www-form-urlencoded; charset=ISO-8859-1");
        assertThat(requests.get(1).body()).isEmpty();
    }

    @Test public void
    the_follow_up_resends_the_request_headers_but_not_the_query_params_or_path_of_the_original_request() {
        server.respond("/users/42", Responder.redirect(302, "/to"));

        given().
                header("X-Custom", "c").
                header("X-Multi", "1", "2").
                accept("application/json").
                queryParam("q", "1").
                pathParam("id", 42).
        when().
                get(server.url("/users/{id}")).
        then().
                statusCode(200);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).extracting(RecordedRequest::target).containsExactly("/users/42?q=1", "/to");
        RecordedRequest followUp = requests.get(1);
        assertThat(followUp.headers("X-Custom")).containsExactly("c");
        assertThat(followUp.headers("X-Multi")).containsExactly("1", "2");
        assertThat(followUp.headers("Accept")).containsExactly("application/json");
    }

    @Test public void
    the_follow_up_sends_the_default_accept_header() {
        server.respond("/from", Responder.redirect(302, "/to"));

        given().get(server.url("/from")).then().statusCode(200);

        assertThat(server.lastRequest().headers("Accept")).containsExactly("*/*");
    }

    @Test public void
    the_user_gets_the_status_headers_and_body_of_the_last_response() {
        server.respond("/from", Responder.redirect(302, "/middle").withHeader("X-From", "from"));
        server.respond("/middle", Responder.redirect(301, "/to").withHeader("X-Middle", "middle"));
        server.respond("/to", Responder.text(201, "landed").withHeader("X-To", "to"));

        Response response = given().get(server.url("/from"));

        assertThat(server.requests()).extracting(RecordedRequest::target).containsExactly("/from", "/middle", "/to");
        response.then().statusCode(201).header("X-To", "to").body(equalTo("landed"));
        assertThat(response.headers().hasHeaderWithName("X-From")).isFalse();
        assertThat(response.headers().hasHeaderWithName("X-Middle")).isFalse();
        assertThat(response.headers().hasHeaderWithName("Location")).isFalse();
    }

    // RedirectConfig

    @Test public void
    follow_redirects_false_in_the_config_returns_the_redirect_response() {
        server.respond("/from", Responder.redirect(302, "/to"));

        Response response = given().config(config().redirect(redirectConfig().followRedirects(false))).get(server.url("/from"));

        assertThat(server.onlyRequest().target()).isEqualTo("/from");
        response.then().statusCode(302).header("Location", "/to");
    }

    @Test public void
    redirects_follow_false_applies_to_that_request_only() {
        server.respond("/from", Responder.redirect(302, "/to"));

        given().redirects().follow(false).get(server.url("/from")).then().statusCode(302).header("Location", "/to");
        given().get(server.url("/from")).then().statusCode(200);

        assertThat(server.requests()).extracting(RecordedRequest::target).containsExactly("/from", "/from", "/to");
    }

    @Test public void
    redirects_follow_true_on_the_request_wins_over_follow_redirects_false_in_the_config() {
        server.respond("/from", Responder.redirect(302, "/to"));

        given().config(config().redirect(redirectConfig().followRedirects(false))).redirects().follow(true).get(server.url("/from")).then().statusCode(200);

        assertThat(server.requests()).extracting(RecordedRequest::target).containsExactly("/from", "/to");
    }

    @Test public void
    as_many_redirects_as_the_max_are_followed() {
        server.respond("/a", Responder.redirect(302, "/b"));
        server.respond("/b", Responder.redirect(302, "/c"));

        given().redirects().max(2).get(server.url("/a")).then().statusCode(200);

        assertThat(server.requests()).extracting(RecordedRequest::target).containsExactly("/a", "/b", "/c");
    }

    @Test public void
    more_redirects_than_the_max_throw_a_too_many_redirects_exception() {
        server.respond("/a", Responder.redirect(302, "/b"));
        server.respond("/b", Responder.redirect(302, "/c"));
        server.respond("/c", Responder.redirect(302, "/d"));

        assertRedirectFails(() -> given().redirects().max(2).get(server.url("/a")), TooManyRedirectsException.class, "Maximum redirects (2) exceeded");
        assertThat(server.requests()).extracting(RecordedRequest::target).containsExactly("/a", "/b", "/c");
    }

    @Test public void
    max_redirects_in_the_config_limits_the_redirects_like_redirects_max() {
        server.respond("/a", Responder.redirect(302, "/b"));
        server.respond("/b", Responder.redirect(302, "/c"));

        assertRedirectFails(() -> given().config(config().redirect(redirectConfig().maxRedirects(1))).get(server.url("/a")), TooManyRedirectsException.class, "Maximum redirects (1) exceeded");
        assertThat(server.requests()).extracting(RecordedRequest::target).containsExactly("/a", "/b");
    }

    @Test public void
    max_redirects_zero_fails_on_the_first_redirect() {
        server.respond("/from", Responder.redirect(302, "/to"));

        assertRedirectFails(() -> given().redirects().max(0).get(server.url("/from")), TooManyRedirectsException.class, "Maximum redirects (0) exceeded");
        assertThat(server.onlyRequest().target()).isEqualTo("/from");
    }

    @Test public void
    at_most_100_redirects_are_followed_by_default() {
        server.respond("/loop", Responder.redirect(302, "/loop"));

        assertRedirectFails(() -> given().redirects().allowCircular(true).get(server.url("/loop")), TooManyRedirectsException.class, "Maximum redirects (100) exceeded");
        assertThat(server.requests()).hasSize(101);
    }

    @Test public void
    a_circular_redirect_throws_a_protocol_exception_by_default() {
        server.respond("/a", Responder.redirect(302, "/b"));
        server.respond("/b", Responder.redirect(302, "/a"));

        assertRedirectFails(() -> given().get(server.url("/a")), ProtocolException.class, "Circular redirect to '" + server.url("/b") + "'");
        // Today's behavior: the first URI isn't counted as visited, so /a is requested twice before the loop is detected
        assertThat(server.requests()).extracting(RecordedRequest::target).containsExactly("/a", "/b", "/a");
    }

    @Test public void
    a_redirect_to_the_same_path_with_another_query_is_not_circular() {
        server.respond("/a", (request, response) -> Responder.redirect(302, request.target().equals("/a") ? "/a?n=1" : "/b").respond(request, response));

        given().get(server.url("/a")).then().statusCode(200);

        assertThat(server.requests()).extracting(RecordedRequest::target).containsExactly("/a", "/a?n=1", "/b");
    }

    @Test public void
    allow_circular_follows_a_loop_until_max_redirects() {
        server.respond("/a", Responder.redirect(302, "/b"));
        server.respond("/b", Responder.redirect(302, "/a"));

        assertRedirectFails(() -> given().redirects().allowCircular(true).redirects().max(3).get(server.url("/a")), TooManyRedirectsException.class, "Maximum redirects (3) exceeded");
        assertThat(server.requests()).extracting(RecordedRequest::target).containsExactly("/a", "/b", "/a", "/b");
    }

    @Test public void
    allow_circular_redirects_in_the_config_follows_a_loop_until_max_redirects() {
        server.respond("/loop", Responder.redirect(302, "/loop"));

        assertRedirectFails(() -> given().config(config().redirect(redirectConfig().allowCircularRedirects(true).maxRedirects(2))).get(server.url("/loop")), TooManyRedirectsException.class, "Maximum redirects (2) exceeded");
        assertThat(server.requests()).hasSize(3);
    }

    @Test public void
    reject_relative_throws_on_a_relative_location() {
        server.respond("/from", Responder.redirect(302, "/to"));

        assertRedirectFails(() -> given().redirects().rejectRelative(true).get(server.url("/from")), ProtocolException.class, "Relative redirect location '/to' not allowed");
        assertThat(server.onlyRequest().target()).isEqualTo("/from");
    }

    @Test public void
    reject_relative_redirect_in_the_config_throws_on_a_relative_location() {
        server.respond("/from", Responder.redirect(302, "to"));

        assertRedirectFails(() -> given().config(config().redirect(redirectConfig().rejectRelativeRedirect(true))).get(server.url("/from")), ProtocolException.class, "Relative redirect location 'to' not allowed");
    }

    @Test public void
    reject_relative_follows_an_absolute_location() {
        server.respond("/from", Responder.redirect(302, server.url("/to")));

        given().redirects().rejectRelative(true).get(server.url("/from")).then().statusCode(200);

        assertThat(server.requests()).extracting(RecordedRequest::target).containsExactly("/from", "/to");
    }

    // Location headers

    @ParameterizedTest(name = "Location {0} -> {1}")
    @MethodSource("locations")
    public void
    the_location_is_resolved_against_the_request_uri(String location, String expectedTarget) {
        server.respond("/dir/from", Responder.redirect(302, location));

        given().queryParam("orig", "1").get(server.url("/dir/from")).then().statusCode(200);

        assertThat(server.requests()).extracting(RecordedRequest::target).containsExactly("/dir/from?orig=1", expectedTarget);
        assertThat(server.lastRequest().host()).isEqualTo("localhost");
    }

    static Stream<Arguments> locations() {
        return Stream.of(
                Arguments.of("to", "/dir/to"),
                Arguments.of("../to", "/to"),
                Arguments.of("/to", "/to"),
                Arguments.of("/to?a=b%20c&d=e%2F", "/to?a=b%20c&d=e%2F"),
                Arguments.of("/to#fragment", "/to"),
                Arguments.of("/to?x=1#fragment", "/to?x=1"),
                Arguments.of("/t%20o", "/t%20o"),
                Arguments.of("/t%C3%B6", "/t%C3%B6"));
    }

    @Test public void
    an_absolute_location_is_followed() {
        server.respond("/from", Responder.redirect(302, server.url("/to?x=1")));

        given().get(server.url("/from")).then().statusCode(200);

        assertThat(server.lastRequest().target()).isEqualTo("/to?x=1");
    }

    @Test public void
    a_network_path_location_is_followed_to_its_host() {
        server.respond("/from", Responder.redirect(302, "//127.0.0.1:" + server.port() + "/to"));

        given().get(server.url("/from")).then().statusCode(200);

        assertThat(server.lastRequest().host()).isEqualTo("127.0.0.1");
        assertThat(server.lastRequest().target()).isEqualTo("/to");
    }

    @Test public void
    a_query_only_location_keeps_the_path_and_replaces_the_query() {
        server.respond("/dir/from", (request, response) -> {
            if (request.target().contains("only")) {
                Responder.ok().respond(request, response);
            } else {
                Responder.redirect(302, "?only=query").respond(request, response);
            }
        });

        given().queryParam("orig", "1").get(server.url("/dir/from")).then().statusCode(200);

        assertThat(server.requests()).extracting(RecordedRequest::target).containsExactly("/dir/from?orig=1", "/dir/from?only=query");
    }

    @ParameterizedTest(name = "Location {0}")
    @ValueSource(strings = {"/t o", "/to?q=a b"})
    public void
    a_location_with_a_space_throws_a_protocol_exception(String location) {
        server.respond("/from", Responder.redirect(302, location));

        assertRedirectFails(() -> given().get(server.url("/from")), ProtocolException.class, "Invalid redirect URI: " + location);
        assertThat(server.onlyRequest().target()).isEqualTo("/from");
    }

    @Test public void
    a_location_with_a_raw_non_ascii_character_is_followed_with_the_character_replaced() {
        // Jetty writes the header as ISO-8859-1, so the location is sent as the single byte 0xF6
        server.respond("/from", Responder.redirect(302, "/tö"));

        given().get(server.url("/from")).then().statusCode(200);

        // Today's behavior: the byte isn't decoded as a character, it becomes U+FFFD and is sent UTF-8 percent-encoded
        assertThat(server.lastRequest().target()).isEqualTo("/t%EF%BF%BD");
    }

    // Cross-host redirects

    @Test public void
    a_redirect_to_another_host_doesnt_resend_authorization_and_cookie_but_resends_other_headers() {
        server.respond("/from", Responder.redirect(302, server.url("127.0.0.1", "/to")));

        sendWithSensitiveHeaders(given());

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).extracting(RecordedRequest::host).containsExactly("localhost", "127.0.0.1");
        RecordedRequest followUp = requests.get(1);
        assertThat(followUp.hasHeader("Authorization")).isFalse();
        assertThat(followUp.hasHeader("Cookie")).isFalse();
        assertThat(followUp.headers("X-Custom")).containsExactly("c");
        assertThat(followUp.headers("Accept")).containsExactly("application/json");
    }

    @Test public void
    a_307_redirect_to_another_host_doesnt_resend_authorization_and_cookie() {
        server.respond("/from", Responder.redirect(307, server.url("127.0.0.1", "/to")));

        sendWithSensitiveHeaders(given());

        RecordedRequest followUp = server.lastRequest();
        assertThat(followUp.host()).isEqualTo("127.0.0.1");
        assertThat(followUp.hasHeader("Authorization")).isFalse();
        assertThat(followUp.hasHeader("Cookie")).isFalse();
        assertThat(followUp.headers("X-Custom")).containsExactly("c");
    }

    @Test public void
    a_redirect_to_the_same_host_resends_authorization_and_cookie() {
        server.respond("/from", Responder.redirect(302, server.url("localhost", "/to")));

        sendWithSensitiveHeaders(given());

        RecordedRequest followUp = server.lastRequest();
        assertThat(followUp.host()).isEqualTo("localhost");
        assertThat(followUp.headers("Authorization")).containsExactly("Bearer token");
        assertThat(followUp.headers("Cookie")).containsExactly("session=abc");
        assertThat(followUp.headers("X-Custom")).containsExactly("c");
        assertThat(followUp.headers("Accept")).containsExactly("application/json");
    }

    @Test public void
    a_redirect_back_to_the_original_host_resends_authorization_and_cookie_again() {
        server.respond("/from", Responder.redirect(302, server.url("127.0.0.1", "/to")));
        server.respond("/to", Responder.redirect(302, server.url("localhost", "/back")));

        sendWithSensitiveHeaders(given());

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).extracting(RecordedRequest::host).containsExactly("localhost", "127.0.0.1", "localhost");
        assertThat(requests.get(1).hasHeader("Authorization")).isFalse();
        assertThat(requests.get(2).headers("Authorization")).containsExactly("Bearer token");
        assertThat(requests.get(2).headers("Cookie")).containsExactly("session=abc");
    }

    @Test public void
    a_redirect_to_another_host_resends_authorization_and_cookie_when_not_stripping_sensitive_headers() {
        server.respond("/from", Responder.redirect(302, server.url("127.0.0.1", "/to")));

        sendWithSensitiveHeaders(given().config(config().redirect(redirectConfig().stripSensitiveHeadersOnCrossHostRedirect(false))));

        RecordedRequest followUp = server.lastRequest();
        assertThat(followUp.host()).isEqualTo("127.0.0.1");
        assertThat(followUp.headers("Authorization")).containsExactly("Bearer token");
        assertThat(followUp.headers("Cookie")).containsExactly("session=abc");
    }

    // A redirect that REST Assured won't follow throws a java.net.ProtocolException, or its TooManyRedirectsException
    private static void assertRedirectFails(ThrowingCallable request, Class<? extends Exception> type, String message) {
        assertThatThrownBy(request).isExactlyInstanceOf(type).hasMessage(message);
    }

    private void sendWithSensitiveHeaders(RequestSpecification spec) {
        spec.
                header("X-Custom", "c").
                header("Authorization", "Bearer token").
                cookie("session", "abc").
                accept("application/json").
        when().
                get(server.url("/from")).
        then().
                statusCode(200);
        assertThat(server.requests().get(0).headers("Authorization")).containsExactly("Bearer token");
        assertThat(server.requests().get(0).headers("Cookie")).containsExactly("session=abc");
    }

    private Response sendJson(RequestSpecification spec, String method) {
        return spec.header("X-Custom", "c").contentType("application/json").body(JSON_BODY).request(method, server.url("/from"));
    }
}
