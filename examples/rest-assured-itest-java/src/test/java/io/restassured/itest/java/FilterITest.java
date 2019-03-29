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

package io.restassured.itest.java;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseBuilder;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.filter.log.ErrorLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.internal.filter.FormAuthFilter;
import io.restassured.itest.java.support.SpookyGreetJsonResponseFilter;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;

import java.io.PrintStream;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class FilterITest extends WithJetty {

    @Test
    public void filterWorks() throws Exception {
        final FormAuthFilter filter = new FormAuthFilter();
        filter.setUserName("John");
        filter.setPassword("Doe");

        given().
                filter(filter).
        expect().
                statusCode(200).
                body(equalTo("OK")).
        when().
                get("/formAuth");
    }

    @Test
    public void supportsSpecifyingDefaultFilters() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        RestAssured.filters(asList(ErrorLoggingFilter.logErrorsTo(captor), ResponseLoggingFilter.logResponseTo(captor)));
        try {
            expect().body(equalTo("ERROR")).when().get("/409");
        }  finally {
            RestAssured.reset();
        }
        assertThat(writer.toString(), is(String.format("HTTP/1.1 409 Conflict%n" +
                "Content-Type: text/plain;charset=utf-8%n" +
                "Content-Length: 5%n" +
                "Server: Jetty(9.3.2.v20150730)%n" +
                "%n" +
                "ERROR%n" +
                "HTTP/1.1 409 Conflict%n" +
                "Content-Type: text/plain;charset=utf-8%n" +
                "Content-Length: 5%n" +
                "Server: Jetty(9.3.2.v20150730)%n" +
                "%n" +
                "ERROR%n")));
    }

    @Test
    public void filtersCanAlterResponseBeforeValidation() throws Exception {
       given().
               filter(new SpookyGreetJsonResponseFilter()).
               queryParam("firstName", "John").
               queryParam("lastName", "Doe").
       expect().
                body("greeting.firstName", equalTo("Spooky")).
                body("greeting.lastName", equalTo("Doe")).
       when().
                get("/greetJSON");
    }

    /**
     * Regression Test for 197
     */
    @Test
    public void defaultFiltersDontAccumluate() {
        CountingFilter myFilter = new CountingFilter();
        try {
            RestAssured.config = RestAssuredConfig.newConfig();
            RestAssured.filters(myFilter);

            RequestSpecification spec = new RequestSpecBuilder().build();

            given().get("/greetJSON?firstName=John&lastName=Doe");
            assertThat(myFilter.counter, equalTo(1));

            given().spec(spec).get("/greetJSON?firstName=Johan&lastName=Doe");
            assertThat(myFilter.counter, equalTo(2));
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void httpClientIsAccessibleFromTheRequestSpecification() {
        // Given
        final MutableObject<HttpClient> client = new MutableObject<HttpClient>();
        // When

        given().
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        client.setValue(requestSpec.getHttpClient());
                        return new ResponseBuilder().setStatusCode(200).setContentType("application/json").setBody("{ \"message\" : \"hello\"}").build();
                    }
                }).
        expect().
                body("message", equalTo("hello")).
        when().
                get("/something");

        // Then
        assertThat(client.getValue(), instanceOf(DefaultHttpClient.class));
    }

    @Test
    public void content_type_in_filter_contains_charset_by_default() {
        final AtomicReference<String> contentType = new AtomicReference<String>();

        given().
                filter(new Filter() {
                   public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                       contentType.set(requestSpec.getContentType());
                       return ctx.next(requestSpec, responseSpec);
                   }
                }).
                formParam("firstName", "John").
                formParam("lastName", "Doe").
        when().
                post("/greet").
        then().
                statusCode(200);

        assertThat(contentType.get(), equalTo("application/x-www-form-urlencoded; charset=ISO-8859-1"));
    }

    @Test
    public void content_type_in_filter_doesnt_contain_charset_if_configured_not_to() {
        final AtomicReference<String> contentType = new AtomicReference<String>();

        given().
                config(RestAssuredConfig.config().encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))).
                filter(new Filter() {
                   public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                       contentType.set(requestSpec.getContentType());
                       return ctx.next(requestSpec, responseSpec);
                   }
                }).
                formParam("firstName", "John").
                formParam("lastName", "Doe").
        when().
                post("/greet").
        then().
                statusCode(200);

        assertThat(contentType.get(), equalTo("application/x-www-form-urlencoded"));
    }

    @Test public void
    it_is_possible_to_change_port_and_base_path_from_filters() {
        given().
                basePath("/x").
                port(80).
                filter((requestSpec, responseSpec, ctx) -> {
                    requestSpec.port(8080);
                    requestSpec.basePath("/John");
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/Doe").
        then().
                statusCode(200).
                body("firstName", equalTo("John")).
                body("lastName", equalTo("Doe")).
                body("fullName", equalTo("John Doe"));
    }

    @Test public void
    can_add_query_params_from_filter() {
        given().
                filter((requestSpec, responseSpec, ctx) -> {
                    requestSpec.queryParam("firstName", "John");
                    requestSpec.queryParam("lastName", "Doe");
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/greetJSON").
        then().
                statusCode(200).
                rootPath("greeting").
                body("firstName", equalTo("John")).
                body("lastName", equalTo("Doe"));
    }


    @Test public void
    can_change_path_from_filter() {
        given().
                filter((requestSpec, responseSpec, ctx) -> {
                    requestSpec.path("/lotto");
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/greetJSON").
        then().
                statusCode(200).
                body("lotto.lottoId", is(5));
    }

    @Test public void
    can_change_path_parameters_from_filter() {
        given().
                filter((requestSpec, responseSpec, ctx) -> {
                    assertThat(requestSpec.getNamedPathParams().size(), is(0));
                    assertThat(requestSpec.getUndefinedPathParamPlaceholders(), contains("firstName", "lastName"));
                    requestSpec.pathParam("firstName", "John");
                    requestSpec.pathParam("lastName", "Doe");
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/{firstName}/{lastName}").
        then().
                statusCode(200).
                body("firstName", equalTo("John")).
                body("lastName", equalTo("Doe")).
                body("fullName", equalTo("John Doe"));
    }

    @Test public void
    can_change_partially_applied_named_path_parameters_from_filter() {
        given().
                pathParam("lastName", "Doe").
                filter((requestSpec, responseSpec, ctx) -> {
                    assertThat(requestSpec.getNamedPathParams().size(), is(1));
                    assertThat(requestSpec.getUndefinedPathParamPlaceholders(), contains("firstName"));
                    requestSpec.pathParam("firstName", "John");
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/{firstName}/{lastName}").
        then().
                statusCode(200).
                body("firstName", equalTo("John")).
                body("lastName", equalTo("Doe")).
                body("fullName", equalTo("John Doe"));
    }

    @Test public void
    can_change_partially_applied_unnamed_path_parameters_from_filter() {
        given().
                filter((requestSpec, responseSpec, ctx) -> {
                    assertThat(requestSpec.getNamedPathParams().size(), is(0));
                    assertThat(requestSpec.getUndefinedPathParamPlaceholders(), contains("lastName"));
                    requestSpec.pathParam("lastName", "Doe");
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/{firstName}/{lastName}", "John").
        then().
                statusCode(200).
                body("firstName", equalTo("John")).
                body("lastName", equalTo("Doe")).
                body("fullName", equalTo("John Doe"));
    }


    @Test public void
    can_change_unnamed_path_param_with_named_path_param_from_filter() {
        given().
                filter((requestSpec, responseSpec, ctx) -> {
                    requestSpec.pathParam("lastName", "Doe");
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/{firstName}/{lastName}", "John", "Doe2").
        then().
                statusCode(200).
                body("firstName", equalTo("John")).
                body("lastName", equalTo("Doe")).
                body("fullName", equalTo("John Doe"));
    }

    @SuppressWarnings("Duplicates")
    @Test public void
    can_change_named_path_param_with_named_path_param_from_filter() {
        given().
                pathParam("lastName", "Doe").
                filter((requestSpec, responseSpec, ctx) -> {
                    assertThat(requestSpec.getUndefinedPathParamPlaceholders().size(), is(0));
                    assertThat(requestSpec.getPathParamPlaceholders(), contains("firstName", "lastName"));
                    requestSpec.pathParam("firstName", "John");
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/{firstName}/{lastName}", "John2").
        then().
                statusCode(200).
                body("firstName", equalTo("John")).
                body("lastName", equalTo("Doe")).
                body("fullName", equalTo("John Doe"));
    }

    @Test public void
    can_change_unnamed_path_param_with_named_path_param_from_filter_when_path_param_is_used_in_several_places() {
        given().
                filter((requestSpec, responseSpec, ctx) -> {
                    requestSpec.pathParam("name", "John");
                    assertThat(requestSpec.getUndefinedPathParamPlaceholders().size(), is(0));
                    assertThat(requestSpec.getPathParamPlaceholders(), contains("name"));
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/{name}/{name}", "John2").
        then().
                statusCode(200).
                body("firstName", equalTo("John")).
                body("lastName", equalTo("John")).
                body("fullName", equalTo("John John"));
    }

    @SuppressWarnings("Duplicates")
    @Test public void
    can_change_named_path_param_with_named_path_param_from_filter_when_path_param_is_used_in_several_places() {
        given().
                pathParam("name", "John2").
                filter((requestSpec, responseSpec, ctx) -> {
                    assertThat(requestSpec.getUndefinedPathParamPlaceholders().size(), is(0));
                    assertThat(requestSpec.getPathParamPlaceholders(), contains("name"));
                    requestSpec.pathParam("name", "John");
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/{name}/{name}").
        then().
                statusCode(200).
                body("firstName", equalTo("John")).
                body("lastName", equalTo("John")).
                body("fullName", equalTo("John John"));
    }

    @Test public void
    primitive_params_are_converted_to_strings() {
        given().
                queryParam("something", 1).
                filter((requestSpec, responseSpec, ctx) -> {
                    assertThat(requestSpec.getQueryParams().get("something"), equalTo("1"));
                    return new ResponseBuilder().setStatusCode(200).build();
                }).
        when().
                get("/somewhere", "Doe");
    }

    @Test public void
    get_path_params_in_request_spec_returns_both_named_and_unnamed_path_params()  {
        given().
                pathParam("firstName", "John").
                filter((requestSpec, responseSpec, ctx) -> {
                    assertThat(requestSpec.getPathParams(), allOf(hasEntry("firstName", "John"), not(hasEntry("lastName", "Doe"))));
                    assertThat(requestSpec.getPathParams().size(), is(2));
                    requestSpec.pathParam("lastName", "Something");
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/{firstName}/{lastName}", "John2").
        then().
                body("firstName", equalTo("John")).
                body("lastName", equalTo("Something")).
                body("fullName", equalTo("John Something"));
    }

    @Test public void
    can_remove_unnamed_path_parameter_by_value_from_filter() throws Exception {
        given().
                filter((requestSpec, responseSpec, ctx) -> {
                    assertThat(requestSpec.getUnnamedPathParamValues(), contains("John", "Doe", "Real Name"));
                    requestSpec.removeUnnamedPathParamByValue("Real Name");
                    assertThat(requestSpec.getUnnamedPathParamValues(), contains("John", "Doe"));
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/{firstName}/{lastName}", "John", "Doe", "Real Name").
        then().
                statusCode(200).
                body("firstName", equalTo("John")).
                body("lastName", equalTo("Doe")).
                body("fullName", equalTo("John Doe"));
    }

    @Test public void
    can_remove_unnamed_path_parameter_by_name_from_filter() throws Exception {
        given().
                filter((requestSpec, responseSpec, ctx) -> {
                    assertThat(requestSpec.getUnnamedPathParamValues(), contains("John", "Doe"));
                    requestSpec.removeUnnamedPathParam("firstName");
                    assertThat(requestSpec.getUnnamedPathParamValues(), contains("Doe"));
                    requestSpec.pathParam("firstName", "John2");
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/{firstName}/{lastName}", "John", "Doe").
        then().
                statusCode(200).
                body("firstName", equalTo("John2")).
                body("lastName", equalTo("Doe")).
                body("fullName", equalTo("John2 Doe"));
    }

    @Test public void
    can_remove_both_unnamed_and_named_path_parameter_from_filter_and_order_is_maintained() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Path parameters were not correctly defined. Redundant path parameters are: John3, Doe.");

        given().
                pathParam("firstName", "John2").
                filter((requestSpec, responseSpec, ctx) -> {
                    assertThat(requestSpec.getUnnamedPathParamValues(), contains("John", "John3", "Doe"));
                    requestSpec.removePathParam("firstName");
                    requestSpec.removePathParam("lastName");
                    assertThat(requestSpec.getUnnamedPathParamValues(), contains("John3", "Doe"));
                    assertThat(requestSpec.getNamedPathParams().isEmpty(), is(true));
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/{firstName}/{lastName}", "John", "John3", "Doe");
    }

    @Test public void
    can_remove_a_specific_header_from_filter() {
        given().
                header("John", "Doe").
                header("Jane", "Doe").
                filter((requestSpec, responseSpec, ctx) -> {
                    requestSpec.removeHeader("john");
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/headersWithValues").
        then().
                body("keySet()", allOf(not(hasItem("John")), hasItem("Jane")));
    }

    @Test public void
    can_remove_all_defined_headers_from_filter() {
        given().
                header("John", "Doe").
                header("Jane", "Doe").
                filter((requestSpec, responseSpec, ctx) -> {
                    requestSpec.removeHeaders();
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/headersWithValues").
        then().
                body("keySet()", allOf(not(hasItem("John")), not(hasItem("Jane"))));
    }

    @Test public void
    can_remove_a_specific_cookie_from_filter_using_the_cookie_name_as_string() {
        given().
                cookie("John", "Doe").
                cookie("Jane", "Doe").
                filter((requestSpec, responseSpec, ctx) -> {
                    requestSpec.removeCookie("john");
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/cookiesWithValues").
        then().
                body("name", contains("Jane"));
    }

    @Test public void
    can_remove_a_specific_cookie_from_filter_using_a_cookie_object() {
        given().
                cookie("John", "Doe").
                cookie("Jane", "Doe").
                filter((requestSpec, responseSpec, ctx) -> {
                    requestSpec.removeCookie(new Cookie.Builder("john", "ikk").build());
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/cookiesWithValues").
        then().
                body("name", contains("Jane"));
    }

    @Test public void
    can_remove_all_defined_cookies() {
        given().
                cookie("John", "Doe").
                cookie("Jane", "Doe").
                filter((requestSpec, responseSpec, ctx) -> {
                    requestSpec.removeCookies();
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/cookiesWithValues").
        then().
                body("size()", is(0));
    }

    @Test public void
    can_replace_a_specific_header_from_filter() {
        given().
                header("John", "Doe").
                header("Jane", "Doe").
                filter((requestSpec, responseSpec, ctx) -> {
                    requestSpec.replaceHeader("john", "Doe2");
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/headersWithValues").
        then().
                body("john", contains("Doe2")).
                body("Jane", contains("Doe"));
    }

    @Test public void
    can_replace_a_specific_cookie_from_filter_using_cookie_name_and_value() {
        given().
                cookie("John", "Doe").
                cookie("Jane", "Doe").
                filter((requestSpec, responseSpec, ctx) -> {
                    requestSpec.replaceCookie("john", "Doe2");
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/cookiesWithValues").
        then().
                body("find { it.name == 'john' }.value", equalTo("Doe2")).
                body("find { it.name == 'Jane' }.value", equalTo("Doe"));
    }

    @Test public void
    can_replace_a_specific_cookie_from_filter_using_cookie_object() {
        given().
                cookie("John", "Doe").
                cookie("Jane", "Doe").
                filter((requestSpec, responseSpec, ctx) -> {
                    requestSpec.replaceCookie(new Cookie.Builder("john", "Doe2").build());
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/cookiesWithValues").
        then().
                body("find { it.name == 'john' }.value", equalTo("Doe2")).
                body("find { it.name == 'Jane' }.value", equalTo("Doe"));
    }

    @Test public void
    can_replace_all_defined_headers() {
        given().
                header("John", "Doe").
                header("Jane", "Doe").
                filter((requestSpec, responseSpec, ctx) -> {
                    requestSpec.replaceHeaders(new Headers(new Header("New", "Header")));
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/headersWithValues").
        then().
                body("keySet()", allOf(hasItem("New"), not(hasItem("John")), not(hasItem("Jane"))));
    }

    @Test public void
    can_replace_all_defined_cookies() {
        given().
                cookie("John", "Doe").
                cookie("Jane", "Doe").
                filter((requestSpec, responseSpec, ctx) -> {
                    requestSpec.replaceCookies(new Cookies(new Cookie.Builder("New", "Cookie").build()));
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                get("/cookiesWithValues").
        then().
                body("name", contains("New")).
                body("value", contains("Cookie"));
    }

    public static class CountingFilter implements Filter {

        public int counter = 0;

        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
            counter++;
            return ctx.next(requestSpec, responseSpec);
        }
    }
}
