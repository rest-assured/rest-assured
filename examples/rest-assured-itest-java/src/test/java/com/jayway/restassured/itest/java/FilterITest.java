/*
 * Copyright 2013 the original author or authors.
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

package com.jayway.restassured.itest.java;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.builder.ResponseBuilder;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.filter.FilterContext;
import com.jayway.restassured.internal.filter.FormAuthFilter;
import com.jayway.restassured.itest.java.support.SpookyGreetJsonResponseFilter;
import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import com.jayway.restassured.specification.FilterableResponseSpecification;
import com.jayway.restassured.specification.RequestSpecification;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Ignore;
import org.junit.Test;

import java.io.PrintStream;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicReference;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.filter.log.ErrorLoggingFilter.logErrorsTo;
import static com.jayway.restassured.filter.log.ResponseLoggingFilter.logResponseTo;
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
        RestAssured.filters(asList(logErrorsTo(captor), logResponseTo(captor)));
        try {
            expect().body(equalTo("ERROR")).when().get("/409");
        }  finally {
            RestAssured.reset();
        }
        String lineSeparator = System.getProperty("line.separator");
        assertThat(writer.toString(), is("HTTP/1.1 409 Conflict\nContent-Type: text/plain;charset=utf-8\nContent-Length: 5\nServer: Jetty(9.3.2.v20150730)\n\nERROR" + lineSeparator + "HTTP/1.1 409 Conflict\nContent-Type: text/plain;charset=utf-8\nContent-Length: 5\nServer: Jetty(9.3.2.v20150730)\n\nERROR" + lineSeparator));
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
                       contentType.set(requestSpec.getRequestContentType());
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
                       contentType.set(requestSpec.getRequestContentType());
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
                root("greeting").
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

    @Ignore
    @Test public void
    can_change_path_parameters_from_filter() {
        given().
                filter((requestSpec, responseSpec, ctx) -> {
                    assertThat(requestSpec.getPathParams(), is(2));
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

    public static class CountingFilter implements Filter {

        public int counter = 0;

        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
            counter++;
            return ctx.next(requestSpec, responseSpec);
        }
    }
}
