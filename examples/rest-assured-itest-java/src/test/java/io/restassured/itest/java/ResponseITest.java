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

import io.restassured.builder.ResponseBuilder;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.Headers;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.parsing.Parser;
import io.restassured.path.json.JsonPath;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.path.json.exception.JsonPathException;
import io.restassured.path.xml.exception.XmlPathException;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.*;
import static io.restassured.config.JsonConfig.jsonConfig;
import static io.restassured.path.xml.XmlPath.CompatibilityMode.HTML;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ResponseITest extends WithJetty {

    @Test
    void whenNoExpectationsDefinedThenGetCanReturnBodyAsString() {
        final String body = get("/hello").asString();
        org.assertj.core.api.Assertions.assertThat(body).isEqualTo("{\"hello\":\"Hello Scalatra\"}");
    }

    @Test
    void whenNoExpectationsDefinedThenGetCanReturnAStringAsByteArray() {
        final byte[] expected = "{\"hello\":\"Hello Scalatra\"}".getBytes();
        final byte[] actual = get("/hello").asByteArray();
        org.assertj.core.api.Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    void whenExpectationsDefinedThenAsStringReturnsCanReturnTheResponseBody() {
        final String body = expect().body(equalTo("{\"hello\":\"Hello Scalatra\"}")).when().get("/hello").asString();

        assertThat(body, containsString("Hello"));
    }

    @Test
    void whenNoExpectationsDefinedThenPostCanReturnBodyAsString() {
        final String body = with().params("firstName", "John", "lastName", "Doe").post("/greetXML").andReturn().body().asString();
        org.assertj.core.api.Assertions.assertThat(body).isEqualTo("<greeting><firstName>John</firstName>\n      <lastName>Doe</lastName>\n    </greeting>");
    }

    @Test
    void whenNoExpectationsDefinedThenPostWithBodyCanReturnBodyAsString() {
        byte[] body = {23, 42, 127, 123};
        final String actual = given().body(body).when().post("/binaryBody").andReturn().asString();
        org.assertj.core.api.Assertions.assertThat(actual).isEqualTo("23, 42, 127, 123");
    }

    @Test
    void whenNoExpectationsDefinedThenPutCanReturnBodyAsString() {
        final String actual = given().cookies("username", "John", "token", "1234").when().put("/cookie").asString();
        org.assertj.core.api.Assertions.assertThat(actual).isEqualTo("username, token");
    }

    @Test
    void whenNoExpectationsDefinedThenPutWithBodyCanReturnBodyAsString() {
        final String body = given().body("a body").when().put("/body").andReturn().body().asString();
        org.assertj.core.api.Assertions.assertThat(body).isEqualTo("a body");
    }

    @Test
    void whenNoExpectationsDefinedThenDeleteWithBodyCanReturnBodyAsString() {
        final String actual = given().params("firstName", "John", "lastName", "Doe").when().delete("/greet").thenReturn().asString();
        org.assertj.core.api.Assertions.assertThat(actual).isEqualTo("{\"greeting\":\"Greetings John Doe\"}");
    }

    @Test
    void responseSupportsGettingCookies() {
        final Response response = get("/setCookies");
        org.assertj.core.api.Assertions.assertThat(response.getCookies().size()).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(response.cookies().size()).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(response.getCookie("key1")).isEqualTo("value1");
        org.assertj.core.api.Assertions.assertThat(response.cookie("key2")).isEqualTo("value2");
    }

    @Test
    void responseSupportsGettingHeaders() {
        final Response response = get("/setCookies");
        org.assertj.core.api.Assertions.assertThat(response.getHeaders().size()).isEqualTo(7);
        org.assertj.core.api.Assertions.assertThat(response.headers().size()).isEqualTo(7);
        org.assertj.core.api.Assertions.assertThat(response.getHeader("Content-Type")).isEqualTo("text/plain;charset=utf-8");
        final String server = response.header("Server");
        assertThat(server, containsString("Jetty"));
    }

    @Test
    void responseSupportsGettingStatusLine() {
        final Response response = get("/hello");

        assertThat(response.statusLine(), equalTo("HTTP/1.1 200 OK"));
        assertThat(response.getStatusLine(), equalTo("HTTP/1.1 200 OK"));
    }

    @Test
    void responseSupportsGettingStatusCode() {
        final Response response = get("/hello");

        assertThat(response.statusCode(), equalTo(200));
        assertThat(response.getStatusCode(), equalTo(200));
    }

    @Test
    void whenNoExpectationsDefinedThenGetCanReturnBodyAsInputStream() throws IOException {
        final InputStream inputStream = get("/hello").asInputStream();
        final String string = org.apache.commons.io.IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        assertThat(string, equalTo("{\"hello\":\"Hello Scalatra\"}"));
    }

    @Test
    void whenExpectationsDefinedThenGetCanReturnBodyAsInputStream() throws IOException {
        final InputStream inputStream = expect().body("hello", equalTo("Hello Scalatra")).when().get("/hello").asInputStream();
        final String string = org.apache.commons.io.IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        assertThat(string, equalTo("{\"hello\":\"Hello Scalatra\"}"));
    }

    @Test
    void whenExpectationsDefinedAndLoggingThenGetCanReturnBodyAsInputStream() throws IOException {
        final InputStream inputStream = expect().log().all().and().body("hello", equalTo("Hello Scalatra")).when().get("/hello").asInputStream();
        final String string = org.apache.commons.io.IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        assertThat(string, equalTo("{\"hello\":\"Hello Scalatra\"}"));
    }

    @Test
    void whenNoExpectationsDefinedButLoggingThenGetCanReturnBodyAsInputStream() throws IOException {
        final InputStream inputStream = expect().log().all().when().get("/hello").asInputStream();
        final String string = org.apache.commons.io.IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        assertThat(string, equalTo("{\"hello\":\"Hello Scalatra\"}"));
    }

    @Test
    void usingJsonPathViewFromTheResponse() {
        final String hello = get("/hello").andReturn().jsonPath().getString("hello");

        assertThat(hello, equalTo("Hello Scalatra"));
    }

    @Test
    void usingXmlPathViewFromTheResponse() {
        final String firstName = with().params("firstName", "John", "lastName", "Doe").post("/greetXML").andReturn().xmlPath().getString("greeting.firstName");

        assertThat(firstName, equalTo("John"));
    }

    @Test
    void usingXmlPathWithHtmlCompatibilityModeFromTheResponse() {
        // When
        final String title = get("/textHTML").xmlPath(HTML).getString("html.head.title");

        // Then
        assertThat(title, equalTo("my title"));
    }

    @Test
    void usingHtmlPathToParseHtmlFromTheResponse() {
        // When
        final String title = get("/textHTML").htmlPath().getString("html.head.title");

        // Then
        assertThat(title, equalTo("my title"));
    }

    @Test
    void usingPathWithContentTypeJsonFromTheResponse() {
        final String hello = get("/hello").andReturn().path("hello");

        assertThat(hello, equalTo("Hello Scalatra"));
    }

    @Test
    void usingPathWithParameters() {
        final String hello = get("/hello").andReturn().path("hel%s", "lo");

        assertThat(hello, equalTo("Hello Scalatra"));
    }

    @Test
    void usingPathWithContentTypeXmlFromTheResponse() {
        final String firstName = with().params("firstName", "John", "lastName", "Doe").post("/greetXML").andReturn().path("greeting.firstName");

        assertThat(firstName, equalTo("John"));
    }

    @Test
    void usingACustomRegisteredParserAllowsUsingPath() {
        final String message = expect().parser("application/vnd.uoml+something", Parser.JSON).when().get("/customMimeTypeJsonCompatible2").path("message");

        assertThat(message, equalTo("It works"));
    }

    @Test
    void usingADefaultParserAllowsUsingPath() {
        final String message = expect().defaultParser(Parser.JSON).when().get("/customMimeTypeJsonCompatible2").path("message");

        assertThat(message, equalTo("It works"));
    }

    @Test
    void responseTakeCharsetIntoAccount() {
        ResponseBuilder b = new ResponseBuilder();
        b.setHeaders(new Headers());
        b.setBody(new ByteArrayInputStream("äöü".getBytes(StandardCharsets.UTF_8)));
        b.setStatusCode(200);
        b.setContentType("application/json;charset=UTF-8");
        final Response response = b.build();
        org.assertj.core.api.Assertions.assertThat(response.asString()).isEqualTo("äöü");
    }

    @Test
    void jsonPathReturnedByResponseUsesConfigurationFromRestAssured() {
        // When
        final JsonPath jsonPath =
                given().
                        config(RestAssuredConfig.newConfig().with().jsonConfig(jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL))).
                        expect().
                        statusCode(200).
                        when().
                        get("/jsonStore").jsonPath();

        // Then
        assertThat(jsonPath.get("store.book.price.min()"), is(new BigDecimal("8.95")));
        assertThat(jsonPath.get("store.book.price.max()"), is(new BigDecimal("22.99")));
    }

    @Test
    void jsonPathWithConfigReturnedByResponseOverridesConfigurationFromRestAssured() {
        // When
        final JsonPath jsonPath =
                given().
                        config(RestAssuredConfig.newConfig().with().jsonConfig(jsonConfig().with().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL))).
                        expect().
                        statusCode(200).
                        body("store.book.price.min()", is(new BigDecimal("8.95"))).
                        when().
                        get("/jsonStore").jsonPath(JsonPathConfig.jsonPathConfig().with().numberReturnType(JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE));

        // Then
        assertThat(jsonPath.get("store.book.price.min()"), is(8.95f));
        assertThat(jsonPath.get("store.book.price.max()"), is(22.99f));
    }

    @Test
    void pathWorksForMultipleInvocationsWithJson() {
        Response response = get("/jsonStore");

        float minPrice = response.path("store.book.price.min()");
        float maxPrice = response.path("store.book.price.max()");

        assertThat(minPrice, is(8.95f));
        assertThat(maxPrice, is(22.99f));
    }

    @Test
    void pathThrowsExceptionWhenTryingToUseXmlPathAfterHavingUsedJsonPath() {
        assertThatThrownBy(() -> {
            Response response = get("/jsonStore");
            response.path("store.book.price.min()");
            response.xmlPath().get("store.book.price.min()");
        })
        .isInstanceOf(XmlPathException.class)
        .hasMessageContaining("Failed to parse the XML document");
    }

    @Test
    void pathWorksForMultipleInvocationsWithXml() {
        Response response = get("/videos");

        String title = response.path("videos.music[0].title.toString().trim()");
        String artist = response.path("videos.music[0].artist.toString().trim()");

        assertThat(title, equalTo("Video Title 1"));
        assertThat(artist, equalTo("Artist 1"));
    }

    @Test
    void pathThrowsExceptionWhenTryingToUseJsonPathAfterHavingUsedXmlPath() {
        assertThatThrownBy(() -> {
            Response response = get("/videos");
            response.path("videos.music[0].title.toString().trim()");
            response.jsonPath().get("videos");
        })
        .isInstanceOf(JsonPathException.class)
        .hasMessageContaining("Failed to parse the JSON document");
    }

    @Test
    void canParsePathAfterPrettyPrint() {
        Response response = get("/videos");

        response.prettyPrint();

        String title = response.path("videos.music[0].title.toString().trim()");
        String artist = response.path("videos.music[0].artist.toString().trim()");

        assertThat(title, equalTo("Video Title 1"));
        assertThat(artist, equalTo("Artist 1"));
    }

    @Test
    void canParsePathAfterPrint() {
        Response response = get("/videos");

        response.print();

        String title = response.path("videos.music[0].title.toString().trim()");
        String artist = response.path("videos.music[0].artist.toString().trim()");

        assertThat(title, equalTo("Video Title 1"));
        assertThat(artist, equalTo("Artist 1"));
    }

    @Test
    void canGetAsStringMultipleTimes() {
        // When
        Response response = get("/videos");

        response.asString();
        String string = response.asString();

        // Then
        assertThat(string, not(nullValue()));
    }

    @Test
    void canGetAsByteArrayMultipleTimes() {
        // When
        Response response = get("/videos");

        response.asByteArray();
        final byte[] bytes = response.asByteArray();

        // Then
        assertThat(bytes, not(nullValue()));
    }

    @Test
    void canCombineAsByteArrayWithPrettyPrintAndAsString() {
        // When
        Response response = get("/videos");

        response.asByteArray();
        response.prettyPrint();
        String string = response.asString();

        // Then
        assertThat(string, not(nullValue()));
    }

    @Test
    void canCombineAsStringWithPrettyPrintAndAsByteArray() {
        // When
        Response response = get("/videos");

        response.asString();
        response.prettyPrint();
        byte[] bytes = response.asByteArray();

        // Then
        assertThat(bytes, not(nullValue()));
    }

    @Test
    void canParsePathAfterPrettyPeek() {
        Response response = get("/videos").prettyPeek();

        String title = response.path("videos.music[0].title.toString().trim()");
        String artist = response.path("videos.music[0].artist.toString().trim()");

        assertThat(title, equalTo("Video Title 1"));
        assertThat(artist, equalTo("Artist 1"));
    }

    @Test
    void canParsePathAfterPeek() {
        Response response = get("/videos").peek();

        String title = response.path("videos.music[0].title.toString().trim()");
        String artist = response.path("videos.music[0].artist.toString().trim()");

        assertThat(title, equalTo("Video Title 1"));
        assertThat(artist, equalTo("Artist 1"));
    }
}
