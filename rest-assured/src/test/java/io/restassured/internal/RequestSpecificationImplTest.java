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
import io.restassured.authentication.BasicAuthScheme;
import io.restassured.authentication.FormAuthConfig;
import io.restassured.authentication.NoAuthScheme;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.builder.ResponseBuilder;
import io.restassured.config.EncoderConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SessionConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.filter.time.TimingFilter;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.internal.filter.CsrfFilter;
import io.restassured.internal.filter.FormAuthFilter;
import io.restassured.internal.log.LogRepository;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.MultiPartSpecification;
import io.restassured.specification.ProxySpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.spi.AuthFilter;
import org.codehaus.groovy.runtime.GStringImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Characterizes {@link RequestSpecificationImpl} without sending requests: a capturing filter (first in the chain unless a
 * test says otherwise) records the request specification and returns a canned response. Covers path, query, form and
 * request parameters, headers and cookies, content-type and charset, base URI, base path and port resolution, multipart
 * definitions, bodies, proxy, auth selection, filter ordering, logging and the error messages of invalid requests.
 */
class RequestSpecificationImplTest {

    @AfterEach
    void resetRestAssured() {
        RestAssured.reset();
    }

    static final class Capture implements OrderedFilter {
        int order = OrderedFilter.DEFAULT_PRECEDENCE;
        FilterableRequestSpecification request;
        FilterContext context;
        List<Filter> filters;
        Consumer<FilterableRequestSpecification> action = r -> {
        };

        @Override
        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
            action.accept(requestSpec);
            request = requestSpec;
            context = ctx;
            filters = new ArrayList<>(requestSpec.getDefinedFilters());
            return new ResponseBuilder().setStatusCode(200).setBody("").build();
        }

        @Override
        public int getOrder() {
            return order;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, Object> objects(Map<String, String> map) {
        return new LinkedHashMap<>((Map) map);
    }

    private static Capture capture() {
        return new Capture();
    }

    // Path parameters

    @Test
    void unnamed_path_params_are_applied_in_order_and_url_encoded() {
        Capture capture = capture();

        given().filter(capture).get("/x/{a}/{b}", "v 1", 2);

        FilterableRequestSpecification r = capture.request;
        assertThat(r.getURI()).isEqualTo("http://localhost:8080/x/v%201/2");
        assertThat(r.getDerivedPath()).isEqualTo("/x/v%201/2");
        assertThat(r.getUserDefinedPath()).isEqualTo("/x/{a}/{b}");
        assertThat(r.getMethod()).isEqualTo("GET");
        assertThat(r.getPathParams()).containsExactly(Map.entry("a", "v 1"), Map.entry("b", "2"));
        assertThat(r.getUnnamedPathParams()).containsExactly(Map.entry("a", "v 1"), Map.entry("b", "2"));
        assertThat(r.getUnnamedPathParamValues()).containsExactly("v 1", "2");
        assertThat(r.getNamedPathParams()).isEmpty();
        assertThat(r.getPathParamPlaceholders()).containsExactly("a", "b");
        assertThat(r.getUndefinedPathParamPlaceholders()).isEmpty();
    }

    @Test
    void named_path_params_have_precedence_and_unnamed_ones_fill_the_remaining_placeholders_including_the_query() {
        Capture capture = capture();

        given().filter(capture).pathParam("b", "B").get("/{a}/{b}/{c}?x={d}", "A", "C", "D");

        FilterableRequestSpecification r = capture.request;
        assertThat(r.getURI()).isEqualTo("http://localhost:8080/A/B/C?x=D");
        assertThat(r.getDerivedPath()).isEqualTo("/A/B/C");
        assertThat(r.getPathParams()).containsExactly(Map.entry("b", "B"), Map.entry("a", "A"), Map.entry("c", "C"), Map.entry("d", "D"));
        assertThat(r.getNamedPathParams()).containsExactly(Map.entry("b", "B"));
        assertThat(r.getUnnamedPathParams()).containsExactly(Map.entry("a", "A"), Map.entry("c", "C"), Map.entry("d", "D"));
        assertThat(r.getPathParamPlaceholders()).containsExactly("a", "b", "c", "d");
    }

    @Test
    void a_named_path_param_fills_every_placeholder_with_its_name() {
        Capture capture = capture();

        given().filter(capture).pathParams("a", 1, "b", "x/y").get("/{a}/{b}/{a}");

        assertThat(capture.request.getURI()).isEqualTo("http://localhost:8080/1/x%2Fy/1");
        assertThat(capture.request.getPathParamPlaceholders()).containsExactly("a", "b");
    }

    @Test
    void a_list_valued_named_path_param_always_uses_its_second_element() {
        // Quirk: the usage count is stored before it's incremented, so the index is always 1
        Capture capture = capture();

        given().filter(capture).pathParams(Collections.singletonMap("a", Arrays.asList("x", "y", "z"))).get("/{a}/{a}");

        assertThat(capture.request.getURI()).isEqualTo("http://localhost:8080/y/y");

        Throwable thrown = catchThrowable(() -> given().filter(capture()).pathParams(Collections.singletonMap("a", Collections.singletonList("x"))).get("/{a}"));
        assertThat(thrown).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void path_params_given_as_map_to_the_http_method() {
        Capture capture = capture();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("a", "1");
        params.put("b", LocalDate.of(2020, 1, 2));

        given().filter(capture).get("/{a}/{b}", params);

        assertThat(capture.request.getURI()).isEqualTo("http://localhost:8080/1/2020-01-02");
        assertThat(capture.request.getNamedPathParams()).containsExactly(Map.entry("a", "1"), Map.entry("b", "2020-01-02"));
    }

    @Test
    void undefined_placeholders_are_left_in_place_and_reported() {
        Capture capture = capture();

        given().filter(capture).get("/{a}/{b}", "A");

        assertThat(capture.request.getURI()).isEqualTo("http://localhost:8080/A/%7Bb%7D");
        assertThat(capture.request.getUndefinedPathParamPlaceholders()).containsExactly("b");
    }

    @Test
    void sending_with_undefined_path_params_fails_with_a_message() {
        assertThatThrownBy(() -> given().get("/{a}/{b}", "A"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid number of path parameters. Expected 2, was 1. Undefined path parameters are: b.");
    }

    @Test
    void sending_with_redundant_path_params_fails_with_a_message() {
        assertThatThrownBy(() -> given().pathParam("z", "Z").get("/{a}", "A", "B"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid number of path parameters. Expected 1, was 3. Redundant path parameters are: z=Z and B.");
        assertThatThrownBy(() -> given().pathParam("z", "Z").pathParam("a", "A").get("/{a}"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid number of path parameters. Expected 1, was 2. Redundant path parameters are: z=Z.");
        assertThatThrownBy(() -> given().get("/{a}", "A", "B", "C"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid number of path parameters. Expected 1, was 3. Redundant path parameters are: B, C.");
    }

    @Test
    void redundant_list_valued_named_path_params_are_reported_with_the_java_rendering_of_the_entry() {
        assertThatThrownBy(() -> given().pathParams(Collections.singletonMap("z", Arrays.asList("x", "y"))).get("/{a}", "A"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid number of path parameters. Expected 1, was 2. Redundant path parameters are: z=[x, y].");
    }

    @Test
    void null_unnamed_path_params_are_rejected() {
        assertThatThrownBy(() -> given().get("/{a}/{b}", "x", null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unnamed path parameter cannot be null (path parameter at index 1 is null)");
        assertThatThrownBy(() -> given().get("/{a}/{b}/{c}", null, "b", null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unnamed path parameter cannot be null (path parameters at indices 0,2 are null)");
    }

    @Test
    void a_removed_unnamed_path_param_is_shown_as_null_by_the_uri_getters() {
        Capture capture = capture();
        capture.action = r -> r.removeUnnamedPathParam("a");

        given().filter(capture).get("/{a}/{b}", "1", "2");

        FilterableRequestSpecification r = capture.request;
        assertThat(r.getURI()).isEqualTo("http://localhost:8080/null/2");
        assertThat(r.getDerivedPath()).isEqualTo("/null/2");
        assertThat(r.getUnnamedPathParams()).containsExactly(Map.entry("b", "2"));
        assertThat(r.getUnnamedPathParamValues()).containsExactly("2");
        assertThat(r.getUndefinedPathParamPlaceholders()).isEmpty();
    }

    @Test
    void unnamed_path_params_can_be_removed_by_value_and_named_ones_by_name() {
        Capture capture = capture();
        capture.action = r -> r.removeUnnamedPathParamByValue("2").removePathParam("n");

        given().filter(capture).pathParam("n", "N").get("/{a}/{b}/{n}", "1", "2");

        FilterableRequestSpecification r = capture.request;
        assertThat(r.getURI()).isEqualTo("http://localhost:8080/1/null/%7Bn%7D");
        assertThat(r.getNamedPathParams()).isEmpty();
        assertThat(r.getUnnamedPathParams()).containsExactly(Map.entry("a", "1"));
    }

    @Test
    void double_slashes_in_the_path_are_kept_and_encoded() {
        Capture capture = capture();

        given().filter(capture).get("/a//{b}/c", "x");

        assertThat(capture.request.getURI()).isEqualTo("http://localhost:8080/a%2F%2Fx/c");
    }

    @Test
    void url_encoding_can_be_disabled() {
        Capture capture = capture();

        given().filter(capture).urlEncodingEnabled(false).queryParam("q", "a%20b").get("/x/{a}", "v%201");

        assertThat(capture.request.getURI()).isEqualTo("http://localhost:8080/x/v%201?q=a%20b");
        // A path that isn't a valid URI fails with the checked URISyntaxException
        assertThatThrownBy(() -> given().filter(capture()).urlEncodingEnabled(false).get("/x/{a}", "v 1"))
                .isExactlyInstanceOf(java.net.URISyntaxException.class)
                .hasMessage("Illegal character in path at index 25: http://localhost:8080/x/v 1");
    }

    @Test
    void a_path_ending_with_a_question_mark_is_rejected() {
        assertThatThrownBy(() -> given().get("/x?"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Request URI cannot end with ?");
    }

    @Test
    void path_params_after_a_trailing_slash_keep_the_slash() {
        Capture capture = capture();

        given().filter(capture).get("/x/{a}/", "y");

        assertThat(capture.request.getURI()).isEqualTo("http://localhost:8080/x/y/");
    }

    // Base URI, base path and port

    @Test
    void fully_qualified_url_uses_the_explicit_port_but_ignores_the_base_path() {
        Capture capture = capture();

        given().filter(capture).port(1234).basePath("/bp").get("http://example.com/x/{a}", "y");

        assertThat(capture.request.getURI()).isEqualTo("http://example.com:1234/x/y");
        assertThat(capture.request.getPort()).isEqualTo(1234);
        assertThat(capture.request.getBasePath()).isEqualTo("/bp");
        assertThat(capture.request.getBaseUri()).isEqualTo("http://localhost");
    }

    @Test
    void fully_qualified_https_url_without_port_uses_the_default_port() {
        Capture capture = capture();

        given().filter(capture).get("https://example.com/x");

        assertThat(capture.request.getURI()).isEqualTo("https://example.com/x");
        assertThat(capture.request.getPort()).isEqualTo(-1);
    }

    @Test
    void fully_qualified_localhost_url_without_port_uses_8080() {
        Capture capture = capture();

        given().filter(capture).get("http://localhost/x");

        assertThat(capture.request.getURI()).isEqualTo("http://localhost:8080/x");
        assertThat(capture.request.getPort()).isEqualTo(8080);
    }

    @Test
    void fully_qualified_non_localhost_url_without_port_has_no_port() {
        Capture capture = capture();

        given().filter(capture).get("http://example.com/x");

        assertThat(capture.request.getURI()).isEqualTo("http://example.com/x");
        assertThat(capture.request.getPort()).isEqualTo(-1);
    }

    @Test
    void base_uri_path_base_path_and_path_are_merged() {
        Capture capture = capture();

        given().filter(capture).baseUri("http://h:99/base").basePath("bp/").get("/x");

        assertThat(capture.request.getURI()).isEqualTo("http://h:99/base/bp/x");
        assertThat(capture.request.getDerivedPath()).isEqualTo("/base/bp/x");
        assertThat(capture.request.getPort()).isEqualTo(99);
    }

    @Test
    void base_uri_without_port_has_no_port_and_path_without_slash_is_appended() {
        Capture capture = capture();

        given().filter(capture).baseUri("http://h/base").basePath("bp/").get("x");

        assertThat(capture.request.getURI()).isEqualTo("http://h/base/bp/x");
        assertThat(capture.request.getPort()).isEqualTo(-1);
    }

    @Test
    void static_base_uri_port_and_base_path_are_used() {
        RestAssured.baseURI = "http://example.org";
        RestAssured.port = 7000;
        RestAssured.basePath = "/api";
        Capture capture = capture();

        given().filter(capture).get("/x");

        assertThat(capture.request.getURI()).isEqualTo("http://example.org:7000/api/x");
    }

    @Test
    void port_must_be_positive_unless_undefined() {
        assertThatThrownBy(() -> given().port(0))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Port must be greater than 0");
        Capture capture = capture();

        given().filter(capture).baseUri("http://h").port(RestAssured.UNDEFINED_PORT).get("/x");

        assertThat(capture.request.getURI()).isEqualTo("http://h/x");
    }

    @Test
    void base_uri_without_scheme_fails_with_the_checked_url_exception() {
        Capture capture = capture();

        given().filter(capture).baseUri("h").port(-1).get("x");

        assertThat(capture.request.getURI()).isEqualTo("h/h/x/h/h/x");
        assertThat(catchThrowable(capture.request::getPort)).isExactlyInstanceOf(java.net.MalformedURLException.class).hasMessage("no protocol: h");
    }

    // Query, request and form parameters

    @Test
    void get_puts_query_params_from_the_path_first_then_request_query_and_form_params_in_the_uri() {
        Capture capture = capture();

        given().filter(capture).formParam("f", "1").queryParam("q", 1, 2).param("p", "x y").get("/x?z=1&w&e=");

        assertThat(capture.request.getURI()).isEqualTo("http://localhost:8080/x?z=1&w&e=&p=x%20y&q=1&q=2&f=1");
        assertThat(objects(capture.request.getQueryParams())).containsExactly(Map.entry("q", Arrays.asList("1", "2")));
        assertThat(capture.request.getRequestParams()).containsExactly(Map.entry("p", "x y"));
        assertThat(capture.request.getFormParams()).containsExactly(Map.entry("f", "1"));
    }

    @Test
    void post_leaves_request_and_form_params_out_of_the_uri() {
        Capture capture = capture();

        given().filter(capture).formParam("f", "1").queryParam("q", "2").param("p", "3").post("/x");

        assertThat(capture.request.getURI()).isEqualTo("http://localhost:8080/x?q=2");
    }

    @Test
    void put_puts_request_params_in_the_uri() {
        Capture capture = capture();

        given().filter(capture).formParam("f", "1").param("p", "3").put("/x");

        assertThat(capture.request.getURI()).isEqualTo("http://localhost:8080/x?p=3");
    }

    @Test
    void params_without_value_and_collection_params() {
        Capture capture = capture();

        given().filter(capture).queryParam("a").queryParam("b", Arrays.asList("1", "2")).param("c", Collections.emptyList())
                .queryParams("d", 1, "e", LocalDate.of(2020, 1, 2)).get("/x");

        assertThat(capture.request.getURI()).isEqualTo("http://localhost:8080/x?c&a&b=1&b=2&d=1&e=2020-01-02");
        assertThat(objects(capture.request.getQueryParams()).get("a")).isInstanceOf(NoParameterValue.class);
    }

    @Test
    void params_can_be_removed() {
        Capture capture = capture();
        capture.action = r -> r.removeParam("p").removeQueryParam("q").removeFormParam("f");

        given().filter(capture).param("p", 1).queryParam("q", 2).formParam("f", 3).formParams(Map.of("g", 4)).get("/x");

        assertThat(capture.request.getRequestParams()).isEmpty();
        assertThat(capture.request.getQueryParams()).isEmpty();
        assertThat(capture.request.getFormParams()).containsExactly(Map.entry("g", "4"));
    }

    @Test
    void param_update_strategies_from_the_config_are_used() {
        Capture capture = capture();

        given().filter(capture).config(config().paramConfig(io.restassured.config.ParamConfig.paramConfig().queryParamsUpdateStrategy(io.restassured.config.ParamConfig.UpdateStrategy.REPLACE)))
                .queryParam("q", "1").queryParam("q", "2").param("p", "1").param("p", "2").get("/x");

        assertThat(capture.request.getQueryParams()).containsExactly(Map.entry("q", "2"));
        assertThat(objects(capture.request.getRequestParams())).containsExactly(Map.entry("p", Arrays.asList("1", "2")));
    }

    @Test
    void illegal_query_params_in_the_path_are_reported_with_the_groovy_rendering_of_the_split_array() {
        assertThatThrownBy(() -> given().get("/x?a=1&&b=2"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Illegal parameters passed to REST Assured. Parameters was: [a=1, , b=2]");
    }

    @Test
    void form_params_and_body_cannot_both_be_sent() {
        assertThatThrownBy(() -> given().formParam("a", 1).body("x").post("/x"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("You can either send form parameters OR body content in POST, not both!");
        assertThatThrownBy(() -> given().param("a", 1).formParam("b", 2).body("x").request("purge", "/x"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("You can either send form parameters OR body content in PURGE, not both!");
    }

    // Headers and cookies

    @Test
    void accept_header_defaults_to_any() {
        Capture capture = capture();

        given().filter(capture).get("/x");

        assertThat(capture.request.getHeaders().asList()).extracting(Header::toString).containsExactly("Accept=*/*");
    }

    @Test
    void headers_with_the_same_name_are_kept_except_content_type_and_accept_which_are_overwritten() {
        Capture capture = capture();

        given().filter(capture).header("x", "1").header("X", "2", 3).accept(ContentType.JSON).accept("text/plain")
                .contentType(ContentType.XML).contentType("text/html").get("/x");

        assertThat(capture.request.getHeaders().asList()).extracting(Header::toString)
                .containsExactly("x=1", "X=2", "X=3", "Accept=text/plain", "Content-Type=text/html; charset=ISO-8859-1");
    }

    @Test
    void header_maps_with_list_values_give_one_header_per_value_and_null_values_become_the_string_null() {
        Capture capture = capture();
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("a", Arrays.asList("1", 2));
        headers.put("b", null);
        headers.put("c", LocalDate.of(2020, 1, 2));

        given().filter(capture).headers(headers).header("d", "x", (Object) null).headers("e", "1", "e", "2").get("/x");

        assertThat(capture.request.getHeaders().asList()).extracting(Header::toString)
                .containsExactly("a=1", "a=2", "b=null", "c=2020-01-02", "d=x", "d=null", "e=1", "e=2", "Accept=*/*");
    }

    @Test
    void header_map_keys_that_are_gstrings_are_converted_to_strings() {
        Capture capture = capture();
        Map<Object, Object> headers = new LinkedHashMap<>();
        headers.put(new GStringImpl(new Object[]{"b"}, new String[]{"a", ""}), "1");

        given().filter(capture).headers((Map) headers).get("/x");

        assertThat(capture.request.getHeaders().getValue("ab")).isEqualTo("1");
    }

    @Test
    void headers_object_and_header_object_are_merged() {
        Capture capture = capture();

        given().filter(capture).headers(new Headers(new Header("a", "1"))).header(new Header("a", "2")).headers(new Headers()).get("/x");

        assertThat(capture.request.getHeaders().asList()).extracting(Header::toString).containsExactly("a=1", "a=2", "Accept=*/*");
    }

    @Test
    void headers_can_be_replaced_and_removed_ignoring_case() {
        Capture capture = capture();
        capture.action = r -> r.replaceHeader("A", "3").removeHeader("b");

        given().filter(capture).header("a", "1").header("a", "2").header("B", "x").header("c", "y").get("/x");

        assertThat(capture.request.getHeaders().asList()).extracting(Header::toString).containsExactly("c=y", "Accept=*/*", "A=3");

        Capture other = capture();
        other.action = r -> r.replaceHeaders(new Headers(new Header("z", "1")));
        given().filter(other).header("a", "1").get("/x");
        assertThat(other.request.getHeaders().asList()).extracting(Header::toString).containsExactly("z=1");

        Capture removed = capture();
        removed.action = FilterableRequestSpecification::removeHeaders;
        given().filter(removed).header("a", "1").get("/x");
        assertThat(removed.request.getHeaders().exist()).isFalse();
    }

    @Test
    void no_content_type_removes_the_content_type_header() {
        Capture capture = capture();

        given().filter(capture).contentType(ContentType.JSON).noContentType().body("x").put("/x");

        assertThat(capture.request.getContentType()).isEqualTo("text/plain; charset=ISO-8859-1");
    }

    @Test
    void cookies_are_merged_and_a_cookie_without_value_gets_the_string_null() {
        Capture capture = capture();

        given().filter(capture).cookie("a").cookie("b", "1", 2).cookie(new Cookie.Builder("c", "3").build())
                .cookies("d", "4", "e", "5").cookies(new Cookies(new Cookie.Builder("f").build())).get("/x");

        List<Cookie> cookies = capture.request.getCookies().asList();
        assertThat(cookies).extracting(Cookie::getName).containsExactly("a", "b", "b", "c", "d", "e", "f");
        assertThat(cookies).extracting(Cookie::getValue).containsExactly("null", "1", "2", "3", "4", "5", null);
    }

    @Test
    void cookie_maps_with_values_that_are_not_strings() {
        // The Groovy implementation failed with "GroovyRuntimeException: Could not find matching constructor for:
        // io.restassured.http.Cookie$Builder(String, Integer)" here
        Capture capture = capture();
        Map<String, Object> cookies = new LinkedHashMap<>();
        cookies.put("b", 2L);
        cookies.put("c", new GStringImpl(new Object[]{"x"}, new String[]{"v-", ""}));

        given().filter(capture).cookies("a", 1).cookies(cookies).get("/x");

        List<Cookie> captured = capture.request.getCookies().asList();
        assertThat(captured).extracting(Cookie::getName).containsExactly("a", "b", "c");
        assertThat(captured).extracting(Cookie::getValue).containsExactly("1", "2", "v-x");
    }

    @Test
    void cookie_maps_keep_null_values() {
        Capture capture = capture();
        Map<String, Object> cookies = new LinkedHashMap<>();
        cookies.put("a", null);
        cookies.put("b", "x");

        given().filter(capture).cookies(cookies).get("/x");

        assertThat(capture.request.getCookies().asList()).extracting(Cookie::getValue).containsExactly(null, "x");
    }

    @Test
    void cookies_can_be_replaced_and_removed_ignoring_case() {
        Capture capture = capture();
        capture.action = r -> r.replaceCookie("A", "2").removeCookie("b").replaceCookie(new Cookie.Builder("c", "4").build()).removeCookie(new Cookie.Builder("d").build());

        given().filter(capture).cookie("a", "1").cookie("B", "x").cookie("c", "3").cookie("D", "y").cookie("e", "z").get("/x");

        assertThat(capture.request.getCookies().asList()).extracting(Cookie::toString).containsExactly("e=z", "A=2", "c=4");

        Capture other = capture();
        other.action = r -> r.replaceCookies(new Cookies(new Cookie.Builder("z", "1").build()));
        given().filter(other).cookie("a", "1").get("/x");
        assertThat(other.request.getCookies().asList()).extracting(Cookie::toString).containsExactly("z=1");

        Capture removed = capture();
        removed.action = FilterableRequestSpecification::removeCookies;
        given().filter(removed).cookie("a", "1").get("/x");
        assertThat(removed.request.getCookies().exist()).isFalse();
    }

    @Test
    void session_id_replaces_an_existing_cookie_with_the_session_id_name_ignoring_case() {
        Capture capture = capture();

        given().filter(capture).cookie("jsessionid", "old").cookie("x", "1").sessionId("new").get("/x");

        assertThat(capture.request.getCookies().asList()).extracting(Cookie::toString).containsExactly("x=1", "JSESSIONID=new");

        Capture other = capture();
        given().filter(other).config(config().sessionConfig(new SessionConfig().sessionIdName("sid"))).sessionId("v").get("/x");
        assertThat(other.request.getCookies().asList()).extracting(Cookie::toString).containsExactly("sid=v");
    }

    @Test
    void session_id_value_from_the_session_config_is_not_a_cookie_until_sent() {
        Capture capture = capture();

        given().filter(capture).config(config().sessionConfig(new SessionConfig().sessionIdValue("v"))).get("/x");

        assertThat(capture.request.getCookies().exist()).isFalse();
    }

    // Content-type and charset

    @Test
    void form_params_on_post_and_get_give_url_encoded_content_type_with_the_default_charset() {
        Capture post = capture();
        given().filter(post).formParam("a", "1").post("/x");
        assertThat(post.request.getContentType()).isEqualTo("application/x-www-form-urlencoded; charset=ISO-8859-1");

        Capture get = capture();
        given().filter(get).formParam("a", "1").get("/x");
        assertThat(get.request.getContentType()).isEqualTo("application/x-www-form-urlencoded; charset=ISO-8859-1");

        Capture emptyPost = capture();
        given().filter(emptyPost).post("/x");
        assertThat(emptyPost.request.getContentType()).isEqualTo("application/x-www-form-urlencoded; charset=ISO-8859-1");
    }

    @Test
    void content_type_is_derived_from_the_body() {
        Capture bytes = capture();
        given().filter(bytes).body(new byte[]{1}).put("/x");
        assertThat(bytes.request.getContentType()).isEqualTo("application/octet-stream; charset=ISO-8859-1");

        Capture stream = capture();
        given().filter(stream).body(new ByteArrayInputStream(new byte[]{1})).put("/x");
        assertThat(stream.request.getContentType()).isEqualTo("application/octet-stream; charset=ISO-8859-1");

        Capture text = capture();
        given().filter(text).body("x").put("/x");
        assertThat(text.request.getContentType()).isEqualTo("text/plain; charset=ISO-8859-1");

        Capture none = capture();
        given().filter(none).delete("/x");
        assertThat(none.request.getContentType()).isNull();
    }

    @Test
    void multipart_content_type_gets_no_charset() {
        Capture capture = capture();

        given().filter(capture).multiPart("a", "b").put("/x");

        assertThat(capture.request.getContentType()).isEqualTo("multipart/form-data");

        Capture mixed = capture();
        given().filter(mixed).config(config().multiPartConfig(io.restassured.config.MultiPartConfig.multiPartConfig().defaultSubtype("mixed"))).multiPart("a", "b").put("/x");
        assertThat(mixed.request.getContentType()).isEqualTo("multipart/mixed");
    }

    @Test
    void json_content_type_gets_no_charset_unless_configured() {
        Capture plain = capture();
        given().filter(plain).contentType("application/json").body("x").put("/x");
        assertThat(plain.request.getContentType()).isEqualTo("application/json");

        Capture configured = capture();
        given().filter(configured).config(config().encoderConfig(encoderConfig().defaultCharsetForContentType("UTF-16", "application/json")))
                .contentType(ContentType.JSON).body("x").put("/x");
        assertThat(configured.request.getContentType()).isEqualTo("application/json; charset=UTF-16");

        Capture userUtf8 = capture();
        given().filter(userUtf8).config(config().encoderConfig(encoderConfig().defaultContentCharset("UTF-16")))
                .contentType("application/json").body("x").put("/x");
        assertThat(userUtf8.request.getContentType()).isEqualTo("application/json; charset=UTF-8");
    }

    @Test
    void content_type_with_charset_is_left_alone_and_charset_appending_can_be_disabled() {
        Capture explicit = capture();
        given().filter(explicit).contentType("text/plain; charset=UTF-8").body("x").put("/x");
        assertThat(explicit.request.getContentType()).isEqualTo("text/plain; charset=UTF-8");

        Capture disabled = capture();
        given().filter(disabled).config(config().encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
                .body("x").put("/x");
        assertThat(disabled.request.getContentType()).isEqualTo("text/plain");
    }

    // Bodies

    @Test
    void bodies_of_every_kind() {
        Capture text = capture();
        given().filter(text).body("x").put("/x");
        assertThat((Object) text.request.getBody()).isEqualTo("x");

        Capture number = capture();
        given().filter(number).body((Object) 12).put("/x");
        assertThat((Object) number.request.getBody()).isEqualTo("12");

        Capture gstring = capture();
        given().filter(gstring).body((Object) new GStringImpl(new Object[]{1}, new String[]{"a", ""})).put("/x");
        assertThat((Object) gstring.request.getBody()).isEqualTo("a1");

        Capture map = capture();
        given().filter(map).contentType(ContentType.JSON).body(Map.of("a", 1)).put("/x");
        assertThat((Object) map.request.getBody()).isEqualTo("{\"a\":1}");

        Capture mapperType = capture();
        given().filter(mapperType).body(Map.of("a", 1), ObjectMapperType.GSON).put("/x");
        assertThat((Object) mapperType.request.getBody()).isEqualTo("{\"a\":1}");

        byte[] bytes = {1, 2};
        Capture binary = capture();
        given().filter(binary).body(bytes).put("/x");
        assertThat((Object) binary.request.getBody()).isSameAs(bytes);

        File file = new File("x.txt");
        Capture fileBody = capture();
        given().filter(fileBody).body(file).put("/x");
        assertThat((Object) fileBody.request.getBody()).isSameAs(file);
    }

    @Test
    void body_with_object_mapper_gets_the_content_type_and_charset() {
        Capture capture = capture();
        List<String> seen = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper() {
            @Override
            public Object deserialize(ObjectMapperDeserializationContext context) {
                return null;
            }

            @Override
            public Object serialize(ObjectMapperSerializationContext context) {
                seen.add(context.getContentType() + "|" + context.getCharset() + "|" + context.getObjectToSerialize());
                return 42;
            }
        };

        given().filter(capture).contentType("application/xml").body("obj", mapper).put("/x");

        assertThat(seen).containsExactly("application/xml|ISO-8859-1|obj");
        assertThat((Object) capture.request.getBody()).isEqualTo(42);
    }

    // Multipart

    @Test
    void multipart_definitions_of_every_overload() {
        Capture capture = capture();
        File file = new File("f.txt");
        byte[] bytes = {1};
        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);

        given().filter(capture)
                .multiPart(file)
                .multiPart("c1", file)
                .multiPart("c2", file, "text/a")
                .multiPart("c3", Map.of("a", 1))
                .multiPart("c4", Map.of("a", 1), "application/json")
                .multiPart("c5", "name5", 7, "text/b")
                .multiPart("c6", "name6", bytes)
                .multiPart("c7", "name7", bytes, "text/c")
                .multiPart("c8", "name8", stream)
                .multiPart("c9", "name9", stream, "text/d")
                .multiPart("c10", "body")
                .multiPart("c11", "body", "text/e")
                .multiPart("c12", new NoParameterValue())
                .put("/x");

        List<MultiPartSpecification> parts = capture.request.getMultiPartParams();
        assertThat(parts).extracting(MultiPartSpecification::getControlName)
                .containsExactly("file", "c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8", "c9", "c10", "c11", "c12");
        assertThat(parts).extracting(MultiPartSpecification::getFileName)
                .containsExactly("f.txt", "f.txt", "f.txt", "file", "file", "name5", "name6", "name7", "name8", "name9", "file", "file", "file");
        // The mime type of a part without one is derived from its content
        assertThat(parts).extracting(MultiPartSpecification::getMimeType)
                .containsExactly("application/octet-stream", "application/octet-stream", "text/a", "text/plain", "application/json", "text/b",
                        "application/octet-stream", "text/c", "application/octet-stream", "text/d", "text/plain", "text/e", "text/plain");
        // NoParameterValue isn't a RequestSpecification overload, so Java calls multiPart(String, Object)
        assertThat(parts).extracting(MultiPartSpecification::getContent)
                .containsExactly(file, file, file, "{\"a\":1}", "{\"a\":1}", "7", bytes, bytes, stream, stream, "body", "body", "{}");
        assertThat(parts).allSatisfy(p -> assertThat(p.getHeaders()).isEmpty());
        assertThat(parts).allSatisfy(p -> assertThat(p.getCharset()).isNull());
    }

    @Test
    void multipart_with_an_object_that_is_a_file_or_a_string_uses_the_file_or_string_overload() {
        File file = new File("f.txt");
        Capture noContentType = capture();
        Capture json = capture();

        given().filter(noContentType).multiPart("a", (Object) file).multiPart("b", (Object) "text").put("/x");
        given().filter(json).contentType("application/json").multiPart("c", (Object) file).multiPart("d", (Object) "text").put("/x");

        // Groovy chose the overload by the runtime type, the String overload was ambiguous without a content type
        List<MultiPartSpecification> parts = new ArrayList<>(noContentType.request.getMultiPartParams());
        parts.addAll(json.request.getMultiPartParams());
        assertThat(parts).extracting(MultiPartSpecification::getFileName).containsExactly("f.txt", "file", "f.txt", "file");
        assertThat(parts).extracting(MultiPartSpecification::getMimeType)
                .containsExactly("application/octet-stream", "text/plain", "application/json", "application/json");
        assertThat(parts).extracting(MultiPartSpecification::getContent).containsExactly(file, "text", file, "text");
    }

    @Test
    void multipart_object_content_uses_the_request_content_type_unless_it_is_missing() {
        Capture capture = capture();

        given().filter(capture).contentType("application/vnd.x+json").multiPart("c", Map.of("a", 1)).multiPart(new MultiPartSpecBuilder(Map.of("b", 2)).build()).put("/x");

        List<MultiPartSpecification> parts = capture.request.getMultiPartParams();
        assertThat(parts).extracting(MultiPartSpecification::getMimeType).containsExactly("application/vnd.x+json", "application/vnd.x+json");
        assertThat(parts).extracting(MultiPartSpecification::getContent).containsExactly("{\"a\":1}", "{\"b\":2}");
    }

    @Test
    void multipart_spec_uses_the_default_control_and_file_name_unless_specified() {
        Capture capture = capture();

        given().filter(capture)
                .config(config().multiPartConfig(io.restassured.config.MultiPartConfig.multiPartConfig().defaultControlName("dc").defaultFileName("df")))
                .multiPart(new MultiPartSpecBuilder("x").build())
                .multiPart(new MultiPartSpecBuilder("y").controlName("c").fileName("f").mimeType("text/x").charset("UTF-8").header("h", "v").build())
                .multiPart(new MultiPartSpecBuilder(new byte[]{1}).build())
                .multiPart(new MultiPartSpecBuilder(Map.of("a", 1)).build())
                .put("/x");

        List<MultiPartSpecification> parts = capture.request.getMultiPartParams();
        assertThat(parts).extracting(MultiPartSpecification::getControlName).containsExactly("dc", "c", "dc", "dc");
        assertThat(parts).extracting(MultiPartSpecification::getFileName).containsExactly("df", "f", "df", "df");
        assertThat(parts).extracting(MultiPartSpecification::getMimeType).containsExactly("text/plain", "text/x", "application/octet-stream", "text/plain");
        assertThat(parts).extracting(MultiPartSpecification::getCharset).containsExactly(null, "UTF-8", null, null);
        assertThat(parts.get(1).getHeaders()).containsExactly(Map.entry("h", "v"));
        assertThat(parts.get(0).getContent()).isEqualTo("x");
        assertThat(parts.get(3).getContent()).isEqualTo("{\"a\":1}");
    }

    @Test
    void a_custom_multipart_specification_keeps_its_control_name_and_null_headers() {
        Capture capture = capture();
        MultiPartSpecification custom = new MultiPartSpecification() {
            public Object getContent() {
                return "c";
            }

            public String getControlName() {
                return "cn";
            }

            public String getMimeType() {
                return "text/plain";
            }

            public String getCharset() {
                return null;
            }

            public String getFileName() {
                return null;
            }

            public Map<String, String> getHeaders() {
                return null;
            }

            public boolean hasFileName() {
                return false;
            }
        };

        given().filter(capture).multiPart(custom).put("/x");

        MultiPartSpecification part = capture.request.getMultiPartParams().get(0);
        assertThat(part.getControlName()).isEqualTo("cn");
        assertThat(part.getFileName()).isNull();
        assertThat(part.getContent()).isEqualTo("c");
    }

    @Test
    void null_uri_url_and_method_arguments_are_rejected() {
        assertThatThrownBy(() -> given().get((URI) null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("URI cannot be null");
        assertThatThrownBy(() -> given().post((java.net.URL) null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("URL cannot be null");
        assertThatThrownBy(() -> given().request(io.restassured.http.Method.GET, (URI) null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("String cannot be null");
        assertThatThrownBy(() -> given().request("GET", (URI) null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("String cannot be null");
        assertThatThrownBy(() -> given().request("GET", (java.net.URL) null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("URL cannot be null");
        assertThatThrownBy(() -> given().request((io.restassured.http.Method) null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Method cannot be null");
        assertThatThrownBy(() -> given().request((io.restassured.http.Method) null, "/x")).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Method cannot be null");
        assertThatThrownBy(() -> given().request(" ", "/x")).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Method cannot be null");
        assertThatThrownBy(() -> given().get((String) null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("path cannot be null");
    }

    @Test
    void the_method_is_trimmed_and_upper_cased() {
        Capture capture = capture();

        given().filter(capture).request(" purge ", "/x");

        assertThat(capture.request.getMethod()).isEqualTo("PURGE");
    }

    // Proxy, SSL and config

    @Test
    void proxy_definitions() {
        assertThat(proxyOf(given().proxy("h", 1))).isEqualTo("h:1:http");
        assertThat(proxyOf(given().proxy("h"))).isEqualTo("h:8888:http");
        assertThat(proxyOf(given().proxy("https://h:2"))).isEqualTo("h:2:https");
        assertThat(proxyOf(given().proxy(3))).isEqualTo("127.0.0.1:3:http");
        assertThat(proxyOf(given().proxy("h", 4, "https"))).isEqualTo("h:4:https");
        assertThat(proxyOf(given().proxy(URI.create("http://h:5")))).isEqualTo("h:5:http");
        assertThat(proxyOf(given().proxy(ProxySpecification.host("h").withAuth("u", "p")))).isEqualTo("h:8888:http");
        // Quirk: in Groovy "URI.class" resolved to getURI().class, so the parameter is named String
        assertThatThrownBy(() -> given().proxy((URI) null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("String cannot be null");
    }

    private static String proxyOf(RequestSpecification spec) {
        ProxySpecification proxy = ((FilterableRequestSpecification) spec).getProxySpecification();
        return proxy.getHost() + ":" + proxy.getPort() + ":" + proxy.getScheme();
    }

    @Test
    void ssl_shortcuts_change_the_ssl_config() {
        FilterableRequestSpecification spec = (FilterableRequestSpecification) given().relaxedHTTPSValidation("TLS");
        assertThat(spec.getConfig().getSSLConfig().getSSLSocketFactory()).isNotNull();
        assertThat(spec.getConfig().getSSLConfig().getX509HostnameVerifier()).isEqualTo(org.apache.http.conn.ssl.SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);

        FilterableRequestSpecification keyStore = (FilterableRequestSpecification) given().keyStore("ks.jks", "pw");
        assertThat(keyStore.getConfig().getSSLConfig().getPathToKeyStore()).isEqualTo("ks.jks");
        assertThat(keyStore.getConfig().getSSLConfig().getKeyStorePassword()).isEqualTo("pw");
        assertThat(keyStore.getConfig().getSSLConfig().getX509HostnameVerifier()).isEqualTo(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        File file = new File("ts.jks");
        FilterableRequestSpecification trustStore = (FilterableRequestSpecification) given().trustStore(file, "pw");
        assertThat(trustStore.getConfig().getSSLConfig().getPathToTrustStore()).isEqualTo(file);
    }

    @Test
    void key_store_and_trust_store_instances_are_set_on_the_ssl_config() throws Exception {
        // The Groovy implementation failed with "MissingMethodException: No signature of method: keyStore for class:
        // io.restassured.config.SSLConfig is applicable for argument types: (java.security.KeyStore)" for the key store
        KeyStore keyStore = KeyStore.getInstance("JKS");
        KeyStore trustStore = KeyStore.getInstance("JKS");

        FilterableRequestSpecification spec = (FilterableRequestSpecification) given().keyStore(keyStore).trustStore(trustStore);

        assertThat(spec.getConfig().getSSLConfig().getKeyStore()).isSameAs(keyStore);
        assertThat(spec.getConfig().getSSLConfig().getTrustStore()).isSameAs(trustStore);
    }

    @Test
    void config_is_also_set_on_the_response_specification() {
        RestAssuredConfig cfg = config().logConfig(logConfig().enablePrettyPrinting(false));

        RequestSpecification spec = given().config(cfg);

        assertThat(((FilterableRequestSpecification) spec).getConfig()).isSameAs(cfg);
        assertThat(((ResponseSpecificationImpl) spec.then()).getConfig()).isSameAs(cfg);
    }

    @Test
    void csrf_shortcuts_change_the_csrf_config() {
        FilterableRequestSpecification spec = (FilterableRequestSpecification) given().csrf("/form", "_csrf");

        assertThat(spec.getConfig().getCsrfConfig().getCsrfTokenPath()).isEqualTo("/form");
        assertThat(spec.getConfig().getCsrfConfig().getCsrfInputFieldName()).isEqualTo("_csrf");
    }

    // Authentication

    @Test
    void the_default_auth_scheme_is_used_when_none_is_given() {
        RestAssured.authentication = RestAssured.basic("u", "p");
        Capture capture = capture();

        given().filter(capture).get("/x");

        assertThat(capture.request.getAuthenticationScheme()).isInstanceOf(BasicAuthScheme.class);

        Capture explicit = capture();
        given().filter(explicit).auth().none().get("/x");
        assertThat(explicit.request.getAuthenticationScheme()).isNotInstanceOf(BasicAuthScheme.class).isNotInstanceOf(NoAuthScheme.class);
    }

    @Test
    void form_auth_replaces_other_auth_filters_with_a_form_auth_filter_first() {
        Capture capture = capture();
        capture.order = OrderedFilter.HIGHEST_PRECEDENCE;
        AuthFilter otherAuthFilter = (req, res, ctx) -> ctx.next(req, res);
        Filter other = (req, res, ctx) -> ctx.next(req, res);

        given().filter(other).filter(capture).filter(otherAuthFilter).auth().form("u", "p", new FormAuthConfig("/login", "user", "pass")).get("/x");

        assertThat(capture.filters).hasSize(6);
        assertThat(capture.filters.get(0)).isSameAs(capture);
        assertThat(capture.filters.get(1)).isInstanceOf(FormAuthFilter.class);
        assertThat(capture.filters.get(2)).isSameAs(other);
        assertThat(capture.filters).doesNotContain(otherAuthFilter);
        FormAuthFilter formAuthFilter = (FormAuthFilter) capture.filters.get(1);
        assertThat(formAuthFilter.getUserName()).isEqualTo("u");
        assertThat(formAuthFilter.getPassword()).isEqualTo("p");
        assertThat(formAuthFilter.getFormAuthConfig().getFormAction()).isEqualTo("/login");
        assertThat(formAuthFilter.getSessionConfig().sessionIdName()).isEqualTo("JSESSIONID");
        assertThat(formAuthFilter.getCsrfConfig()).isNotNull();
    }

    // Filters

    @Test
    void filters_are_sorted_by_order_keeping_insertion_order_for_equal_orders_then_timing_and_send_request_filters_are_added() {
        Capture capture = capture();
        List<String> calls = new ArrayList<>();
        Filter a = named("a", calls, null);
        Filter b = named("b", calls, 5);
        Filter c = named("c", calls, null);
        Filter d = named("d", calls, OrderedFilter.HIGHEST_PRECEDENCE);
        Filter e = named("e", calls, OrderedFilter.LOWEST_PRECEDENCE);

        given().filters(a, b, (Filter) null).filters(Arrays.asList(c, e)).filter(d).filter(capture).disableCsrf().get("/x");

        assertThat(calls).containsExactly("d", "b", "a", "c");
        assertThat(capture.filters).hasSize(9);
        assertThat(capture.filters.subList(0, 7)).containsExactly(d, b, a, null, c, capture, e);
        assertThat(capture.filters.get(7)).isInstanceOf(TimingFilter.class);
        assertThat(capture.filters.get(8).getClass().getSimpleName()).isEqualTo("SendRequestFilter");
    }

    @Test
    void csrf_filter_is_added_unless_disabled_and_a_timing_filter_is_not_added_twice() {
        Capture capture = capture();
        TimingFilter timing = new TimingFilter();

        given().filter(capture).filter(timing).get("/x");

        assertThat(capture.filters).hasSize(4);
        assertThat(capture.filters.get(0)).isSameAs(capture);
        assertThat(capture.filters.get(1)).isSameAs(timing);
        assertThat(capture.filters.get(2)).isInstanceOf(CsrfFilter.class);
        assertThat(capture.filters.get(3).getClass().getSimpleName()).isEqualTo("SendRequestFilter");
    }

    @Test
    void filters_can_be_removed() {
        Filter a = (req, res, ctx) -> ctx.next(req, res);
        RequestLoggingFilter logging = new RequestLoggingFilter();

        FilterableRequestSpecification all = (FilterableRequestSpecification) given().filter(a).filter(logging).noFilters();
        FilterableRequestSpecification typed = (FilterableRequestSpecification) given().filter(a).filter(logging).noFiltersOfType(RequestLoggingFilter.class);

        assertThat(all.getDefinedFilters()).isEmpty();
        assertThat(typed.getDefinedFilters()).containsExactly(a);
    }

    @Test
    void logging_filters_are_added_when_logging_if_validation_fails_is_enabled() {
        Capture capture = capture();

        given().config(config().logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.HEADERS))).filter(capture).disableCsrf().get("/x");

        assertThat(capture.filters).extracting(f -> f.getClass().getSimpleName())
                .containsExactly("Capture", "RequestLoggingFilter", "ResponseLoggingFilter", "TimingFilter", "SendRequestFilter");
    }

    @Test
    void a_response_logging_filter_is_added_when_the_response_should_be_logged() {
        Capture capture = capture();

        given().filter(capture).disableCsrf().expect().log().status().when().get("/x");

        assertThat(capture.filters).extracting(f -> f.getClass().getSimpleName())
                .containsExactly("Capture", "ResponseLoggingFilter", "TimingFilter", "SendRequestFilter");
    }

    private static Filter named(String name, List<String> calls, Integer order) {
        if (order == null) {
            return new Filter() {
                public Response filter(FilterableRequestSpecification req, FilterableResponseSpecification res, FilterContext ctx) {
                    calls.add(name);
                    return ctx.next(req, res);
                }

                public String toString() {
                    return name;
                }
            };
        }
        return new OrderedFilter() {
            public Response filter(FilterableRequestSpecification req, FilterableResponseSpecification res, FilterContext ctx) {
                calls.add(name);
                return ctx.next(req, res);
            }

            public int getOrder() {
                return order;
            }

            public String toString() {
                return name;
            }
        };
    }

    @Test
    void filter_context_sends_the_path_with_query_params_from_the_path_only() {
        Capture capture = capture();

        given().filter(capture).queryParam("q", 1).get("/x/{a}?b=2", "y");

        assertThat(((io.restassured.internal.filter.FilterContextImpl) capture.context).getInternalRequestURI()).isEqualTo("http://localhost:8080/x/y?b=2");
    }

    // Logging

    @Test
    void log_all_prints_the_request() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(out, true, StandardCharsets.UTF_8);
        Capture capture = capture();

        given().config(config().logConfig(new LogConfig(stream, true))).log().all().filter(capture)
                .header("h", "v").cookie("c", "1").queryParam("q", "a b").formParam("f", "1").pathParam("p", "P")
                .post("/x/{p}/{u}", "U");

        assertThat(out.toString(StandardCharsets.UTF_8)).isEqualTo(
                "Request method:\tPOST\n" +
                        "Request URI:\thttp://localhost:8080/x/P/U?q=a%20b\n" +
                        "Proxy:\t\t\t<none>\n" +
                        "Request params:\t<none>\n" +
                        "Query params:\tq=a b\n" +
                        "Form params:\tf=1\n" +
                        "Path params:\tp=P\n" +
                        "Headers:\t\th=v\n" +
                        "\t\t\t\tAccept=*/*\n" +
                        "\t\t\t\tContent-Type=application/x-www-form-urlencoded; charset=ISO-8859-1\n" +
                        "Cookies:\t\tc=1\n" +
                        "Multiparts:\t\t<none>\n" +
                        "Body:\t\t\t<none>\n");
    }

    @Test
    void log_all_prints_byte_array_bodies_and_json_bodies() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(out, true, StandardCharsets.UTF_8);

        given().config(config().logConfig(new LogConfig(stream, true))).log().body().filter(capture()).body(new byte[]{65, 66}).put("/x");
        given().config(config().logConfig(new LogConfig(stream, true))).log().body().filter(capture()).contentType(ContentType.JSON).body(Map.of("a", 1)).put("/x");

        assertThat(out.toString(StandardCharsets.UTF_8)).isEqualTo("Body:\n[65, 66]\nBody:\n{\n    \"a\": 1\n}\n");
    }

    // Spec merging and the internal API used by other modules

    @Test
    void a_default_request_specification_is_merged() {
        RestAssured.requestSpecification = new io.restassured.builder.RequestSpecBuilder().addHeader("h", "1").addQueryParam("q", "1").setBasePath("/base").build();
        Capture capture = capture();

        given().filter(capture).header("h", "2").get("/x");

        assertThat(capture.request.getURI()).isEqualTo("http://localhost:8080/base/x?q=1");
        assertThat(capture.request.getHeaders().getValues("h")).containsExactly("1", "2");
    }

    @Test
    void a_request_specification_that_was_not_created_by_rest_assured_cannot_be_merged() {
        // The Groovy version failed with groovy.lang.MissingMethodException
        RequestSpecification foreign = (RequestSpecification) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{RequestSpecification.class}, (proxy, method, args) -> null);

        assertThatThrownBy(() -> given().spec(foreign))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Cannot merge a request specification of type jdk.proxy")
                .hasMessageEndingWith(", it must be of type io.restassured.internal.RequestSpecificationImpl.");
    }

    @Test
    void the_internal_api_used_by_spring_modules() {
        RequestSpecificationImpl spec = new RequestSpecificationImpl("http://localhost", RestAssured.UNDEFINED_PORT, "", new NoAuthScheme(), Collections.emptyList(),
                null, true, new RestAssuredConfig(), new LogRepository(), null, true, true);

        spec.setMethod("patch");
        spec.path(" /x/{a}/{b} ");
        spec.buildUnnamedPathParameterTuples(new Object[]{"1", 2});

        assertThat(spec.getMethod()).isEqualTo("PATCH");
        assertThat(spec.getUserDefinedPath()).isEqualTo("/x/{a}/{b}");
        assertThat(spec.getURI()).isEqualTo("http://localhost:8080/x/1/2");
        assertThat(spec.getUnnamedPathParams()).containsExactly(Map.entry("a", "1"), Map.entry("b", "2"));
        assertThat(spec.restAssuredConfig()).isNotNull();
        assertThat(RequestSpecificationImpl.getPlaceholders("/{a}/{ b }/{a}")).containsExactly("a", "b");
        assertThat(RequestSpecificationImpl.getDerivedPath("http://h/x/y?z")).isEqualTo("/x/y");
        assertThat(spec.partiallyApplyPathParams("/{a}/{b}", false, Arrays.asList("v 1", "2"))).isEqualTo("http://localhost:8080/v 1/2");

        spec.buildUnnamedPathParameterTuples(new Object[0]);
        assertThat(spec.getUnnamedPathParamValues()).isEmpty();
        spec.setMethod(null);
        assertThat(spec.getMethod()).isNull();
    }

    @Test
    void redundant_path_param_getters() {
        RequestSpecificationImpl spec = new RequestSpecificationImpl("http://localhost", RestAssured.UNDEFINED_PORT, "", new NoAuthScheme(), Collections.emptyList(),
                null, true, new RestAssuredConfig(), new LogRepository(), null, true, true);
        spec.pathParams("a", "1", "z", "2");
        spec.path("/{a}/{b}");
        spec.buildUnnamedPathParameterTuples(new Object[]{"x", "y", "1"});

        assertThat(spec.getRedundantNamedPathParams()).containsExactly(Map.entry("z", "2"));
        assertThat(spec.getRedundantUnnamedPathParamValues()).containsExactly("y");
        assertThat(spec.getPathParams()).containsExactly(Map.entry("a", "1"), Map.entry("z", "2"), Map.entry("b", "x"));
    }

    @Test
    void auth_scheme_property() {
        RequestSpecificationImpl spec = (RequestSpecificationImpl) given();
        BasicAuthScheme scheme = new BasicAuthScheme();

        spec.setAuthenticationScheme(scheme);

        assertThat(spec.getAuthenticationScheme()).isSameAs(scheme);
    }
}
