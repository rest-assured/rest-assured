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

import groovy.lang.GString;
import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.internal.RestAssuredResponseOptionsImpl;
import io.restassured.internal.filter.RecordingServer.Reply;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import org.codehaus.groovy.runtime.GStringImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

/**
 * Pins the behavior of {@link FilterContextImpl}: filter chaining, the filter context values, sending a request
 * from a filter with {@link FilterContext#send(io.restassured.specification.RequestSender)}, and how exceptions
 * propagate. The spring-mock-mvc and spring-web-test-client modules construct it for request logging, which is
 * covered by their RequestLoggingTest.
 */
class FilterContextImplTest {

    private static final String[] STANDARD_METHODS = {"GET", "POST", "PUT", "DELETE", "HEAD", "PATCH", "OPTIONS", "QUERY"};

    private RecordingServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new RecordingServer();
        RestAssured.baseURI = "http://127.0.0.1";
        RestAssured.port = server.port();
        for (String method : STANDARD_METHODS) {
            server.route(method + " /things/5", r -> Reply.text(r.method() + " " + r.path() + (r.query() == null ? "" : "?" + r.query())));
        }
        for (String method : new String[]{"TRACE", "PURGE"}) {
            server.route(method + " /things/5", r -> Reply.text(r.method() + " " + r.path()));
        }
    }

    @AfterEach
    void stopServer() {
        server.close();
        RestAssured.reset();
    }

    // Constructed directly, the way spring-mock-mvc and spring-web-test-client do for request logging

    @Test
    void next_returns_null_when_there_are_no_more_filters() {
        FilterContextImpl ctx = newContext(Collections.<Filter>emptyList().iterator(), new HashMap<>());

        assertThat(ctx.next(null, null)).isNull();
    }

    @Test
    void next_skips_null_filters_and_returns_null_when_only_null_filters_remain() {
        FilterContextImpl ctx = newContext(Arrays.<Filter>asList(null, null).iterator(), new HashMap<>());

        assertThat(ctx.next(null, null)).isNull();
    }

    @Test
    void next_fails_with_class_cast_exception_when_the_request_specification_is_not_rest_assureds_own() {
        Filter filter = (req, res, ctx) -> null;
        FilterContextImpl ctx = newContext(Collections.singletonList(filter).iterator(), new HashMap<>());
        FilterableRequestSpecification foreign = (FilterableRequestSpecification) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{FilterableRequestSpecification.class}, (proxy, method, args) -> null);

        assertThatThrownBy(() -> ctx.next(foreign, null)).isInstanceOf(ClassCastException.class);
    }

    @Test
    void exposes_internal_request_uri_assertion_closure_and_properties() {
        Map<String, Object> properties = new HashMap<>();
        Object assertionClosure = new Object();
        FilterContextImpl ctx = new FilterContextImpl("http://localhost:8080/x?a=b", "/x", "/x", "/internal?a=b", "/x", new Object[0], "GET",
                assertionClosure, Collections.<Filter>emptyList().iterator(), properties);

        assertThat(ctx.getInternalRequestURI()).isEqualTo("/internal?a=b");
        assertThat(ctx.getAssertionClosure()).isSameAs(assertionClosure);
        assertThat(ctx.getProperties()).isSameAs(properties);

        Object otherClosure = new Object();
        Map<String, Object> otherProperties = new HashMap<>();
        ctx.setAssertionClosure(otherClosure);
        ctx.setProperties(otherProperties);
        ctx.setValue("x", "y");

        assertThat(ctx.getAssertionClosure()).isSameAs(otherClosure);
        assertThat(ctx.getProperties()).isSameAs(otherProperties).containsEntry("x", "y");
        assertThat(properties).isEmpty();
    }

    @Test
    void set_value_writes_through_to_the_properties_map() {
        Map<String, Object> properties = new HashMap<>();
        FilterContextImpl ctx = newContext(Collections.<Filter>emptyList().iterator(), properties);

        ctx.setValue("name", "value");
        ctx.setValue("nothing", null);

        assertThat(properties).containsEntry("name", "value").containsEntry("nothing", null).hasSize(2);
        Object value = ctx.getValue("name");
        assertThat(value).isEqualTo("value");
        assertThat((Object) ctx.getValue("missing")).isNull();
    }

    @Test
    void has_value_is_false_for_missing_names_and_null_values() {
        FilterContextImpl ctx = newContext(Collections.<Filter>emptyList().iterator(), new HashMap<>());
        ctx.setValue("name", "value");
        ctx.setValue("nothing", null);

        assertThat(ctx.hasValue("name")).isTrue();
        assertThat(ctx.hasValue("nothing")).isFalse();
        assertThat(ctx.hasValue("missing")).isFalse();
        assertThat(ctx.hasValue("nothing", null)).isFalse();
        assertThat(ctx.hasValue("missing", null)).isFalse();
    }

    @Test
    void has_value_with_value_compares_like_groovy_equality() {
        FilterContextImpl ctx = newContext(Collections.<Filter>emptyList().iterator(), new HashMap<>());
        ctx.setValue("string", "value");
        ctx.setValue("int", 1);
        ctx.setValue("bigDecimal", new BigDecimal("1.0"));
        ctx.setValue("double", 2.5d);
        ctx.setValue("char", 'a');
        ctx.setValue("singleCharString", "a");
        ctx.setValue("list", Arrays.asList(1, "x"));
        ctx.setValue("array", new String[]{"a", "b"});
        ctx.setValue("intArray", new int[]{1, 2});
        ctx.setValue("map", Collections.singletonMap("k", "v"));
        ctx.setValue("numberMap", Collections.singletonMap("k", 1));
        ctx.setValue("nestedList", Collections.singletonList(Collections.singletonList(1)));
        ctx.setValue("set", new HashSet<>(Arrays.asList("a", "b")));
        ctx.setValue("gstring", gstring("val", "ue"));
        ctx.setValue("object", new Object());

        assertThat(ctx.hasValue("string", "value")).isTrue();
        assertThat(ctx.hasValue("string", "other")).isFalse();
        assertThat(ctx.hasValue("string", gstring("val", "ue"))).isTrue();
        assertThat(ctx.hasValue("string", 1)).isFalse();

        assertThat(ctx.hasValue("int", 1)).isTrue();
        assertThat(ctx.hasValue("int", 1L)).isTrue();
        assertThat(ctx.hasValue("int", (short) 1)).isTrue();
        assertThat(ctx.hasValue("int", 1.0d)).isTrue();
        assertThat(ctx.hasValue("int", new BigDecimal("1.00"))).isTrue();
        assertThat(ctx.hasValue("int", BigInteger.ONE)).isTrue();
        assertThat(ctx.hasValue("int", 2L)).isFalse();
        assertThat(ctx.hasValue("int", "1")).isFalse();
        assertThat(ctx.hasValue("int", (char) 1)).isTrue();
        assertThat(ctx.hasValue("bigDecimal", new BigDecimal("1.000"))).isTrue();
        assertThat(ctx.hasValue("bigDecimal", 1)).isTrue();
        assertThat(ctx.hasValue("double", 2.5f)).isTrue();
        assertThat(ctx.hasValue("double", new BigDecimal("2.5"))).isTrue();

        assertThat(ctx.hasValue("char", "a")).isTrue();
        assertThat(ctx.hasValue("char", 'a')).isTrue();
        assertThat(ctx.hasValue("char", 97)).isTrue();
        assertThat(ctx.hasValue("char", "b")).isFalse();
        assertThat(ctx.hasValue("singleCharString", 'a')).isTrue();
        assertThat(ctx.hasValue("singleCharString", 97)).isTrue();

        assertThat(ctx.hasValue("list", Arrays.asList(1L, "x"))).isTrue();
        assertThat(ctx.hasValue("list", new Object[]{1, "x"})).isTrue();
        assertThat(ctx.hasValue("list", Arrays.asList(1, "y"))).isFalse();
        assertThat(ctx.hasValue("list", Collections.singletonList(1))).isFalse();
        assertThat(ctx.hasValue("array", new String[]{"a", "b"})).isTrue();
        assertThat(ctx.hasValue("array", Arrays.asList("a", "b"))).isTrue();
        assertThat(ctx.hasValue("array", new String[]{"a"})).isFalse();
        assertThat(ctx.hasValue("intArray", new int[]{1, 2})).isTrue();
        assertThat(ctx.hasValue("intArray", new long[]{1, 2})).isTrue();
        assertThat(ctx.hasValue("intArray", Arrays.asList(1, 2))).isTrue();

        assertThat(ctx.hasValue("map", Collections.singletonMap("k", "v"))).isTrue();
        assertThat(ctx.hasValue("map", Collections.singletonMap("k", "w"))).isFalse();
        assertThat(ctx.hasValue("map", Collections.singletonMap("j", "v"))).isFalse();
        assertThat(ctx.hasValue("numberMap", Collections.singletonMap("k", 1L))).isTrue();
        assertThat(ctx.hasValue("numberMap", Collections.singletonMap("k", 2L))).isFalse();
        assertThat(ctx.hasValue("nestedList", Collections.singletonList(Collections.singletonList(1L)))).isTrue();
        assertThat(ctx.hasValue("set", new HashSet<>(Arrays.asList("b", "a")))).isTrue();
        assertThat(ctx.hasValue("set", Collections.singleton("a"))).isFalse();
        assertThat(ctx.hasValue("gstring", "value")).isTrue();
        assertThat(ctx.hasValue("gstring", gstring("va", "lue"))).isTrue();
        assertThat(ctx.hasValue("gstring", 'v')).isFalse();
        Object object = ctx.getValue("object");
        assertThat(ctx.hasValue("object", object)).isTrue();
        assertThat(ctx.hasValue("object", new Object())).isFalse();
    }

    // Through a real request

    @Test
    void filters_run_in_order_with_a_new_context_each_sharing_the_same_values() {
        List<String> calls = new ArrayList<>();
        AtomicReference<FilterContext> first = new AtomicReference<>();
        AtomicReference<Object> seenBySecond = new AtomicReference<>();

        Response response = given().
                filter((req, res, ctx) -> {
                    calls.add("first");
                    first.set(ctx);
                    ctx.setValue("key", 1);
                    return ctx.next(req, res);
                }).
                filter((req, res, ctx) -> {
                    calls.add("second");
                    assertThat(ctx).isNotSameAs(first.get()).isInstanceOf(FilterContextImpl.class);
                    seenBySecond.set(ctx.getValue("key"));
                    assertThat(ctx.hasValue("key")).isTrue();
                    assertThat(ctx.hasValue("key", 1L)).isTrue();
                    ctx.setValue("second", "two");
                    return ctx.next(req, res);
                }).
        when().
                get("/things/{id}", 5);

        assertThat(calls).containsExactly("first", "second");
        assertThat(seenBySecond.get()).isEqualTo(1);
        assertThat(first.get().<Object>getValue("second")).isEqualTo("two");
        assertThat(((RestAssuredResponseOptionsImpl<?>) response).getFilterContextProperties())
                .containsEntry("key", 1).containsEntry("second", "two");
        assertThat(response.asString()).isEqualTo("GET /things/5");
    }

    @Test
    void next_passes_the_assertion_closure_on_so_expectations_are_validated() {
        Filter passThrough = (req, res, ctx) -> ctx.next(req, res);

        given().filter(passThrough).expect().body(equalTo("GET /things/5")).when().get("/things/5");

        assertThatThrownBy(() -> given().filter(passThrough).expect().body(equalTo("other")).when().get("/things/5"))
                .isInstanceOf(AssertionError.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "DELETE", "HEAD", "PATCH", "OPTIONS", "QUERY"})
    void send_uses_the_method_and_path_of_the_original_request(String method) {
        Response response = given().
                filter((req, res, ctx) -> ctx.send(given().header("X-Sent-By", "filter"))).
        when().
                request(method, "/things/{id}", 5);

        assertThat(server.requestLines()).containsExactly(method + " /things/5");
        assertThat(server.lastRequestTo(method + " /things/5").header("X-Sent-By")).isEqualTo("filter");
        assertThat(response.statusCode()).isEqualTo(200);
        if (!"HEAD".equals(method)) {
            assertThat(response.asString()).isEqualTo(method + " /things/5");
        }
    }

    @Test
    void send_uses_query_parameters_in_the_path_but_not_those_defined_outside_it() {
        Response response = given().
                queryParam("outside", "o").
                filter((req, res, ctx) -> ctx.send(given())).
        when().
                get("/things/{id}?inside=i", 5);

        assertThat(response.asString()).isEqualTo("GET /things/5?inside=i");
    }

    @Test
    void send_with_a_fully_qualified_uri_sends_to_that_uri() {
        Response response = given().
                filter((req, res, ctx) -> ctx.send(given().baseUri("http://unused.invalid"))).
        when().
                post("http://127.0.0.1:" + server.port() + "/things/5");

        assertThat(response.asString()).isEqualTo("POST /things/5");
    }

    @Test
    void send_passes_through_exceptions_thrown_by_the_sent_request_unchanged() {
        IllegalStateException failure = new IllegalStateException("boom");

        assertThatThrownBy(() -> given().
                filter((req, res, ctx) -> ctx.send(given().filter((r, s, c) -> {
                    throw failure;
                }))).
        when().
                get("/things/5")).isSameAs(failure);
    }

    @Test
    void next_passes_through_runtime_exceptions_thrown_by_later_filters_unchanged() {
        IllegalStateException failure = new IllegalStateException("boom");

        assertThatThrownBy(() -> given().
                filter((req, res, ctx) -> ctx.next(req, res)).
                filter((req, res, ctx) -> {
                    throw failure;
                }).
        when().
                get("/things/5")).isSameAs(failure);
    }

    @Test
    void next_passes_through_checked_exceptions_thrown_by_later_filters_unchanged() {
        IOException failure = new IOException("boom");

        assertThatThrownBy(() -> given().
                filter((req, res, ctx) -> ctx.next(req, res)).
                filter((req, res, ctx) -> sneakyThrow(failure)).
        when().
                get("/things/5")).isSameAs(failure);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TRACE", "PURGE"})
    void send_works_for_a_method_without_a_request_sender_method_of_its_own(String method) {
        Response response = given().
                filter((req, res, ctx) -> ctx.send(given())).
        when().
                request(method, "/things/{id}", 5);

        assertThat(server.requestLines()).containsExactly(method + " /things/5");
        assertThat(response.asString()).isEqualTo(method + " /things/5");
    }

    private static FilterContextImpl newContext(Iterator<Filter> filters, Map<String, Object> properties) {
        return new FilterContextImpl("http://localhost:8080/x", "/x", "/x", "/x", "/x", new Object[0], "GET", null, filters, properties);
    }

    private static GString gstring(String first, String second) {
        return new GStringImpl(new Object[]{second}, new String[]{first, ""});
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends Throwable> T sneakyThrow(Throwable t) throws E {
        throw (E) t;
    }
}
