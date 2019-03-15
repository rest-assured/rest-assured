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
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;

import static io.restassured.RestAssured.*;
import static io.restassured.config.JsonConfig.jsonConfig;
import static io.restassured.path.xml.XmlPath.CompatibilityMode.HTML;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class ResponseITest extends WithJetty {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void whenNoExpectationsDefinedThenGetCanReturnBodyAsString() throws Exception {
        final String body = get("/hello").asString();
        assertEquals("{\"hello\":\"Hello Scalatra\"}", body);
    }

    @Test
    public void whenNoExpectationsDefinedThenGetCanReturnAStringAsByteArray() throws Exception {
        final byte[] expected = "{\"hello\":\"Hello Scalatra\"}".getBytes();
        final byte[] actual = get("/hello").asByteArray();
        assertArrayEquals(expected, actual);
    }

    @Test
    public void whenExpectationsDefinedThenAsStringReturnsCanReturnTheResponseBody() throws Exception {
        final String body = expect().body(equalTo("{\"hello\":\"Hello Scalatra\"}")).when().get("/hello").asString();

        assertThat(body, containsString("Hello"));
    }

    @Test
    public void whenNoExpectationsDefinedThenPostCanReturnBodyAsString() throws Exception {
        final String body = with().params("firstName", "John", "lastName", "Doe").post("/greetXML").andReturn().body().asString();
        assertEquals("<greeting><firstName>John</firstName>\n" +
                "      <lastName>Doe</lastName>\n" +
                "    </greeting>", body);
    }

    @Test
    public void whenNoExpectationsDefinedThenPostWithBodyCanReturnBodyAsString() throws Exception {
        byte[] body = {23, 42, 127, 123};
        final String actual = given().body(body).when().post("/binaryBody").andReturn().asString();
        assertEquals("23, 42, 127, 123", actual);
    }

    @Test
    public void whenNoExpectationsDefinedThenPutCanReturnBodyAsString() throws Exception {
        final String actual = given().cookies("username", "John", "token", "1234").when().put("/cookie").asString();
        assertEquals("username, token", actual);
    }

    @Test
    public void whenNoExpectationsDefinedThenPutWithBodyCanReturnBodyAsString() throws Exception {
        final String body = given().body("a body").when().put("/body").andReturn().body().asString();
        assertEquals("a body", body);
    }

    @Test
    public void whenNoExpectationsDefinedThenDeleteWithBodyCanReturnBodyAsString() throws Exception {
        final String actual = given().params("firstName", "John", "lastName", "Doe").when().delete("/greet").thenReturn().asString();
        assertEquals("{\"greeting\":\"Greetings John Doe\"}", actual);
    }

    @Test
    public void responseSupportsGettingCookies() throws Exception {
        final Response response = get("/setCookies");
        assertEquals(3, response.getCookies().size());
        assertEquals(3, response.cookies().size());
        assertEquals("value1", response.getCookie("key1"));
        assertEquals("value2", response.cookie("key2"));
    }

    @Test
    public void responseSupportsGettingHeaders() throws Exception {
        final Response response = get("/setCookies");
        assertEquals(7, response.getHeaders().size());
        assertEquals(7, response.headers().size());
        assertEquals("text/plain;charset=utf-8", response.getHeader("Content-Type"));
        final String server = response.header("Server");
        assertThat(server, containsString("Jetty"));
    }

    @Test
    public void responseSupportsGettingStatusLine() throws Exception {
        final Response response = get("/hello");

        assertThat(response.statusLine(), equalTo("HTTP/1.1 200 OK"));
        assertThat(response.getStatusLine(), equalTo("HTTP/1.1 200 OK"));
    }

    @Test
    public void responseSupportsGettingStatusCode() throws Exception {
        final Response response = get("/hello");

        assertThat(response.statusCode(), equalTo(200));
        assertThat(response.getStatusCode(), equalTo(200));
    }

    @Test
    public void whenNoExpectationsDefinedThenGetCanReturnBodyAsInputStream() throws Exception {
        final InputStream inputStream = get("/hello").asInputStream();
        final String string = IOUtils.toString(inputStream);

        assertThat(string, equalTo("{\"hello\":\"Hello Scalatra\"}"));
    }

    @Test
    public void whenExpectationsDefinedThenGetCanReturnBodyAsInputStream() throws Exception {
        final InputStream inputStream = expect().body("hello", equalTo("Hello Scalatra")).when().get("/hello").asInputStream();
        final String string = IOUtils.toString(inputStream);

        assertThat(string, equalTo("{\"hello\":\"Hello Scalatra\"}"));
    }

    @Test
    public void whenExpectationsDefinedAndLoggingThenGetCanReturnBodyAsInputStream() throws Exception {
        final InputStream inputStream = expect().log().all().and().body("hello", equalTo("Hello Scalatra")).when().get("/hello").asInputStream();
        final String string = IOUtils.toString(inputStream);

        assertThat(string, equalTo("{\"hello\":\"Hello Scalatra\"}"));
    }

    @Test
    public void whenNoExpectationsDefinedButLoggingThenGetCanReturnBodyAsInputStream() throws Exception {
        final InputStream inputStream = expect().log().all().when().get("/hello").asInputStream();
        final String string = IOUtils.toString(inputStream);

        assertThat(string, equalTo("{\"hello\":\"Hello Scalatra\"}"));
    }

    @Test
    public void usingJsonPathViewFromTheResponse() throws Exception {
        final String hello = get("/hello").andReturn().jsonPath().getString("hello");

        assertThat(hello, equalTo("Hello Scalatra"));
    }

    @Test
    public void usingXmlPathViewFromTheResponse() throws Exception {
        final String firstName = with().params("firstName", "John", "lastName", "Doe").post("/greetXML").andReturn().xmlPath().getString("greeting.firstName");

        assertThat(firstName, equalTo("John"));
    }

    @Test
    public void usingXmlPathWithHtmlCompatibilityModeFromTheResponse() throws Exception {
        // When
        final String title = get("/textHTML").xmlPath(HTML).getString("html.head.title");

        // Then
        assertThat(title, equalTo("my title"));
    }

    @Test
    public void usingHtmlPathToParseHtmlFromTheResponse() throws Exception {
        // When
        final String title = get("/textHTML").htmlPath().getString("html.head.title");

        // Then
        assertThat(title, equalTo("my title"));
    }

    @Test
    public void usingPathWithContentTypeJsonFromTheResponse() throws Exception {
        final String hello = get("/hello").andReturn().path("hello");

        assertThat(hello, equalTo("Hello Scalatra"));
    }

    @Test
    public void usingPathWithParameters() throws Exception {
        final String hello = get("/hello").andReturn().path("hel%s", "lo");

        assertThat(hello, equalTo("Hello Scalatra"));
    }

    @Test
    public void usingPathWithContentTypeXmlFromTheResponse() throws Exception {
        final String firstName = with().params("firstName", "John", "lastName", "Doe").post("/greetXML").andReturn().path("greeting.firstName");

        assertThat(firstName, equalTo("John"));
    }

    @Test
    public void usingACustomRegisteredParserAllowsUsingPath() throws Exception {
        final String message = expect().parser("application/vnd.uoml+something", Parser.JSON).when().get("/customMimeTypeJsonCompatible2").path("message");

        assertThat(message, equalTo("It works"));
    }

    @Test
    public void usingADefaultParserAllowsUsingPath() throws Exception {
        final String message = expect().defaultParser(Parser.JSON).when().get("/customMimeTypeJsonCompatible2").path("message");

        assertThat(message, equalTo("It works"));
    }

    @Test
    public void responseTakeCharsetIntoAccount() throws Exception {
        ResponseBuilder b = new ResponseBuilder();
        b.setHeaders(new Headers());
        b.setBody(new ByteArrayInputStream("äöü".getBytes("UTF-8")));
        b.setStatusCode(200);
        b.setContentType("application/json;charset=UTF-8");
        final Response response = b.build();
        assertThat("äöü", equalTo(response.asString()));
    }

    @Test
    public void jsonPathReturnedByResponseUsesConfigurationFromRestAssured() throws Exception {
        // When
        final JsonPath jsonPath =
                given().
                        config(RestAssuredConfig.newConfig().with().jsonConfig(jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL))).
                        expect().
                        statusCode(200).
                        when().
                        get("/jsonStore").jsonPath();

        // Then
        assertThat(jsonPath.<BigDecimal>get("store.book.price.min()"), is(new BigDecimal("8.95")));
        assertThat(jsonPath.<BigDecimal>get("store.book.price.max()"), is(new BigDecimal("22.99")));
    }

    @Test
    public void jsonPathWithConfigReturnedByResponseOverridesConfigurationFromRestAssured() throws Exception {
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
        assertThat(jsonPath.<Float>get("store.book.price.min()"), is(8.95f));
        assertThat(jsonPath.<Float>get("store.book.price.max()"), is(22.99f));
    }

    @Test
    public void pathWorksForMultipleInvocationsWithJson() throws Exception {
        Response response = get("/jsonStore");

        float minPrice = response.path("store.book.price.min()");
        float maxPrice = response.path("store.book.price.max()");

        assertThat(minPrice, is(8.95f));
        assertThat(maxPrice, is(22.99f));
    }

    @Test
    public void pathThrowsExceptionWhenTryingToUseXmlPathAfterHavingUsedJsonPath() throws Exception {
        exception.expect(XmlPathException.class);
        exception.expectMessage("Failed to parse the XML document");

        Response response = get("/jsonStore");

        response.path("store.book.price.min()");
        response.xmlPath().get("store.book.price.min()");
    }

    @Test
    public void pathWorksForMultipleInvocationsWithXml() throws Exception {
        Response response = get("/videos");

        String title = response.path("videos.music[0].title.toString().trim()");
        String artist = response.path("videos.music[0].artist.toString().trim()");

        assertThat(title, equalTo("Video Title 1"));
        assertThat(artist, equalTo("Artist 1"));
    }

    @Test
    public void pathThrowsExceptionWhenTryingToUseJsonPathAfterHavingUsedXmlPath() throws Exception {
        exception.expect(JsonPathException.class);
        exception.expectMessage("Failed to parse the JSON document");

        Response response = get("/videos");

        response.path("videos.music[0].title.toString().trim()");
        response.jsonPath().get("videos");
    }

    @Test
    public void canParsePathAfterPrettyPrint() throws Exception {
        Response response = get("/videos");

        response.prettyPrint();

        String title = response.path("videos.music[0].title.toString().trim()");
        String artist = response.path("videos.music[0].artist.toString().trim()");

        assertThat(title, equalTo("Video Title 1"));
        assertThat(artist, equalTo("Artist 1"));
    }

    @Test
    public void canParsePathAfterPrint() throws Exception {
        Response response = get("/videos");

        response.print();

        String title = response.path("videos.music[0].title.toString().trim()");
        String artist = response.path("videos.music[0].artist.toString().trim()");

        assertThat(title, equalTo("Video Title 1"));
        assertThat(artist, equalTo("Artist 1"));
    }

    @Test
    public void canGetAsStringMultipleTimes() throws Exception {
        // When
        Response response = get("/videos");

        response.asString();
        String string = response.asString();

        // Then
        assertThat(string, not(nullValue()));
    }

    @Test
    public void canGetAsByteArrayMultipleTimes() throws Exception {
        // When
        Response response = get("/videos");

        response.asByteArray();
        final byte[] bytes = response.asByteArray();

        // Then
        assertThat(bytes, not(nullValue()));
    }

    @Test
    public void canCombineAsByteArrayWithPrettyPrintAndAsString() throws Exception {
        // When
        Response response = get("/videos");

        response.asByteArray();
        response.prettyPrint();
        String string = response.asString();

        // Then
        assertThat(string, not(nullValue()));
    }

    @Test
    public void canCombineAsStringWithPrettyPrintAndAsByteArray() throws Exception {
        // When
        Response response = get("/videos");

        response.asString();
        response.prettyPrint();
        byte[] bytes = response.asByteArray();

        // Then
        assertThat(bytes, not(nullValue()));
    }

    @Test
    public void canParsePathAfterPrettyPeek() throws Exception {
        Response response = get("/videos").prettyPeek();

        String title = response.path("videos.music[0].title.toString().trim()");
        String artist = response.path("videos.music[0].artist.toString().trim()");

        assertThat(title, equalTo("Video Title 1"));
        assertThat(artist, equalTo("Artist 1"));
    }

    @Test
    public void canParsePathAfterPeek() throws Exception {
        Response response = get("/videos").peek();

        String title = response.path("videos.music[0].title.toString().trim()");
        String artist = response.path("videos.music[0].artist.toString().trim()");

        assertThat(title, equalTo("Video Title 1"));
        assertThat(artist, equalTo("Artist 1"));
    }
}
