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

import io.restassured.http.ContentType;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Which method, body and body headers REST Assured sends for each HTTP method, with and without a body.
 */
public class WireRequestMethodITest extends WireTest {

    private static final String TEXT_PLAIN_ISO_8859_1 = "text/plain; charset=ISO-8859-1";

    @ParameterizedTest(name = "{0}")
    @MethodSource("methodsSentWithoutBodyHeaders")
    public void
    method_without_body_sends_neither_content_type_nor_content_length(String method, Send send) {
        send.apply(given(), server.url("/m")).then().statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.requestLine()).isEqualTo(method + " /m HTTP/1.1");
        assertSendsNoBodyHeaders(request);
        assertThat(request.body()).isEmpty();
    }

    static Stream<Arguments> methodsSentWithoutBodyHeaders() {
        return Stream.of(
                arguments("GET", send(RequestSpecification::get)),
                arguments("DELETE", send(RequestSpecification::delete)),
                arguments("HEAD", send(RequestSpecification::head)),
                arguments("OPTIONS", send(RequestSpecification::options)),
                arguments("TRACE", send((spec, url) -> spec.request(Method.TRACE, url))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("methodsSentWithAnEmptyBody")
    public void
    method_without_body_sends_content_length_zero_and_no_content_type(String method, Send send) {
        send.apply(given(), server.url("/m")).then().statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.requestLine()).isEqualTo(method + " /m HTTP/1.1");
        assertThat(request.hasHeader("Content-Type")).isFalse();
        assertThat(request.header("Content-Length")).isEqualTo("0");
        assertThat(request.body()).isEmpty();
    }

    static Stream<Arguments> methodsSentWithAnEmptyBody() {
        return Stream.of(
                arguments("PUT", send(RequestSpecification::put)),
                arguments("PATCH", send(RequestSpecification::patch)),
                arguments("QUERY", send((spec, url) -> spec.request(Method.QUERY, url))),
                arguments("PURGE", send((spec, url) -> spec.request("PURGE", url))));
    }

    @Test public void
    post_without_body_sends_form_url_encoded_content_type_and_empty_body() {
        given().post(server.url("/m")).then().statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.requestLine()).isEqualTo("POST /m HTTP/1.1");
        assertThat(request.header("Content-Type")).isEqualTo("application/x-www-form-urlencoded; charset=ISO-8859-1");
        assertThat(request.header("Content-Length")).isEqualTo("0");
        assertThat(request.body()).isEmpty();
    }

    @Test public void
    head_returns_the_response_headers_and_an_empty_body() {
        Response response = given().head(server.url("/m"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.header("Content-Length")).isEqualTo("2");
        assertThat(response.asString()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE", "QUERY", "PURGE"})
    public void
    every_method_sends_a_string_body_as_text_plain_iso_8859_1(String method) {
        given().body("hello").request(method, server.url("/m")).then().statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.method()).isEqualTo(method);
        assertThat(request.target()).isEqualTo("/m");
        assertThat(request.header("Content-Type")).isEqualTo(TEXT_PLAIN_ISO_8859_1);
        assertThat(request.header("Content-Length")).isEqualTo("5");
        assertThat(request.bodyAsString()).isEqualTo("hello");
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE", "QUERY", "PURGE"})
    public void
    every_method_sends_an_empty_string_body_with_content_type_and_content_length_zero(String method) {
        given().body("").request(method, server.url("/m")).then().statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.method()).isEqualTo(method);
        assertThat(request.header("Content-Type")).isEqualTo(TEXT_PLAIN_ISO_8859_1);
        assertThat(request.header("Content-Length")).isEqualTo("0");
        assertThat(request.body()).isEmpty();
    }

    @Test public void
    get_with_json_body_sends_the_body_as_utf_8_json() {
        given().contentType(ContentType.JSON).body("{\"a\":\"ä\"}").get(server.url("/m")).then().statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.header("Content-Type")).isEqualTo("application/json");
        assertThat(request.header("Content-Length")).isEqualTo("10");
        assertThat(request.body()).containsExactly("{\"a\":\"ä\"}".getBytes(UTF_8));
    }

    @Test public void
    delete_with_map_body_sends_it_serialized_as_json() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("a", "b");

        given().contentType(ContentType.JSON).body(body).delete(server.url("/m")).then().statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.method()).isEqualTo("DELETE");
        assertThat(request.header("Content-Type")).isEqualTo("application/json");
        assertThat(request.header("Content-Length")).isEqualTo("9");
        assertThat(request.bodyAsString()).isEqualTo("{\"a\":\"b\"}");
    }

    @Test public void
    query_with_json_body_and_query_param_sends_both() {
        given().contentType(ContentType.JSON).queryParam("a", "b").body("{}").request(Method.QUERY, server.url("/m")).then().statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.requestLine()).isEqualTo("QUERY /m?a=b HTTP/1.1");
        assertThat(request.header("Content-Type")).isEqualTo("application/json");
        assertThat(request.header("Content-Length")).isEqualTo("2");
        assertThat(request.bodyAsString()).isEqualTo("{}");
    }

    @Test public void
    query_as_string_is_the_same_as_the_query_enum() {
        given().body("q").request("query", server.url("/m")).then().statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.method()).isEqualTo("QUERY");
        assertThat(request.header("Content-Type")).isEqualTo(TEXT_PLAIN_ISO_8859_1);
        assertThat(request.bodyAsString()).isEqualTo("q");
    }

    @Test public void
    custom_method_with_object_body_sends_it_serialized_as_json() {
        given().contentType(ContentType.JSON).body(new Item("x")).request("PURGE", server.url("/m")).then().statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.method()).isEqualTo("PURGE");
        assertThat(request.header("Content-Type")).isEqualTo("application/json");
        assertThat(request.bodyAsString()).isEqualTo("{\"name\":\"x\"}");
    }

    @Test public void
    custom_method_returns_the_response_to_the_user() {
        server.respond("/m", Responder.text(202, "purged"));

        given().body("x").request("PURGE", server.url("/m")).then().statusCode(202).body(equalTo("purged"));
    }

    @Test public void
    options_with_content_type_and_no_body_sends_the_content_type_without_content_length() {
        given().contentType("text/plain").options(server.url("/m")).then().statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.method()).isEqualTo("OPTIONS");
        assertThat(request.header("Content-Type")).isEqualTo(TEXT_PLAIN_ISO_8859_1);
        assertNoContentLength(request);
        assertThat(request.body()).isEmpty();
    }

    @Test public void
    get_with_content_type_and_no_body_sends_the_content_type() {
        given().contentType(ContentType.JSON).get(server.url("/m")).then().statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/json");
        assertThat(request.hasHeader("Content-Length")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUT", "PATCH", "DELETE", "QUERY", "PURGE"})
    public void
    params_on_methods_other_than_post_are_sent_as_query_parameters(String method) {
        given().param("a", "b c").request(method, server.url("/m")).then().statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.method()).isEqualTo(method);
        assertThat(request.target()).isEqualTo("/m?a=b%20c");
        assertThat(request.hasHeader("Content-Type")).isFalse();
        assertThat(request.body()).isEmpty();
    }

    @Test public void
    put_with_param_and_body_sends_the_param_in_the_query_and_the_body_as_is() {
        given().param("a", "b").body("x").put(server.url("/m")).then().statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.requestLine()).isEqualTo("PUT /m?a=b HTTP/1.1");
        assertThat(request.header("Content-Type")).isEqualTo(TEXT_PLAIN_ISO_8859_1);
        assertThat(request.bodyAsString()).isEqualTo("x");
    }

    @Test public void
    method_name_is_trimmed_and_upper_cased() {
        given().request(" opTions  ", server.url("/m")).then().statusCode(200);

        assertThat(server.onlyRequest().method()).isEqualTo("OPTIONS");
    }

    @Test public void
    method_name_is_trimmed_and_upper_cased_when_there_is_a_body() {
        given().body("x").request(" opTions  ", server.url("/m")).then().statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.method()).isEqualTo("OPTIONS");
        assertThat(request.bodyAsString()).isEqualTo("x");
    }

    @Test public void
    custom_method_name_is_upper_cased() {
        given().request("purge", server.url("/m")).then().statusCode(200);

        assertThat(server.onlyRequest().method()).isEqualTo("PURGE");
    }

    @Test public void
    lower_case_get_is_sent_as_get_without_body_headers() {
        given().request("get", server.url("/m")).then().statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.method()).isEqualTo("GET");
        assertSendsNoBodyHeaders(request);
    }

    private static void assertSendsNoBodyHeaders(RecordedRequest request) {
        assertThat(request.hasHeader("Content-Type")).isFalse();
        assertNoContentLength(request);
        assertThat(request.hasHeader("Transfer-Encoding")).isFalse();
    }

    private static void assertNoContentLength(RecordedRequest request) {
        String method = request.method();
        boolean jdkSendsContentLength = !method.equals("GET") && !method.equals("DELETE") && (!method.equals("HEAD") || Runtime.version().feature() < 18);
        if (jdkSendsContentLength) {
            // JDK differences table, not in the table (W5): the JDK client sends Content-Length: 0 for a method without a
            // body other than GET, DELETE and, from Java 18, HEAD
            assertThat(request.header("Content-Length")).isEqualTo("0");
        } else {
            assertThat(request.hasHeader("Content-Length")).isFalse();
        }
    }

    // Sends the request to the url, typed so that a method reference or lambda can be an argument of a parameterized test
    interface Send extends BiFunction<RequestSpecification, String, Response> {
    }

    private static Send send(Send send) {
        return send;
    }

    public static class Item {
        public String name;

        public Item(String name) {
            this.name = name;
        }
    }
}
