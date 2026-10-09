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

package io.restassured.internal;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSender;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins how {@link TestSpecificationImpl} (returned by {@code RestAssured.given(RequestSpecification, ResponseSpecification)})
 * wires the specifications together and delegates to the request specification.
 */
class TestSpecificationImplTest {

    private final List<String> sent = new ArrayList<>();

    @AfterEach
    void resetRestAssured() {
        RestAssured.reset();
    }

    @Test
    void wires_the_request_and_response_specifications_together() {
        RequestSpecification request = new RequestSpecBuilder().build();
        ResponseSpecification response = new ResponseSpecBuilder().build();

        TestSpecificationImpl testSpecification = new TestSpecificationImpl(request, response);

        assertThat(testSpecification.getRequestSpecification()).isSameAs(request);
        assertThat(testSpecification.getResponseSpecification()).isSameAs(response);
        assertThat(response.request()).isSameAs(request);
        assertThat(request.response()).isSameAs(response);
    }

    @Test
    void the_response_specification_is_validated() {
        RequestSpecification request = new RequestSpecBuilder().addFilter((req, res, ctx) -> new ResponseBuilder().setStatusCode(404).setBody("").build()).build();
        ResponseSpecification response = new ResponseSpecBuilder().expectStatusCode(200).build();

        assertThatThrownBy(() -> RestAssured.given(request, response).get("/x"))
                .isInstanceOf(AssertionError.class).hasMessageContaining("Expected status code <200> but was <404>.");
    }

    @Test
    void the_specifications_must_not_be_null() {
        assertThatThrownBy(() -> new TestSpecificationImpl(null, new ResponseSpecBuilder().build()))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("requestSpecification cannot be null");
        assertThatThrownBy(() -> new TestSpecificationImpl(new RequestSpecBuilder().build(), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("responseSpecification cannot be null");
    }

    @Test
    void path_and_unnamed_path_params_are_delegated() throws Exception {
        assertSent(s -> s.get("/{a}", "1"), "GET http://localhost:8080/1");
        assertSent(s -> s.post("/{a}", "1"), "POST http://localhost:8080/1");
        assertSent(s -> s.put("/{a}", "1"), "PUT http://localhost:8080/1");
        assertSent(s -> s.delete("/{a}", "1"), "DELETE http://localhost:8080/1");
        assertSent(s -> s.head("/{a}", "1"), "HEAD http://localhost:8080/1");
        assertSent(s -> s.patch("/{a}", "1"), "PATCH http://localhost:8080/1");
        assertSent(s -> s.options("/{a}", "1"), "OPTIONS http://localhost:8080/1");
        assertSent(s -> s.query("/{a}", "1"), "QUERY http://localhost:8080/1");
        assertSent(s -> s.request(Method.TRACE, "/{a}", "1"), "TRACE http://localhost:8080/1");
        assertSent(s -> s.request("purge", "/{a}", "1"), "PURGE http://localhost:8080/1");
    }

    @Test
    void path_and_named_path_params_are_delegated() {
        Map<String, Object> pathParams = new LinkedHashMap<>();
        pathParams.put("a", 1);
        assertSent(s -> s.get("/{a}", pathParams), "GET http://localhost:8080/1");
        assertSent(s -> s.post("/{a}", pathParams), "POST http://localhost:8080/1");
        assertSent(s -> s.put("/{a}", pathParams), "PUT http://localhost:8080/1");
        assertSent(s -> s.delete("/{a}", pathParams), "DELETE http://localhost:8080/1");
        assertSent(s -> s.head("/{a}", pathParams), "HEAD http://localhost:8080/1");
        assertSent(s -> s.patch("/{a}", pathParams), "PATCH http://localhost:8080/1");
        assertSent(s -> s.options("/{a}", pathParams), "OPTIONS http://localhost:8080/1");
        assertSent(s -> s.query("/{a}", pathParams), "QUERY http://localhost:8080/1");
    }

    @Test
    void uris_and_urls_are_delegated() throws Exception {
        URI uri = URI.create("http://example.com:1234/x?y=z");
        URL url = uri.toURL();
        assertSent(s -> s.get(uri), "GET http://example.com:1234/x?y=z");
        assertSent(s -> s.post(uri), "POST http://example.com:1234/x?y=z");
        assertSent(s -> s.put(uri), "PUT http://example.com:1234/x?y=z");
        assertSent(s -> s.delete(uri), "DELETE http://example.com:1234/x?y=z");
        assertSent(s -> s.head(uri), "HEAD http://example.com:1234/x?y=z");
        assertSent(s -> s.patch(uri), "PATCH http://example.com:1234/x?y=z");
        assertSent(s -> s.options(uri), "OPTIONS http://example.com:1234/x?y=z");
        assertSent(s -> s.query(uri), "QUERY http://example.com:1234/x?y=z");
        assertSent(s -> s.get(url), "GET http://example.com:1234/x?y=z");
        assertSent(s -> s.post(url), "POST http://example.com:1234/x?y=z");
        assertSent(s -> s.put(url), "PUT http://example.com:1234/x?y=z");
        assertSent(s -> s.delete(url), "DELETE http://example.com:1234/x?y=z");
        assertSent(s -> s.head(url), "HEAD http://example.com:1234/x?y=z");
        assertSent(s -> s.patch(url), "PATCH http://example.com:1234/x?y=z");
        assertSent(s -> s.options(url), "OPTIONS http://example.com:1234/x?y=z");
        assertSent(s -> s.query(url), "QUERY http://example.com:1234/x?y=z");
        assertSent(s -> s.request(Method.GET, uri), "GET http://example.com:1234/x?y=z");
        assertSent(s -> s.request(Method.GET, url), "GET http://example.com:1234/x?y=z");
        assertSent(s -> s.request("copy", uri), "COPY http://example.com:1234/x?y=z");
        assertSent(s -> s.request("copy", url), "COPY http://example.com:1234/x?y=z");
    }

    @Test
    void no_path_means_the_base_uri() {
        assertSent(RequestSender::get, "GET http://localhost:8080/");
        assertSent(RequestSender::post, "POST http://localhost:8080/");
        assertSent(RequestSender::put, "PUT http://localhost:8080/");
        assertSent(RequestSender::delete, "DELETE http://localhost:8080/");
        assertSent(RequestSender::head, "HEAD http://localhost:8080/");
        assertSent(RequestSender::patch, "PATCH http://localhost:8080/");
        assertSent(RequestSender::options, "OPTIONS http://localhost:8080/");
        assertSent(RequestSender::query, "QUERY http://localhost:8080/");
        assertSent(s -> s.request(Method.DELETE), "DELETE http://localhost:8080/");
        assertSent(s -> s.request("lock"), "LOCK http://localhost:8080/");
    }

    @Test
    void null_arguments_are_rejected() {
        assertThatThrownBy(() -> sender().get((URI) null)).isInstanceOf(IllegalArgumentException.class).hasMessage("URI cannot be null");
        assertThatThrownBy(() -> sender().query((URI) null)).isInstanceOf(IllegalArgumentException.class).hasMessage("URI cannot be null");
        assertThatThrownBy(() -> sender().post((URL) null)).isInstanceOf(IllegalArgumentException.class).hasMessage("URL cannot be null");
        assertThatThrownBy(() -> sender().query((URL) null)).isInstanceOf(IllegalArgumentException.class).hasMessage("URL cannot be null");
        assertThatThrownBy(() -> sender().get((String) null)).isInstanceOf(IllegalArgumentException.class).hasMessage("path cannot be null");
        assertThatThrownBy(() -> sender().get("/x", (Map<String, ?>) null)).isInstanceOf(IllegalArgumentException.class).hasMessage("parameterNameValuePairs cannot be null");
        assertThatThrownBy(() -> sender().request((Method) null)).isInstanceOf(IllegalArgumentException.class).hasMessage("Method cannot be null");
        assertThatThrownBy(() -> sender().request((String) null)).isInstanceOf(IllegalArgumentException.class).hasMessage("Method cannot be null");
        assertThatThrownBy(() -> sender().request((Method) null, "/x")).isInstanceOf(IllegalArgumentException.class).hasMessage("Method cannot be null");
        assertThatThrownBy(() -> sender().request(Method.GET, (String) null)).isInstanceOf(IllegalArgumentException.class).hasMessage("path cannot be null");
        assertThatThrownBy(() -> sender().request((String) null, URI.create("http://localhost/x"))).isInstanceOf(IllegalArgumentException.class).hasMessage("Method cannot be null");
    }

    @Test
    void null_arguments_that_groovy_dispatched_differently_are_rejected_by_the_request_specification() {
        // Groovy routed a null array of path params to get(String, Map) ("parameterNameValuePairs cannot be null")
        assertThatThrownBy(() -> sender().get("/x", (Object[]) null)).isInstanceOf(IllegalArgumentException.class).hasMessage("Path params cannot be null");
        // Groovy failed with "Ambiguous method overloading" for these
        assertThatThrownBy(() -> sender().request(Method.GET, (URI) null)).isInstanceOf(IllegalArgumentException.class).hasMessage("String cannot be null");
        assertThatThrownBy(() -> sender().request("GET", (URI) null)).isInstanceOf(IllegalArgumentException.class).hasMessage("String cannot be null");
        assertThatThrownBy(() -> sender().request(Method.GET, (URL) null)).isInstanceOf(IllegalArgumentException.class).hasMessage("URL cannot be null");
        assertThatThrownBy(() -> sender().request("GET", (URL) null)).isInstanceOf(IllegalArgumentException.class).hasMessage("URL cannot be null");
    }

    private void assertSent(Function<RequestSender, Response> request, String expected) {
        sent.clear();
        request.apply(sender());
        assertThat(sent).containsExactly(expected);
    }

    private RequestSender sender() {
        RequestSpecification request = new RequestSpecBuilder().addFilter((req, res, ctx) -> {
            sent.add(req.getMethod() + " " + req.getURI());
            return new ResponseBuilder().setStatusCode(200).setBody("").build();
        }).build();
        return RestAssured.given(request, new ResponseSpecBuilder().build());
    }
}
