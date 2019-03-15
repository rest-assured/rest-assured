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
import io.restassured.builder.ResponseBuilder;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.apache.commons.io.output.WriterOutputStream;
import org.junit.Assert;
import org.junit.Test;

import java.io.PrintStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.*;

import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.itest.java.support.RequestPathFromLogExtractor.loggedRequestPathIn;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class URLITest extends WithJetty {

    @Test
    public void specifyingFullyQualifiedPathOverridesValues() throws Exception {
        RestAssured.basePath = "/something";
        RestAssured.baseURI = "http://www.google.com";
        RestAssured.port = 80;
        try {
            expect().body("store.book[0..2].size()", equalTo(3)).when().get("http://localhost:8080/jsonStore");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void whenBaseURIEndsWithSlashAndPathBeginsWithSlashThenOneSlashIsRemoved() throws Exception {
        RestAssured.baseURI = "http://localhost/";
        RestAssured.port = 8080;
        try {
            expect().body("store.book[0..2].size()", equalTo(3)).when().get("/jsonStore");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void whenBaseURIIncludesPortAndEndsWithSlashAndPathBeginsWithSlashThenOneSlashIsRemoved() throws Exception {
        RestAssured.baseURI = "http://localhost:8080/";
        try {
            expect().body("store.book[0..2].size()", equalTo(3)).when().get("/jsonStore");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void whenBaseURIAndPathDoesntEndsWithSlashThenOneSlashIsInserted() throws Exception {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;
        try {
            expect().body("store.book[0..2].size()", equalTo(3)).when().get("jsonStore");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void baseURIPicksUpSchemeAndPort() throws Exception {
        RestAssured.baseURI = "http://localhost:8080/lotto";

        try {
            expect().body("lotto.lottoId", equalTo(5)).when().get("");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void baseURIPicksUpSchemeAndPortAndBasePath() throws Exception {
        RestAssured.basePath = "/lotto";
        RestAssured.baseURI = "http://localhost:8080";

        try {
            expect().body("lotto.lottoId", equalTo(5)).when().get("");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void basicAuthenticationWithBasePath() throws Exception {
        RestAssured.basePath = "/secured/hello";
        try {
            given().auth().basic("jetty", "jetty").expect().statusCode(200).when().get("");
        } finally {
            RestAssured.reset();
        }
    }

    @Test(timeout = 3000)
    public void canCallFullyQualifiedUrlsWithoutPortDefined() throws Exception {
        // This test hangs forever unless it works
        get("http://filehost-semc-rss-dev.s3.amazonaws.com/testfile1.txt");
    }

    @Test
    public void urlWithUnderscoreInHostNameWorks() throws Exception {
        // Given
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);


        // When
        given().
                contentType(JSON).
                filter(new RequestLoggingFilter(captor)).
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                    }
                }).
        expect().
                 statusCode(200).
                 body(equalTo("changed")).
        when().
                get("http://toto_titi.alarmesomfy.net"); // Has _ in host name

        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://toto_titi.alarmesomfy.net"));
    }

    @Test
    public void fullyQualifiedUrlEndingWithSlashDoesntAddPort8080() throws Exception {
        // Given
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);


        // When
        given().
                contentType(JSON).
                filter(new RequestLoggingFilter(captor)).
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                    }
                }).
        expect().
                 statusCode(200).
                 body(equalTo("changed")).
        when().
                get("http://tototiti.alarmesomfy.net/");

        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://tototiti.alarmesomfy.net/"));
    }

    @Test
    public void doesntAddPort8080ToFullyQualifiedUrlDefinedInHttpVerbMethod() throws Exception {
        // Given
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        // When
        given().
                port(8080).
                contentType(JSON).
                filter(new RequestLoggingFilter(captor)).
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                    }
                }).
        expect().
                 statusCode(200).
                 body(equalTo("changed")).
        when().
                get("http://tototiti.alarmesomfy.net/");

        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://tototiti.alarmesomfy.net:8080/"));
    }

    @Test
    public void fullyQualifiedUrlAddsPort8080IfExplicitlyDefinedStatically() throws Exception {
        // Given
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        RestAssured.port = 8080;
        RestAssured.baseURI = "http://tototiti.alarmesomfy.net/";
        RestAssured.basePath = "/api";

        // When
        try {
            given().
                    contentType(JSON).
                    filter(new RequestLoggingFilter(captor)).
                    filter(new Filter() {
                        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                            return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                        }
                    }).
            expect().
                     statusCode(200).
                     body(equalTo("changed")).
            when().
                    get("/");
        } finally {
            RestAssured.reset();
        }

        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://tototiti.alarmesomfy.net:8080/api/"));
    }

    @Test
    public void fullyQualifiedUrlIncludingPortWorks() throws Exception {
        // Given
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);


        // When
        given().
                contentType(JSON).
                filter(new RequestLoggingFilter(captor)).
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                    }
                }).
        expect().
                 statusCode(200).
                 body(equalTo("changed")).
        when().
                get("http://tototiti.alarmesomfy.net:8080/");

        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://tototiti.alarmesomfy.net:8080/"));
    }

    @Test
    public void fullyQualifiedLocalhostUrlIncludingPortWorks() throws Exception {
        // Given
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);


        // When
        given().
                contentType(JSON).
                filter(new RequestLoggingFilter(captor)).
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                    }
                }).
        expect().
                 statusCode(200).
                 body(equalTo("changed")).
        when().
                get("http://localhost:8082/");

        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://localhost:8082/"));
    }

    @Test
    public void fullyQualifiedLocalhostUrlEndingWithSlashDoesntAddsPort8080() throws Exception {
        // Given
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);


        // When
        given().
                contentType(JSON).
                filter(new RequestLoggingFilter(captor)).
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                    }
                }).
        expect().
                 statusCode(200).
                 body(equalTo("changed")).
        when().
                get("http://localhost");

        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://localhost:8080"));
    }

    @Test
    public void whenNoAuthorityOrPortIsSpecifiedThenLocalhostOnPort8080IsUsed() throws Exception {
        // Given
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        // When
        given().
                contentType(JSON).
                filter(new RequestLoggingFilter(captor)).
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                    }
                }).
        expect().
                 statusCode(200).
                 body(equalTo("changed")).
        when().
                get("/");

        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://localhost:8080/"));
    }

    @Test
    public void takesSpecificationPortIntoAccountWhenNoHostIsSpecified() throws Exception {
        // Given
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        // When
        given().
                port(8083).
                contentType(JSON).
                filter(new RequestLoggingFilter(captor)).
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                    }
                }).
        expect().
                 statusCode(200).
                 body(equalTo("changed")).
        when().
                get("/");

        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://localhost:8083/"));
    }

    @Test
    public void takesSpecificationPortIntoAccountWhenLocalhostHostIsSpecifiedEndingWithSlash() throws Exception {
        // Given
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        // When
        given().
                port(8084).
                contentType(JSON).
                filter(new RequestLoggingFilter(captor)).
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                    }
                }).
        expect().
                 statusCode(200).
                 body(equalTo("changed")).
        when().
                get("http://localhost/");


        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://localhost:8084/"));
    }

    @Test
    public void takesSpecificationPortIntoAccountWhenLocalhostHostIsSpecifiedWithoutEndingWithSlash() throws Exception {
        // Given
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        // When
        given().
                port(8084).
                contentType(JSON).
                filter(new RequestLoggingFilter(captor)).
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                    }
                }).
        expect().
                 statusCode(200).
                 body(equalTo("changed")).
        when().
                get("http://localhost");


        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://localhost:8084"));
    }

    @Test
    public void takesSpecificationPortIntoAccountWhenLocalhostHostIsSpecifiedStatically() throws Exception {
        // Given
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        RestAssured.port = 8084;
        RestAssured.baseURI = "http://localhost";

        // When
        try {
            given().
                    contentType(JSON).
                    filter(new RequestLoggingFilter(captor)).
                    filter(new Filter() {
                        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                            return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                        }
                    }).
            expect().
                     statusCode(200).
                     body(equalTo("changed")).
            when().
                    get("/");
        } finally {
            RestAssured.reset();
        }


        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://localhost:8084/"));
    }

    @Test
    public void takesPortIntoAccountWhenSpecifiedInTheURLUsingLocalhostHost() throws Exception {
        // Given
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        // When
        try {
            given().
                    contentType(JSON).
                    auth().digest("username", "password").
                    filter(new RequestLoggingFilter(captor)).
                    filter(new Filter() {
                        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                            return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                        }
                    }).
            expect().
                     statusCode(200).
                     body(equalTo("changed")).
            when().
                    head("http://localhost:8084/something");
        } finally {
            RestAssured.reset();
        }

        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://localhost:8084/something"));
    }

    @Test
    public void takesSpecificationPortIntoAccountWhenLocalhostHostAndPort8080IsSpecifiedStatically() throws Exception {
        // Given
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        RestAssured.port = 8080;
        RestAssured.baseURI = "http://localhost/";
        RestAssured.basePath = "/api";

        // When
        try {
            given().
                    contentType(JSON).
                    filter(new RequestLoggingFilter(captor)).
                    filter(new Filter() {
                        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                            return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                        }
                    }).
            expect().
                     statusCode(200).
                     body(equalTo("changed")).
            when().
                    get("/");
        } finally {
            RestAssured.reset();
        }


        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://localhost:8080/api/"));
    }

    @Test
    public void takesStaticSpecificationPortIntoAccountWhenNoHostIsSpecified() throws Exception {
        // Given
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        RestAssured.port = 9093;

        // When
        try {
            given().
                    contentType(JSON).
                    filter(new RequestLoggingFilter(captor)).
                    filter(new Filter() {
                        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                            return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                        }
                    }).
            expect().
                     statusCode(200).
                     body(equalTo("changed")).
            when().
                    get("/");
        } finally {
            RestAssured.reset();
        }

        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://localhost:9093/"));
    }

    @Test
    public void takesStaticSpecificationPortIntoAccountWhenBaseUriIsSpecified() throws Exception {
        // Given
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        RestAssured.port = 9093;
        RestAssured.baseURI = "http://something.com";

        // When
        try {
            given().
                    contentType(JSON).
                    filter(new RequestLoggingFilter(captor)).
                    filter(new Filter() {
                        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                            return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                        }
                    }).
            expect().
                     statusCode(200).
                     body(equalTo("changed")).
            when().
                    get("/");
        } finally {
            RestAssured.reset();
        }

        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://something.com:9093/"));
    }

    @Test
    public void takesNonStaticSpecificationPortIntoAccountWhenHostIsSpecified() throws Exception {
        // Given
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);


        // When
        given().
                port(9093).
                contentType(JSON).
                filter(new RequestLoggingFilter(captor)).
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                    }
                }).
        expect().
                 statusCode(200).
                 body(equalTo("changed")).
        when().
                get("http://something.com");

        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://something.com:9093"));
    }


    @Test
    public void trailingSlashesAreRetainedWhenConfiguredStatically() throws Exception {
        // Given

        RestAssured.basePath = "/v1/";
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        // When
        try {
            given().
                    filter(new RequestLoggingFilter(captor)).
                    filter(new Filter() {
                        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                            return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                        }
                    }).
            expect().
                     statusCode(200).
                     body(equalTo("changed")).
            when().
                    get("/");
        } finally {
            RestAssured.reset();
        }

        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://localhost:8080/v1/"));
    }

    @Test
    public void doesntAddTrailingSlashesWhenNoTrailingSlashIsUsed() throws Exception {
        // Given

        RestAssured.basePath = "/v1";
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        // When
        try {
            given().
                    filter(new RequestLoggingFilter(captor)).
                    filter(new Filter() {
                        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                            return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                        }
                    }).
            expect().
                     statusCode(200).
                     body(equalTo("changed")).
            when().
                    get("");
        } finally {
            RestAssured.reset();
        }

        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://localhost:8080/v1"));
    }

    @Test
    public void addsSingleTrailingSlashToPathWhenSlashIsUsedAsPathInGetMethod() throws Exception {
        // Given

        RestAssured.basePath = "/v1";
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        // When
        try {
            given().
                    filter(new RequestLoggingFilter(captor)).
                    filter(new Filter() {
                        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                            return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                        }
                    }).
            expect().
                     statusCode(200).
                     body(equalTo("changed")).
            when().
                    get("/");
        } finally {
            RestAssured.reset();
        }

        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://localhost:8080/v1/"));
    }

    @Test
    public void trailingSlashesAreRetainedWhenPassedAsArgumentToGetMethod() throws Exception {
        // Given

        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        // When
        given().
                filter(new RequestLoggingFilter(captor)).
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                    }
                }).
        expect().
                 statusCode(200).
                 body(equalTo("changed")).
        when().
                get("/v1/");

        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://localhost:8080/v1/"));
    }

    /**
     * See issue 304 & 305
     */
    @Test
    public void fullyQualifiedUrlIsHandledCorrectlyInLog() throws Exception {
        // Given
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        // When
        given().
                filter(new RequestLoggingFilter(captor)).
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                    }
                }).
        expect().
                 statusCode(200).
                 body(equalTo("changed")).
        when().
                get("http://ya.ru/bla/?param=value=");

        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://ya.ru/bla/?param=value%3D"));
    }

    @Test
    public void fullyQualifiedUrlIsHandledCorrectlyInLogWithNoValueParam() throws Exception {
        // Given
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        // When
        given().
                filter(new RequestLoggingFilter(captor)).
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                    }
                }).
        expect().
                 statusCode(200).
                 body(equalTo("changed")).
        when().
                get("http://ya.ru/bla/?param=value=&ikk");

        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://ya.ru/bla/?param=value%3D&ikk"));
    }

    @Test
    public void canUseAGetRequestInDslForUris() throws Exception {
        // Given
        final URI uri = new URI("http://localhost:8080/greet?firstName=John&lastName=Doe");

        // When
        expect().body("greeting", equalTo("Greetings John Doe")).when().get(uri);
    }

    @Test
    public void canUseAPostRequestInDslForUris() throws Exception {
        // Given
        final URI uri = new URI("http://localhost:8080/greet?firstName=John&lastName=Doe");

        // When
        expect().body("greeting", equalTo("Greetings John Doe")).when().post(uri);
    }

    @Test
    public void canUseAStaticGetRequestWithUris() throws Exception {
        // Given
        final URI uri = new URI("http://localhost:8080/greet?firstName=John&lastName=Doe");

        // When
        String greeting = get(uri).andReturn().path("greeting");

        // Then
        assertThat(greeting, equalTo("Greetings John Doe"));
    }

    @Test
    public void canUseAStaticPostRequestWithUris() throws Exception {
        // Given
        final URI uri = new URI("http://localhost:8080/greet?firstName=John&lastName=Doe");

        // When
        String greeting = post(uri).andReturn().path("greeting");

        // Then
        assertThat(greeting, equalTo("Greetings John Doe"));
    }
    
    @Test
    public void canUseAGetRequestInDslForUrls() throws Exception {
        // Given
        final URL url = new URL("http://localhost:8080/greet?firstName=John&lastName=Doe");

        // When
        expect().body("greeting", equalTo("Greetings John Doe")).when().get(url);
    }

    @Test
    public void canUseAPostRequestInDslForUrls() throws Exception {
        // Given
        final URL url = new URL("http://localhost:8080/greet?firstName=John&lastName=Doe");

        // When
        expect().body("greeting", equalTo("Greetings John Doe")).when().post(url);
    }

    @Test
    public void canUseAStaticGetRequestWithUrls() throws Exception {
        // Given
        final URL url = new URL("http://localhost:8080/greet?firstName=John&lastName=Doe");

        // When
        String greeting = get(url).andReturn().path("greeting");

        // Then
        assertThat(greeting, equalTo("Greetings John Doe"));
    }

    @Test
    public void canUseAStaticPostRequestWithUrls() throws Exception {
        // Given
        final URL url = new URL("http://localhost:8080/greet?firstName=John&lastName=Doe");

        // When
        String greeting = post(url).andReturn().path("greeting");

        // Then
        assertThat(greeting, equalTo("Greetings John Doe"));
    }

    @Test
    public void canUseAGetRequestWithoutAnyParametersInDsl() throws Exception {
        // Given
        RestAssured.baseURI = "http://localhost:8080/hello";

        // When
        try {
            expect().body("hello", equalTo("Hello Scalatra")).when().get();
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void canUseAPostRequestWithoutAnyParametersInDsl() throws Exception {
        // Given
        RestAssured.baseURI = "http://localhost:8080/hello";

        // When
        try {
            expect().body("hello", equalTo("Hello Scalatra")).when().post();
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void canUseAStaticGetRequestWithoutAnyParameters() throws Exception {
        // Given
        RestAssured.baseURI = "http://localhost:8080/hello";

        // When
        String greeting = null;
        try {
            greeting = get().path("hello");
        } finally {
            RestAssured.reset();
        }

        // Then
        assertThat(greeting, equalTo("Hello Scalatra"));
    }

    @Test
    public void canUseAStaticPostRequestWithoutAnyParameters() throws Exception {
        // Given
        RestAssured.baseURI = "http://localhost:8080/hello";

        // When
        String greeting = null;
        try {
            greeting = post().path("hello");
        } finally {
            RestAssured.reset();
        }

        // Then
        assertThat(greeting, equalTo("Hello Scalatra"));
    }

    /**
     * Asserts that issue 314 is resolved
     */
    @Test public void
    query_param_name_with_empty_value_is_not_treated_as_no_value_query_param() {
        // Given
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        // When
        given().
                filter(new RequestLoggingFilter(captor)).
        when().
                get("/requestUrl?param1=1&voidparam=&param3=3").
        then().
                 statusCode(200).
                 body(equalTo("http://localhost:8080/requestUrl?param1=1&voidparam=&param3=3"));

        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://localhost:8080/requestUrl?param1=1&voidparam=&param3=3"));
    }

    @Test public void
    given_a_base_uri_with_fully_qualified_url_defaults_to_using_port_80_if_port_is_not_explicitly_defined() {
        // Given
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        // When
        given().
                baseUri("http://httpbin.org/get").
                filter(new RequestLoggingFilter(captor)).
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                    }
                }).
        when().
                get().
        then().
                statusCode(200).
                body(equalTo("changed"));

        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://httpbin.org/get"));
    }

    @Test public void
    get_using_fully_qualified_url_uses_port_80() {
        // Given
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        // When
        given().
                baseUri("http://httpbin.org/get").
                filter(new RequestLoggingFilter(captor)).
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                    }
                }).
        when().
                get().
        then().
                statusCode(200).
                body(equalTo("changed"));

        // Then
        assertThat(loggedRequestPathIn(writer), equalTo("http://httpbin.org/get"));
    }

    /**
     * This test try to add query parameters as map that contains value as {@link Collection}
     * @throws Exception
     */
    @Test
    public void get_request_with_query_parameters_as_map_that_contains_collection_as_one_of_params() throws Exception {
        final ArrayList<String> parameterValues = new ArrayList<String>() {{
            add("value1");
            add("value2");
        }};

        Map<String, Object> queryParameters = new HashMap<String, Object>() {{
            put("queryParameter", parameterValues);
        }};

        String response = given()
                .queryParams(queryParameters)
                .when()
                .get("/requestUrl").asString();
        //query parameters should be parsed correctly
        Assert.assertEquals("http://localhost:8080/requestUrl?queryParameter=value1&queryParameter=value2", response);
    }


    /**
     * This test try to add form parameters as map that contains value as {@link Collection}
     * @throws Exception
     */
    @Test
    public void get_request_with_form_parameters_as_map_that_contains_collection_as_one_of_params() throws Exception {
        final List parameterValues = new ArrayList<String>() {{
            add("value1");
            add("value2");
        }};

        Map<String, Object> formParameters = new HashMap<String, Object>() {{
            put("list", parameterValues);
        }};

        Response response = given()
                .formParams(formParameters)
                .when()
                .post("/multiValueParam");
        //Convert returned value to list
        List<String> paramListFromResponse = Arrays.asList(response.jsonPath().getString("list").split(","));
        Assert.assertEquals(parameterValues,  paramListFromResponse);

    }
}
