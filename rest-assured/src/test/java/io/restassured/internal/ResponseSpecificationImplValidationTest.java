/*
 * Copyright 2019 the original author or authors.
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

import io.restassured.builder.ResponseBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.FailureConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.time.TimingFilter;
import io.restassured.http.ContentType;
import io.restassured.internal.log.LogRepository;
import io.restassured.listener.ResponseValidationFailureListener;
import io.restassured.matcher.ResponseAwareMatcher;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.codehaus.groovy.runtime.GStringImpl;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingFormatArgumentException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.withArgs;
import static io.restassured.matcher.RestAssuredMatchers.detailedCookie;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.startsWith;

/**
 * Characterizes {@link ResponseSpecificationImpl}: every kind of expectation, the exact assertion error messages, root
 * paths and arguments, specification merging, when validation runs (eagerly for {@code then()}, after the filter chain for
 * {@code expect()}), failure listeners and logging on failure, and how errors propagate.
 * <p>
 * The responses come from a filter, so no request is sent.
 */
class ResponseSpecificationImplValidationTest {

    private static final String URL = "http://localhost:8080/";
    private static final String JSON = "{\"a\":{\"b\":1,\"c\":[1,2],\"d\":{\"e\":\"x\"}},\"f\":\"g\"}";
    private static final String HEADERS = "X-Name=value\nContent-Type=application/json; charset=UTF-8\nSet-Cookie=name=value; Path=/p";

    private static ResponseBuilder jsonResponse() {
        return new ResponseBuilder()
                .setStatusCode(200)
                .setStatusLine("HTTP/1.1 200 OK")
                .setHeader("X-Name", "value")
                .setHeader("Content-Type", "application/json; charset=UTF-8")
                .setHeader("Set-Cookie", "name=value; Path=/p")
                .setBody(JSON);
    }

    private static Filter respondWith(ResponseBuilder builder) {
        return new LastFilter(builder);
    }

    private static Response get(ResponseBuilder builder) {
        return given().filter(respondWith(builder)).get(URL);
    }

    private static ValidatableResponse then() {
        return get(jsonResponse()).then();
    }

    private static ResponseSpecificationImpl spec() {
        return new ResponseSpecificationImpl("", null, new ResponseParserRegistrar(), RestAssuredConfig.config(), new LogRepository());
    }

    private static Throwable failure(Runnable runnable) {
        Throwable throwable = catchThrowable(runnable::run);
        assertThat(throwable).isNotNull();
        return throwable;
    }

    // Status code and status line

    @Test
    void status_code_mismatch() {
        then().statusCode(200).statusCode(lessThan(300));

        assertThatThrownBy(() -> then().statusCode(201))
                .isExactlyInstanceOf(AssertionError.class)
                .hasMessage("1 expectation failed.\nExpected status code <201> but was <200>.\n");
        assertThatThrownBy(() -> then().statusCode(greaterThan(300)))
                .hasMessage("1 expectation failed.\nExpected status code a value greater than <300> but <200> was less than <300>.\n");
    }

    @Test
    void status_line_mismatch() {
        then().statusLine("HTTP/1.1 200 OK").statusLine(containsString("OK"));

        assertThatThrownBy(() -> then().statusLine("HTTP/1.1 201 Created"))
                .hasMessage("1 expectation failed.\nExpected status line \"HTTP/1.1 201 Created\" doesn't match actual status line \"HTTP/1.1 200 OK\".\n");
        assertThatThrownBy(() -> then().statusLine(containsString("Created")))
                .hasMessage("1 expectation failed.\nExpected status line a string containing \"Created\" doesn't match actual status line \"HTTP/1.1 200 OK\".\n");
    }

    // Content type

    @Test
    void content_type_as_string_ignores_case_and_white_space_and_matches_the_start() {
        then().contentType("application/json").contentType("APPLICATION/JSON;charset=utf-8").contentType("application/json ; charset = UTF-8");

        assertThatThrownBy(() -> then().contentType("application/xml"))
                .hasMessage("1 expectation failed.\nExpected content-type \"application/xml\" doesn't match actual content-type \"application/json; charset=UTF-8\".\n");
    }

    @Test
    void content_type_as_enum_ignores_parameters() {
        then().contentType(ContentType.JSON);

        assertThatThrownBy(() -> then().contentType(ContentType.XML))
                .hasMessage("1 expectation failed.\nExpected content-type \"XML\" doesn't match actual content-type \"application/json; charset=UTF-8\".\n");
    }

    @Test
    void content_type_as_enum_without_parameters_and_without_content_type() {
        get(new ResponseBuilder().setStatusCode(200).setContentType("application/xml")).then().contentType(ContentType.XML);

        assertThatThrownBy(() -> get(new ResponseBuilder().setStatusCode(200)).then().contentType(ContentType.JSON))
                .hasMessage("1 expectation failed.\nExpected content-type \"JSON\" doesn't match actual content-type \"null\".\n");
        assertThatThrownBy(() -> get(new ResponseBuilder().setStatusCode(200)).then().contentType("application/json"))
                .hasMessage("1 expectation failed.\nExpected content-type \"application/json\" doesn't match actual content-type \"null\".\n");
    }

    @Test
    void content_type_as_matcher() {
        then().contentType(startsWith("application/json"));

        assertThatThrownBy(() -> then().contentType(startsWith("text")))
                .hasMessage("1 expectation failed.\nExpected content-type a string starting with \"text\" doesn't match actual content-type \"application/json; charset=UTF-8\".\n");
    }

    @Test
    void the_content_type_used_for_the_request_is_any_unless_an_expected_content_type_is_set() throws Exception {
        ResponseSpecificationImpl spec = spec();
        assertThat(responseContentTypeOfAssertionClosure(spec)).isEqualTo(ContentType.ANY);

        spec.contentType("");
        assertThat(responseContentTypeOfAssertionClosure(spec)).isEqualTo(ContentType.ANY);

        spec.contentType("text/plain");
        assertThat(responseContentTypeOfAssertionClosure(spec)).isEqualTo("text/plain");

        spec.contentType(ContentType.XML);
        assertThat(responseContentTypeOfAssertionClosure(spec)).isEqualTo(ContentType.XML);

        Matcher<String> matcher = startsWith("text");
        spec.contentType(matcher);
        assertThat(responseContentTypeOfAssertionClosure(spec)).isSameAs(matcher);
    }

    @Test
    void response_content_type_is_the_expected_content_type_as_string_or_any() {
        // The Groovy version failed with a StackOverflowError
        ResponseSpecificationImpl spec = spec();
        assertThat(spec.getResponseContentType()).isEqualTo("*/*");

        spec.contentType("");
        assertThat(spec.getResponseContentType()).isEqualTo("*/*");

        spec.contentType("text/plain");
        assertThat(spec.getResponseContentType()).isEqualTo("text/plain");

        spec.contentType(ContentType.XML);
        assertThat(spec.getResponseContentType()).isEqualTo("application/xml");

        spec.contentType(startsWith("text"));
        assertThat(spec.getResponseContentType()).isEqualTo("a string starting with \"text\"");
    }

    private static Object responseContentTypeOfAssertionClosure(ResponseSpecificationImpl spec) throws Exception {
        Field field = ResponseSpecificationImpl.class.getDeclaredField("assertionClosure");
        field.setAccessible(true);
        ResponseSpecificationImpl.HamcrestAssertionClosure assertionClosure = (ResponseSpecificationImpl.HamcrestAssertionClosure) field.get(spec);
        return assertionClosure.getResponseContentType();
    }

    // Headers

    @Test
    void single_header_expectations() {
        then().header("X-Name", "value")
                .header("X-Name", startsWith("v"))
                .header("X-Name", String::length, equalTo(5))
                .header("X-Name", response -> equalTo(response.header("X-Name")));

        assertThatThrownBy(() -> then().header("X-Name", "other"))
                .hasMessage("1 expectation failed.\nExpected header \"X-Name\" was not \"other\", was \"value\". Headers are:\n" + HEADERS + "\n");
        assertThatThrownBy(() -> then().header("X-Name", String::length, equalTo(2)))
                .hasMessage("1 expectation failed.\nExpected header \"X-Name\" was not <2>, was \"5\". Headers are:\n" + HEADERS + "\n");
        assertThatThrownBy(() -> then().header("X-Missing", "value"))
                .hasMessage("1 expectation failed.\nExpected header \"X-Missing\" was not \"value\", was \"null\". Headers are:\n" + HEADERS + "\n");
    }

    @Test
    void header_maps_take_matchers_values_and_lists_of_both() {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("X-Name", Arrays.asList("value", startsWith("v")));
        expected.put("Content-Type", containsString("json"));
        then().headers(expected).headers("X-Name", "value", "X-Name", startsWith("va"), "Content-Type", containsString("UTF"));

        assertThatThrownBy(() -> then().headers("X-Name", "other", "X-Name", startsWith("x")))
                .hasMessage("2 expectations failed.\n"
                        + "Expected header \"X-Name\" was not \"other\", was \"value\". Headers are:\n" + HEADERS + "\n\n"
                        + "Expected header \"X-Name\" was not a string starting with \"x\", was \"value\". Headers are:\n" + HEADERS + "\n");
        assertThatThrownBy(() -> then().headers("X-Name", "value", "X-Other"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("You must supply the same number of keys as values.");
    }

    @Test
    void header_parameters_must_not_be_null() {
        assertThatThrownBy(() -> spec().header("X-Name", (Matcher<?>) null)).hasMessage("expectedValueMatcher cannot be null");
        assertThatThrownBy(() -> spec().header(null, startsWith("a"))).hasMessage("headerName cannot be null");
        assertThatThrownBy(() -> spec().headers(null)).hasMessage("expectedHeaders cannot be null");
        assertThatThrownBy(() -> spec().headers("X", null)).hasMessage("firstExpectedHeaderValue cannot be null");
        assertThatThrownBy(() -> spec().header("X", (Function<String, Integer>) String::length, (Matcher<Integer>) null)).hasMessage("Hamcrest matcher cannot be null");
        assertThatThrownBy(() -> spec().header("X", null, equalTo(1))).hasMessage("Mapping function cannot be null");
    }

    // Cookies

    @Test
    void cookie_expectations() {
        then().cookie("name")
                .cookie("name", "value")
                .cookie("name", startsWith("v"))
                .cookie("name", detailedCookie().path("/p"))
                .cookies("name", "value", "name", startsWith("va"))
                .cookies(Collections.singletonMap("name", Arrays.asList("value", startsWith("v"))));

        assertThatThrownBy(() -> then().cookie("name", "other"))
                .hasMessage("1 expectation failed.\nExpected cookie \"name\" was not \"other\", was \"value\".\n");
        assertThatThrownBy(() -> then().cookie("missing"))
                .hasMessage("1 expectation failed.\nCookie \"missing\" was not defined in the response. Cookies are: \nname=value;Path=/p\n");
        assertThatThrownBy(() -> then().cookie("name", detailedCookie().path("/q")))
                .hasMessage("1 expectation failed.\nExpected cookie \"name\" was not (not null and hasProperty(\"path\", \"/q\")), hasProperty(\"path\", \"/q\")  property 'path' was \"/p\".\n");
        assertThatThrownBy(() -> get(new ResponseBuilder().setStatusCode(200)).then().cookie("name"))
                .hasMessage("1 expectation failed.\nNo cookies defined in the response\n");
    }

    @Test
    void cookie_parameters_must_not_be_null() {
        assertThatThrownBy(() -> spec().cookies("a", "b", (Object[]) null)).hasMessage("expectedCookieNameValuePairs cannot be null");
        assertThatThrownBy(() -> spec().cookies((Map<String, ?>) null)).hasMessage("expectedCookies cannot be null");
        assertThatThrownBy(() -> spec().cookie(null)).hasMessage("cookieName cannot be null");
        assertThatThrownBy(() -> spec().cookie("a", (Matcher<?>) null)).hasMessage("expectedValueMatcher cannot be null");
    }

    // Body

    @Test
    void body_matchers_on_the_whole_body() {
        then().body(containsString("\"f\""), startsWith("{"));

        assertThatThrownBy(() -> then().body(startsWith("x"), startsWith("y")))
                .hasMessage("2 expectations failed.\n"
                        + "Response body doesn't match expectation.\nExpected: a string starting with \"x\"\n  Actual: " + JSON + "\n\n"
                        + "Response body doesn't match expectation.\nExpected: a string starting with \"y\"\n  Actual: " + JSON + "\n");
        assertThatThrownBy(() -> spec().body(null)).hasMessage("matcher cannot be null");
    }

    @Test
    void body_path_matchers_with_additional_key_matcher_pairs() {
        then().body("a.b", equalTo(1), "f", equalTo("g"), "a.c", hasItem(2), "a.c", hasItem(1));

        assertThatThrownBy(() -> then().body("a.b", equalTo(2), "f", equalTo("h"), "a.c", hasItem(3), "a.c", hasItem(1)))
                .hasMessage("3 expectations failed.\n"
                        + "JSON path a.b doesn't match.\nExpected: <2>\n  Actual: <1>\n\n"
                        + "JSON path f doesn't match.\nExpected: h\n  Actual: g\n\n"
                        + "JSON path a.c doesn't match.\nExpected: a collection containing <3>\n  Actual: <[1, 2]>\n");
        assertThatThrownBy(() -> then().body("a.b", equalTo(1), "f"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("You must supply at least one key and one value.");
        assertThatThrownBy(() -> then().body("a.b", equalTo(1), "f", equalTo("g"), "a.b"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("You must supply the same number of keys as values.");
        assertThatThrownBy(() -> spec().body((String) null, equalTo(1))).hasMessage("key cannot be null");
        assertThatThrownBy(() -> spec().body("a", (Matcher<?>) null)).hasMessage("matcher cannot be null");
    }

    @Test
    void body_path_matchers_with_arguments() {
        then().body("a.%s", withArgs("b"), equalTo(1),
                "a.%s.%s", withArgs("d", "e"), equalTo("x"),
                "a.%s", withArgs("c"), hasItem(1),
                "a.%s", withArgs("b"), equalTo(1));

        assertThatThrownBy(() -> then().body("a.%s", withArgs("b"), equalTo(2), "a.%s", withArgs("d"), equalTo(3), "a.%s", withArgs("b"), equalTo(4)))
                .hasMessage("3 expectations failed.\n"
                        + "JSON path a.b doesn't match.\nExpected: <2>\n  Actual: <1>\n\n"
                        + "JSON path a.d doesn't match.\nExpected: <3>\n  Actual: <{e=x}>\n\n"
                        + "JSON path a.b doesn't match.\nExpected: <4>\n  Actual: <1>\n");
        assertThatThrownBy(() -> then().body("a.%s", withArgs("b"), equalTo(1), "a.%s", Collections.singletonList("b"), equalTo(1)))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("You must supply the same number of keys as values.");
    }

    @Test
    void root_path_is_prepended_to_body_paths() {
        then().rootPath("a").body("b", equalTo(1), "c", hasItem(2)).body("c[0]", equalTo(1));

        assertThatThrownBy(() -> then().rootPath("a").body("b", equalTo(2), "[\"b\"]", equalTo(3), ".b", equalTo(4)))
                .hasMessage("3 expectations failed.\n"
                        + "JSON path a.b doesn't match.\nExpected: <2>\n  Actual: <1>\n\n"
                        + "JSON path a[\"b\"] doesn't match.\nExpected: <3>\n  Actual: <1>\n\n"
                        + "JSON path a.b doesn't match.\nExpected: <4>\n  Actual: <1>\n");
        assertThatThrownBy(() -> then().rootPath("a.").body(".b", equalTo(2), "b", equalTo(3)))
                .hasMessage("2 expectations failed.\n"
                        + "JSON path a.b doesn't match.\nExpected: <2>\n  Actual: <1>\n\n"
                        + "JSON path a.b doesn't match.\nExpected: <3>\n  Actual: <1>\n");
        assertThatThrownBy(() -> then().rootPath("a").body("?.b", equalTo(2), "?[0]", equalTo(3), "*.b", equalTo(4)))
                .hasMessageStartingWith("3 expectations failed.\n"
                        + "JSON path a?.b doesn't match.\nExpected: <2>\n  Actual: <1>\n\n"
                        + "JSON path a?[0] doesn't match.\n");
    }

    @Test
    void root_path_with_arguments_and_body_with_only_arguments() {
        then().rootPath("a.%s", withArgs("d")).body("e", equalTo("x"))
                .rootPath("a.%s.%s", withArgs("d")).body(withArgs("e"), equalTo("x"))
                .root("a.%s", withArgs("d")).body("e", equalTo("x"));

        assertThatThrownBy(() -> then().rootPath("a.%s.%s", withArgs("d")).body(withArgs("e"), equalTo("y"), withArgs("e"), equalTo("z")))
                .hasMessage("2 expectations failed.\n"
                        + "JSON path a.d.e doesn't match.\nExpected: y\n  Actual: x\n\n"
                        + "JSON path a.d.e doesn't match.\nExpected: z\n  Actual: x\n");
        assertThatThrownBy(() -> then().body(withArgs("e"), equalTo("y")))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot specify arguments when root path is empty");
    }

    @Test
    void a_gstring_key_is_used_as_a_string() {
        GStringImpl key = new GStringImpl(new Object[]{"f"}, new String[]{"", ""});
        then().body("a.b", equalTo(1), key, equalTo("g"));

        assertThatThrownBy(() -> then().rootPath("a").body("b", equalTo(1), new GStringImpl(new Object[]{"b"}, new String[]{"", ""}), equalTo(2)))
                .hasMessage("1 expectation failed.\nJSON path a.b doesn't match.\nExpected: <2>\n  Actual: <1>\n");
    }

    @Test
    void a_key_that_is_not_a_string_is_used_in_its_string_form_and_a_list_key_must_hold_arguments() {
        // The Groovy version failed with groovy.lang.MissingMethodException in both cases
        get(new ResponseBuilder().setStatusCode(200).setContentType(ContentType.JSON).setBody("{\"5\":\"five\"}")).then()
                .body("5", equalTo("five"), 5, equalTo("five"));
        assertThatThrownBy(() -> then().rootPath("a").body("b", equalTo(1), 5, equalTo(2)))
                .hasMessage("1 expectation failed.\nJSON path a.5 doesn't match.\nExpected: <2>\n  Actual: null\n");
        assertThatThrownBy(() -> spec().rootPath("a.%s").body("a", equalTo(1), Collections.singletonList("b"), equalTo(2)))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("The path of a body expectation must be a String or a list of io.restassured.specification.Argument, was '[b]'.");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void path_arguments_that_are_not_arguments_fail_with_illegal_argument_exception() {
        // The Groovy version failed with groovy.lang.MissingMethodException
        List notArguments = Collections.singletonList("b");
        String message = "Path arguments must be instances of io.restassured.specification.Argument, use withArgs(..) to create them. Was '[b]'.";
        assertThatThrownBy(() -> spec().rootPath("a.%s", notArguments).body("c", equalTo(1)))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage(message);
        assertThatThrownBy(() -> spec().body("a.%s", notArguments, equalTo(1)))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage(message);
    }

    @Test
    void a_value_that_is_not_a_matcher_fails_with_a_class_cast_exception() {
        String message = "Cannot cast object 'x' with class 'java.lang.String' to class 'org.hamcrest.Matcher'";
        assertThatThrownBy(() -> spec().body("a", equalTo(1), "b", "x")).isInstanceOf(ClassCastException.class).hasMessage(message);
        assertThatThrownBy(() -> spec().body("a", equalTo(1), "b", equalTo(1), "b", "x")).isInstanceOf(ClassCastException.class).hasMessage(message);
        assertThatThrownBy(() -> spec().rootPath("a").body("a", equalTo(1), withArgs("b"), "x")).isInstanceOf(ClassCastException.class).hasMessage(message);
    }

    @Test
    void all_missing_arguments_are_kept_as_placeholders() {
        ResponseSpecificationImpl spec = spec();
        spec.rootPath("a.%s.%s", withArgs("x"));
        assertThat(spec.getRootPath()).isEqualTo("a.x.%s");
        spec.rootPath("a.%s.%s.%s", withArgs("x"));
        assertThat(spec.getRootPath()).isEqualTo("a.x.%s.%s");
        spec.rootPath("a.%s[%d].%-3s.%%.%n", withArgs("x"));
        assertThat(spec.getRootPath()).isEqualTo("a.x[%d].%-3s.%." + System.lineSeparator());
        // A placeholder with an explicit index is not padded
        assertThatThrownBy(() -> spec().rootPath("a.%s.%2$s", withArgs("x")))
                .isExactlyInstanceOf(MissingFormatArgumentException.class)
                .hasMessage("Format specifier '%2$s'");

        then().rootPath("%s.%s.%s", withArgs("a")).body(withArgs("d", "e"), equalTo("x"))
                .rootPath("a.%s[%d]", withArgs("c")).body(withArgs(1), equalTo(2))
                .rootPath("%s.%s", withArgs("a")).body("%s", withArgs("d", "e"), equalTo("x"));
    }

    @Test
    void append_detach_and_no_root_path() {
        ResponseSpecificationImpl spec = spec();
        spec.rootPath("a");
        spec.appendRootPath("d");
        assertThat(spec.getRootPath()).isEqualTo("a.d");
        spec.appendRootPath("%s", withArgs("e"));
        assertThat(spec.getRootPath()).isEqualTo("a.d.e");
        spec.detachRootPath(" e ");
        assertThat(spec.getRootPath()).isEqualTo("a.d");
        spec.detachRootPath("d");
        assertThat(spec.getRootPath()).isEqualTo("a");
        spec.noRootPath();
        assertThat(spec.getRootPath()).isEqualTo("");
        spec.appendRootPath("b");
        assertThat(spec.getRootPath()).isEqualTo("b");

        assertThatThrownBy(() -> spec.detachRootPath("x"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot detach path 'x' since root path 'b' doesn't end with 'x'.");
        assertThatThrownBy(() -> spec().detachRootPath("x"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot detach path when root path is empty");
        assertThatThrownBy(() -> spec.rootPath(null)).hasMessage("Root path cannot be null");
        assertThatThrownBy(() -> spec.rootPath("a", null)).hasMessage("Arguments cannot be null");
        assertThatThrownBy(() -> spec.appendRootPath(null)).hasMessage("Path to append to root path cannot be null");
        assertThatThrownBy(() -> spec.appendRootPath("a", null)).hasMessage("Arguments for path to append cannot be null");
        assertThatThrownBy(() -> spec.detachRootPath(null)).hasMessage("Path to detach from root path cannot be null");
    }

    @Test
    void parsers_are_registered_on_the_spec() {
        ResponseBuilder custom = new ResponseBuilder().setStatusCode(200).setContentType("application/custom").setBody(JSON);
        get(custom).then().parser("application/custom", Parser.JSON).body("a.b", equalTo(1));

        ResponseBuilder noContentType = new ResponseBuilder().setStatusCode(200).setBody(JSON);
        get(noContentType).then().defaultParser(Parser.JSON).body("f", equalTo("g"));

        assertThatThrownBy(() -> spec().defaultParser(null)).hasMessage("Parser cannot be null");
    }

    // Response time

    @Test
    void response_time() {
        Response response = get(jsonResponse());
        Map<String, Object> properties = new HashMap<>();
        properties.put(TimingFilter.RESPONSE_TIME_MILLISECONDS, 1500L);
        ((RestAssuredResponseImpl) response).setFilterContextProperties(properties);

        response.then().time(lessThan(2000L)).time(lessThan(2L), TimeUnit.SECONDS);

        assertThatThrownBy(() -> response.then().time(lessThan(1000L)))
                .hasMessage("1 expectation failed.\nExpected response time was not a value less than <1000L> milliseconds, was 1500 milliseconds (1500 milliseconds).");
        assertThatThrownBy(() -> response.then().time(lessThan(1L), TimeUnit.SECONDS))
                .hasMessage("1 expectation failed.\nExpected response time was not a value less than <1L> seconds, was 1500 milliseconds (1 seconds).");
        assertThatThrownBy(() -> then().time(lessThan(1000L)))
                .hasMessage("1 expectation failed.\nNo time was recorded, cannot perform response time validation.");
        assertThatThrownBy(() -> spec().time(null)).hasMessage("Matcher cannot be null");
        assertThatThrownBy(() -> spec().time(lessThan(1L), null)).hasMessage("TimeUnit cannot be null");
    }

    // Several failures

    @Test
    void failures_are_reported_in_order_status_headers_cookies_content_type_time_body() {
        ResponseSpecificationImpl spec = spec();
        spec.forceDisableEagerAssert();
        spec.body("f", equalTo("x"))
                .time(lessThan(1L))
                .contentType("text/plain")
                .cookie("name", "other")
                .header("X-Name", "other")
                .statusLine("other")
                .statusCode(500);

        assertThatThrownBy(() -> spec.validate(get(jsonResponse())))
                .isExactlyInstanceOf(AssertionError.class)
                .hasMessage("7 expectations failed.\n"
                        + "Expected status code <500> but was <200>.\n\n"
                        + "Expected status line \"other\" doesn't match actual status line \"HTTP/1.1 200 OK\".\n\n"
                        + "Expected header \"X-Name\" was not \"other\", was \"value\". Headers are:\n" + HEADERS + "\n\n"
                        + "Expected cookie \"name\" was not \"other\", was \"value\".\n\n"
                        + "Expected content-type \"text/plain\" doesn't match actual content-type \"application/json; charset=UTF-8\".\n\n"
                        + "No time was recorded, cannot perform response time validation.\n"
                        + "JSON path f doesn't match.\nExpected: x\n  Actual: g\n");
    }

    @Test
    void on_fail_message_is_appended_unless_empty() {
        assertThatThrownBy(() -> then().onFailMessage("Some context").statusCode(201))
                .hasMessage("1 expectation failed.\nExpected status code <201> but was <200>.\n\nOn fail message: Some context");
        assertThatThrownBy(() -> then().onFailMessage("").statusCode(201))
                .hasMessage("1 expectation failed.\nExpected status code <201> but was <200>.\n");
        assertThatThrownBy(() -> given().filter(respondWith(jsonResponse())).expect().onFailMessage("ctx").statusCode(201).when().get(URL))
                .hasMessage("1 expectation failed.\nExpected status code <201> but was <200>.\n\nOn fail message: ctx");
    }

    // Validation timing

    @Test
    void then_validates_each_expectation_when_it_is_added_and_body_matchers_only_once() {
        CountingMatcher<Object> header = new CountingMatcher<>(true);
        CountingMatcher<Object> body = new CountingMatcher<>(true);
        CountingMatcher<Object> cookie = new CountingMatcher<>(true);

        ValidatableResponse then = then();
        then.header("X-Name", header);
        assertThat(header.count.get()).isEqualTo(1);
        then.body(body);
        assertThat(header.count.get()).isEqualTo(2);
        assertThat(body.count.get()).isEqualTo(1);
        then.cookie("name", cookie).statusCode(200).body("f", equalTo("g"));
        assertThat(header.count.get()).isEqualTo(5);
        assertThat(cookie.count.get()).isEqualTo(3);
        assertThat(body.count.get()).isEqualTo(1);
    }

    @Test
    void then_stops_at_the_first_failing_expectation() {
        CountingMatcher<Object> header = new CountingMatcher<>(true);

        ValidatableResponse then = then();
        assertThatThrownBy(() -> then.statusCode(500).header("X-Name", header)).isInstanceOf(AssertionError.class);

        assertThat(header.count.get()).isZero();
    }

    @Test
    void forced_validation_validates_all_expectations_once() {
        CountingMatcher<Object> header = new CountingMatcher<>(true);
        ResponseSpecificationImpl spec = (ResponseSpecificationImpl) ((ValidatableResponseImpl) then()).responseSpec.forceDisableEagerAssert();

        spec.header("X-Name", header).statusCode(500).statusLine("x");
        assertThat(header.count.get()).isZero();

        assertThatThrownBy(spec::forceValidateResponse)
                .isExactlyInstanceOf(AssertionError.class)
                .hasMessage("2 expectations failed.\nExpected status code <500> but was <200>.\n\n"
                        + "Expected status line \"x\" doesn't match actual status line \"HTTP/1.1 200 OK\".\n");
        assertThat(header.count.get()).isEqualTo(1);

        ResponseSpecificationImpl passing = (ResponseSpecificationImpl) ((ValidatableResponseImpl) then()).responseSpec.forceDisableEagerAssert();
        passing.statusCode(200);
        assertThat(passing.forceValidateResponse()).isNull();
    }

    @Test
    void expect_validates_after_the_filter_chain_once() {
        List<String> events = new ArrayList<>();
        CountingMatcher<Object> header = new CountingMatcher<>(true, () -> events.add("header matcher"));
        Filter outer = (requestSpec, responseSpec, ctx) -> {
            events.add("outer before");
            Response response = ctx.next(requestSpec, responseSpec);
            events.add("outer after");
            return response;
        };

        Response response = given().filter(outer).filter(respondWith(jsonResponse()))
                .expect().header("X-Name", header).statusCode(200).body("f", equalTo("g"))
                .when().get(URL);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(events).containsExactly("outer before", "outer after", "header matcher");
    }

    @Test
    void expect_reports_all_failures_after_the_request() {
        List<String> events = new ArrayList<>();
        Filter recording = (requestSpec, responseSpec, ctx) -> {
            events.add("filter");
            return ctx.next(requestSpec, responseSpec);
        };

        Throwable throwable = failure(() -> given().filter(recording).filter(respondWith(jsonResponse()))
                .expect().statusCode(500).header("X-Name", "other").body("a.b", equalTo(2))
                .when().get(URL));

        assertThat(events).containsExactly("filter");
        assertThat(throwable).isExactlyInstanceOf(AssertionError.class).hasMessage("3 expectations failed.\n"
                + "Expected status code <500> but was <200>.\n\n"
                + "Expected header \"X-Name\" was not \"other\", was \"value\". Headers are:\n" + HEADERS + "\n\n"
                + "JSON path a.b doesn't match.\nExpected: <2>\n  Actual: <1>\n");
    }

    @Test
    void filters_see_the_response_specification_of_the_request() {
        List<Object> seen = new ArrayList<>();
        Filter recording = (requestSpec, responseSpec, ctx) -> {
            seen.add(responseSpec);
            seen.add(((ResponseSpecificationImpl) responseSpec).getStatusCode());
            seen.add(((ResponseSpecificationImpl) responseSpec).hasHeaderAssertions());
            return ctx.next(requestSpec, responseSpec);
        };

        given().filter(recording).filter(respondWith(jsonResponse())).expect().statusCode(200).header("X-Name", "value").when().get(URL);

        assertThat(seen.get(0)).isInstanceOf(ResponseSpecificationImpl.class);
        assertThat(((Matcher<?>) seen.get(1)).matches(200)).isTrue();
        assertThat(seen.get(2)).isEqualTo(true);
    }

    // Failure listeners, logging and propagation

    @Test
    void failure_listeners_are_called_with_the_specs_and_response_when_validation_fails() {
        List<Object> calls = new ArrayList<>();
        ResponseValidationFailureListener listener = (requestSpec, responseSpec, response) -> {
            calls.add(requestSpec);
            calls.add(responseSpec);
            calls.add(response);
        };
        RestAssuredConfig config = RestAssuredConfig.config().failureConfig(FailureConfig.failureConfig().failureListeners(listener));

        Response response = given().config(config).filter(respondWith(jsonResponse())).get(URL);
        response.then().statusCode(200);
        assertThat(calls).isEmpty();
        ValidatableResponse then = response.then();
        assertThatThrownBy(() -> then.statusCode(201)).isInstanceOf(AssertionError.class);
        assertThat(calls).hasSize(3);
        assertThat(calls.get(0)).isNull();
        assertThat(calls.get(1)).isSameAs(((ValidatableResponseImpl) then).responseSpec);
        assertThat(calls.get(2)).isSameAs(response);

        calls.clear();
        RequestSpecification request = given().config(config).filter(respondWith(jsonResponse()));
        assertThatThrownBy(() -> request.expect().statusCode(201).when().get(URL)).isInstanceOf(AssertionError.class);
        assertThat(calls).hasSize(3);
        assertThat(calls.get(0)).isSameAs(request);
        assertThat(calls.get(1)).isSameAs(request.response());
        assertThat(calls.get(2)).isInstanceOf(Response.class);
    }

    @Test
    void errors_other_than_assertion_errors_propagate_unwrapped_after_calling_failure_listeners() {
        AtomicInteger calls = new AtomicInteger();
        RestAssuredConfig config = RestAssuredConfig.config().failureConfig(FailureConfig.failureConfig().failureListeners((req, res, response) -> calls.incrementAndGet()));
        IOException checked = new IOException("checked");
        ResponseAwareMatcher<Response> throwing = response -> {
            throw checked;
        };

        // then() evaluates a ResponseAwareMatcher before it reaches the response specification
        ValidatableResponse then = given().config(config).filter(respondWith(jsonResponse())).get(URL).then();
        assertThat(failure(() -> then.header("X-Name", throwing))).isSameAs(checked);
        assertThat(calls.get()).isZero();

        assertThat(failure(() -> given().config(config).filter(respondWith(jsonResponse())).expect().header("X-Name", throwing).when().get(URL)))
                .isSameAs(checked);
        assertThat(calls.get()).isEqualTo(1);

        IllegalStateException unchecked = new IllegalStateException("unchecked");
        Throwable thrown = failure(() -> then.body(new CountingMatcher<>(true, () -> {
            throw unchecked;
        })));
        assertThat(thrown).isSameAs(unchecked);
        assertThat(calls.get()).isEqualTo(2);

        assertThatThrownBy(() -> given().config(config).filter(respondWith(jsonResponse())).expect().header("X-Name", (ResponseAwareMatcher<Response>) response -> null).when().get(URL))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("The ResponseAwareMatcher for header \"X-Name\" returned null instead of a Hamcrest matcher");
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void assertion_errors_thrown_by_matchers_propagate_unwrapped() {
        AssertionError error = new AssertionError("from matcher");
        Throwable thrown = failure(() -> then().body(new CountingMatcher<>(true, () -> {
            throw error;
        })));
        assertThat(thrown).isSameAs(error);

        Throwable thrownFromExpect = failure(() -> given().filter(respondWith(jsonResponse()))
                .expect().body(new CountingMatcher<>(true, () -> {
                    throw error;
                })).when().get(URL));
        assertThat(thrownFromExpect).isSameAs(error);
    }

    @Test
    void request_and_response_are_logged_when_validation_fails() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(out, true);
        RestAssuredConfig config = RestAssuredConfig.config().logConfig(LogConfig.logConfig().defaultStream(stream).enableLoggingOfRequestAndResponseIfValidationFails());

        given().config(config).filter(respondWith(jsonResponse())).expect().statusCode(200).when().get(URL);
        assertThat(out.toString()).isEmpty();

        assertThatThrownBy(() -> given().config(config).filter(respondWith(jsonResponse())).expect().statusCode(201).when().get(URL))
                .isInstanceOf(AssertionError.class);
        String logged = out.toString();
        assertThat(logged).contains("Request method:\tGET").contains("Request URI:\t" + URL).contains("HTTP/1.1 200 OK").contains("X-Name: value");
    }

    @Test
    void then_logs_response_when_validation_fails() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RestAssuredConfig config = RestAssuredConfig.config().logConfig(LogConfig.logConfig().defaultStream(new PrintStream(out, true)));

        given().config(config).filter(respondWith(jsonResponse())).get(URL).then().log().ifValidationFails().statusCode(200);
        assertThat(out.toString()).isEmpty();

        assertThatThrownBy(() -> given().config(config).filter(respondWith(jsonResponse())).get(URL).then().log().ifValidationFails(LogDetail.STATUS).statusCode(201))
                .isInstanceOf(AssertionError.class);
        assertThat(out.toString()).isEqualTo("HTTP/1.1 200 OK" + System.lineSeparator());
    }

    @Test
    void validate_returns_the_response_and_does_nothing_without_expectations() {
        AtomicInteger calls = new AtomicInteger();
        RestAssuredConfig config = RestAssuredConfig.config().failureConfig(FailureConfig.failureConfig().failureListeners((req, res, response) -> calls.incrementAndGet()));
        ResponseSpecificationImpl spec = new ResponseSpecificationImpl("", null, new ResponseParserRegistrar(), config, new LogRepository());
        Response response = get(jsonResponse());

        assertThat(spec.validate(response)).isSameAs(response);
        assertThat(spec.hasAssertionsDefined()).isFalse();
        assertThat(calls.get()).isZero();
    }

    // Merging

    @Test
    void spec_merges_and_overwrites_expectations() {
        ResponseSpecification other = new ResponseSpecBuilder()
                .expectStatusCode(200)
                .expectStatusLine(containsString("OK"))
                .expectHeader("X-Name", "value")
                .expectCookie("name", "value")
                .expectContentType(ContentType.JSON)
                .rootPath("a")
                .expectBody("b", equalTo(1))
                .expectResponseTime(lessThan(10L), TimeUnit.DAYS)
                .log(LogDetail.STATUS)
                .build();

        ResponseSpecificationImpl spec = spec();
        spec.statusCode(500).header("X-Other", "x").rootPath("x").contentType("text/plain");
        spec.spec(other);

        assertThat(spec.getStatusCode().matches(200)).isTrue();
        assertThat(spec.getStatusLine().matches("200 OK")).isTrue();
        assertThat(spec.getRootPath()).isEqualTo("a");
        assertThat(spec.getLogDetail()).isEqualTo(LogDetail.STATUS);
        assertThat(spec.hasHeaderAssertions()).isTrue();
        assertThat(spec.hasCookieAssertions()).isTrue();
        assertThat(spec.hasBodyAssertionsDefined()).isTrue();

        spec.forceDisableEagerAssert();
        Response response = get(jsonResponse());
        Map<String, Object> properties = new HashMap<>();
        properties.put(TimingFilter.RESPONSE_TIME_MILLISECONDS, 1L);
        ((RestAssuredResponseImpl) response).setFilterContextProperties(properties);
        assertThatThrownBy(() -> spec.validate(response))
                .hasMessage("1 expectation failed.\nExpected header \"X-Other\" was not \"x\", was \"null\". Headers are:\n" + HEADERS + "\n");
    }

    @Test
    void then_spec_validates_the_merged_specification() {
        ResponseSpecification other = new ResponseSpecBuilder().expectStatusCode(201).expectBody("f", equalTo("h")).build();

        then().spec(new ResponseSpecBuilder().expectStatusCode(200).rootPath("a").expectBody("b", equalTo(1)).build());
        assertThatThrownBy(() -> then().spec(other))
                .hasMessage("2 expectations failed.\nExpected status code <201> but was <200>.\n\n"
                        + "JSON path f doesn't match.\nExpected: h\n  Actual: g\n");
        assertThatThrownBy(() -> given().filter(respondWith(jsonResponse())).expect().spec(other).when().get(URL))
                .hasMessage("2 expectations failed.\nExpected status code <201> but was <200>.\n\n"
                        + "JSON path f doesn't match.\nExpected: h\n  Actual: g\n");
        ResponseSpecificationImpl spec = spec();
        assertThat(spec.specification(other)).isSameAs(spec);
        assertThat(spec.getStatusCode().matches(201)).isTrue();
    }

    @Test
    void spec_only_merges_specifications_created_by_rest_assured() {
        // The Groovy version failed with groovy.lang.MissingMethodException
        ResponseSpecification other = (ResponseSpecification) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{ResponseSpecification.class}, (proxy, method, args) -> null);

        assertThatThrownBy(() -> spec().spec(other))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Cannot merge a response specification of type " + other.getClass().getName())
                .hasMessageEndingWith(", it must be of type io.restassured.internal.ResponseSpecificationImpl.");
        assertThatThrownBy(() -> spec().spec(null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Specification to merge with cannot be null");
    }

    @Test
    void default_spec_passed_to_the_constructor_is_merged() {
        ResponseSpecification defaultSpec = new ResponseSpecBuilder().expectStatusCode(201).rootPath("root").build();
        ResponseSpecificationImpl spec = new ResponseSpecificationImpl("ignored", defaultSpec, new ResponseParserRegistrar(), RestAssuredConfig.config(), new LogRepository());

        assertThat(spec.getStatusCode().matches(201)).isTrue();
        assertThat(spec.getRootPath()).isEqualTo("root");
        assertThatThrownBy(() -> new ResponseSpecificationImpl("", null, new ResponseParserRegistrar(), null, new LogRepository()))
                .isExactlyInstanceOf(NullPointerException.class)
                .hasMessage("RestAssuredConfig cannot be null");
    }

    // Accessors and syntactic sugar

    @Test
    void accessors_and_syntactic_sugar() {
        ResponseParserRegistrar rpr = new ResponseParserRegistrar();
        RestAssuredConfig config = RestAssuredConfig.config();
        LogRepository logRepository = new LogRepository();
        ResponseSpecificationImpl spec = new ResponseSpecificationImpl("", null, rpr, config, logRepository);

        assertThat(spec.getRpr()).isSameAs(rpr);
        assertThat(spec.getConfig()).isSameAs(config);
        assertThat(spec.getLogRepository()).isSameAs(logRepository);
        assertThat(spec.getStatusCode()).isNull();
        assertThat(spec.getStatusLine()).isNull();
        assertThat(spec.getLogDetail()).isNull();
        assertThat(spec.hasAssertionsDefined()).isFalse();
        assertThat(spec.hasHeaderAssertions()).isFalse();
        assertThat(spec.hasCookieAssertions()).isFalse();
        assertThat(spec.hasBodyAssertionsDefined()).isFalse();
        assertThat(spec.and()).isSameAs(spec);
        assertThat(spec.then()).isSameAs(spec);
        assertThat(spec.expect()).isSameAs(spec);
        assertThat(spec.that()).isSameAs(spec);
        assertThat(spec.response()).isSameAs(spec);
        // Without a request specification, as when built by ResponseSpecBuilder
        assertThatThrownBy(spec::given).isInstanceOf(IllegalStateException.class).hasMessageStartingWith("A response specification built by ResponseSpecBuilder");
        assertThatThrownBy(spec::when).isInstanceOf(IllegalStateException.class).hasMessageStartingWith("A response specification built by ResponseSpecBuilder");
        assertThatThrownBy(spec::request).isInstanceOf(IllegalStateException.class).hasMessageStartingWith("A response specification built by ResponseSpecBuilder");
        assertThatThrownBy(spec::with).isInstanceOf(IllegalStateException.class).hasMessageStartingWith("A response specification built by ResponseSpecBuilder");
        RequestSpecification requestSpecification = given();
        spec.setRequestSpecification(requestSpecification);
        assertThat(spec.given()).isSameAs(requestSpecification);
        assertThat(spec.when()).isSameAs(requestSpecification);
        assertThat(spec.request()).isSameAs(requestSpecification);
        assertThat(spec.with()).isSameAs(requestSpecification);
        assertThat(spec.logDetail(LogDetail.BODY)).isSameAs(spec);
        assertThat(spec.getLogDetail()).isEqualTo(LogDetail.BODY);
        assertThat(spec.log()).isInstanceOf(ResponseLogSpecificationImpl.class);
        assertThat(spec.forceDisableEagerAssert()).isSameAs(spec);

        spec.contentType(ContentType.JSON);
        assertThat(spec.hasAssertionsDefined()).isTrue();

        RestAssuredConfig otherConfig = RestAssuredConfig.newConfig();
        LogRepository otherLogRepository = new LogRepository();
        ResponseParserRegistrar otherRpr = new ResponseParserRegistrar();
        spec.setConfig(otherConfig);
        spec.setLogRepository(otherLogRepository);
        spec.setRpr(otherRpr);
        assertThat(spec.getConfig()).isSameAs(otherConfig);
        assertThat(spec.getLogRepository()).isSameAs(otherLogRepository);
        assertThat(spec.getRpr()).isSameAs(otherRpr);

        RequestSpecification request = given();
        assertThat(request.response().given()).isSameAs(request);
        assertThat(request.response().when()).isSameAs(request);
        assertThat(request.response().request()).isSameAs(request);
    }

    @Test
    void every_expectation_kind_counts_as_an_assertion() {
        assertThat(spec().statusCode(200)).matches(s -> ((ResponseSpecificationImpl) s).hasAssertionsDefined());
        assertThat(spec().statusLine("x")).matches(s -> ((ResponseSpecificationImpl) s).hasAssertionsDefined());
        assertThat(spec().header("x", "y")).matches(s -> ((ResponseSpecificationImpl) s).hasHeaderAssertions() && ((ResponseSpecificationImpl) s).hasAssertionsDefined());
        assertThat(spec().cookie("x")).matches(s -> ((ResponseSpecificationImpl) s).hasCookieAssertions() && ((ResponseSpecificationImpl) s).hasAssertionsDefined());
        assertThat(spec().contentType("x")).matches(s -> ((ResponseSpecificationImpl) s).hasAssertionsDefined());
        assertThat(spec().time(lessThan(1L))).matches(s -> ((ResponseSpecificationImpl) s).hasAssertionsDefined());
        assertThat(spec().body("a", anything())).matches(s -> ((ResponseSpecificationImpl) s).hasBodyAssertionsDefined() && ((ResponseSpecificationImpl) s).hasAssertionsDefined());
    }

    private static class CountingMatcher<T> extends BaseMatcher<T> {
        final AtomicInteger count = new AtomicInteger();
        private final boolean result;
        private final Runnable onMatch;

        CountingMatcher(boolean result) {
            this(result, () -> {
            });
        }

        CountingMatcher(boolean result, Runnable onMatch) {
            this.result = result;
            this.onMatch = onMatch;
        }

        @Override
        public boolean matches(Object actual) {
            count.incrementAndGet();
            onMatch.run();
            return result;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("counting");
        }
    }

    private static class LastFilter implements OrderedFilter {
        private final ResponseBuilder builder;

        LastFilter(ResponseBuilder builder) {
            this.builder = builder;
        }

        @Override
        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
            // Like a response that was sent: it carries the config of the request and a log repository
            RestAssuredResponseImpl response = (RestAssuredResponseImpl) builder.build();
            response.setConfig(requestSpec.getConfig());
            response.setLogRepository(new LogRepository());
            return response;
        }

        @Override
        public int getOrder() {
            return LOWEST_PRECEDENCE;
        }
    }
}
