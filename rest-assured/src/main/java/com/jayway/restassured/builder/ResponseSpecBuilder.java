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

package com.jayway.restassured.builder;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.internal.ResponseParserRegistrar;
import com.jayway.restassured.internal.ResponseSpecificationImpl;
import com.jayway.restassured.internal.SpecificationMerger;
import com.jayway.restassured.internal.log.LogRepository;
import com.jayway.restassured.parsing.Parser;
import com.jayway.restassured.specification.Argument;
import com.jayway.restassured.specification.ResponseSpecification;
import org.hamcrest.Matcher;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.responseContentType;
import static com.jayway.restassured.RestAssured.rootPath;

/**
 * You can use the builder to construct a response specification. The specification can be used as e.g.
 * <pre>
 * ResponseSpecification responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
 * RequestSpecification requestSpec = new RequestSpecBuilder().addParam("parameter1", "value1").build();
 *
 * given(responseSpec, requestSpec).post("/something");
 * </pre>
 * <p/>
 * or
 * <pre>
 * ResponseSpecification responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
 *
 * expect().
 *         spec(responseSpec).
 *         body("x.y.z", equalTo("something")).
 * when().
 *        get("/something");
 * </pre>
 */
public class ResponseSpecBuilder {
    private final ResponseSpecification spec;

    public ResponseSpecBuilder() {
        spec = new ResponseSpecificationImpl(rootPath, responseContentType(), null, getResponseParserRegistrar(), restAssuredConfig(), new LogRepository());
    }

    /**
     * Expect that the response content conforms to one or more Hamcrest matchers.
     *
     * @param matcher The hamcrest matcher that must response content must match.
     * @return The builder
     */
    public ResponseSpecBuilder expectContent(Matcher<?> matcher) {
        spec.content(matcher);
        return this;
    }

    /**
     * Expect that the JSON or XML response content conforms to one or more Hamcrest matchers.<br>
     * <h3>JSON example</h3>
     * <p/>
     * Assume that a GET request to "/lotto" returns a JSON response containing:
     * <pre>
     * { "lotto":{
     *   "lottoId":5,
     *   "winning-numbers":[2,45,34,23,7,5,3],
     *   "winners":[{
     *     "winnerId":23,
     *     "numbers":[2,45,34,23,3,5]
     *   },{
     *     "winnerId":54,
     *     "numbers":[52,3,12,11,18,22]
     *   }]
     *  }}
     * </pre>
     * <p/>
     * You can verify that the lottoId is equal to 5 like this:
     * <pre>
     * ResponseSpecBuilder builder = new ResponseSpecBuilder();
     * builder.expectContent("lotto.lottoId", equalTo(5));
     * </pre>
     *
     * @param matcher The hamcrest matcher that the response content must match.
     * @return The builder
     */
    public ResponseSpecBuilder expectContent(String path, Matcher<?> matcher) {
        spec.content(path, matcher);
        return this;
    }

    /**
     * Same as {@link #expectContent(String, org.hamcrest.Matcher)} expect that you can pass arguments to the path. This
     * is useful in situations where you have e.g. pre-defined variables that constitutes the path:
     * <pre>
     * String someSubPath = "else";
     * int index = 1;
     * expect().body("something.%s[%d]", withArgs(someSubPath, index), equalTo("some value")). ..
     * </pre>
     * <p/>
     * or if you have complex root paths and don't wish to duplicate the path for small variations:
     * <pre>
     * expect().
     *          root("filters.filterConfig[%d].filterConfigGroups.find { it.name == 'Gold' }.includes").
     *          body(withArgs(0), hasItem("first")).
     *          body(withArgs(1), hasItem("second")).
     *          ..
     * </pre>
     * <p/>
     * The path and arguments follows the standard <a href="http://download.oracle.com/javase/1,5.0/docs/api/java/util/Formatter.html#syntax">formatting syntax</a> of Java.
     * <p>
     * Note that <code>withArgs</code> can be statically imported from the <code>com.jayway.restassured.RestAssured</code> class.
     * </p>
     *
     * @param path    The body path
     * @param matcher The hamcrest matcher that must response body must match.
     * @return the response specification
     * @see #expectContent(String, org.hamcrest.Matcher)
     */
    public ResponseSpecBuilder expectContent(String path, List<Argument> arguments, Matcher<?> matcher) {
        spec.content(path, arguments, matcher);
        return this;
    }

    /**
     * Expect that the response status code matches the given Hamcrest matcher.
     *
     * @param expectedStatusCode The expected status code matcher.
     * @return The builder
     */
    public ResponseSpecBuilder expectStatusCode(Matcher<Integer> expectedStatusCode) {
        spec.statusCode(expectedStatusCode);
        return this;
    }

    /**
     * Expect that the response status code matches an integer.
     *
     * @param expectedStatusCode The expected status code.
     * @return The builder
     */
    public ResponseSpecBuilder expectStatusCode(int expectedStatusCode) {
        spec.statusCode(expectedStatusCode);
        return this;
    }

    /**
     * Expect that the response status line matches the given Hamcrest matcher.
     *
     * @param expectedStatusLine The expected status line matcher.
     * @return The builder
     */
    public ResponseSpecBuilder expectStatusLine(Matcher<String> expectedStatusLine) {
        spec.statusLine(expectedStatusLine);
        return this;
    }

    /**
     * Expect that the response status line matches the given String.
     *
     * @param expectedStatusLine The expected status line.
     * @return The builder
     */
    public ResponseSpecBuilder expectStatusLine(String expectedStatusLine) {
        spec.statusLine(expectedStatusLine);
        return this;
    }

    /**
     * Expect that response headers matches those specified in a Map.
     * <p>
     * E.g. expect that the response of the GET request to "/something" contains header <tt>headerName1=headerValue1</tt>
     * and <tt>headerName2=headerValue2</tt>:
     * <pre>
     * Map expectedHeaders = new HashMap();
     * expectedHeaders.put("headerName1", "headerValue1"));
     * expectedHeaders.put("headerName2", "headerValue2");
     * </pre>
     * </p>
     * <p/>
     * <p>
     * You can also use Hamcrest matchers:
     * <pre>
     * Map expectedHeaders = new HashMap();
     * expectedHeaders.put("Content-Type", containsString("charset=UTF-8"));
     * expectedHeaders.put("Content-Length", "160");
     * </pre>
     * </p>
     *
     * @param expectedHeaders The Map of expected response headers
     * @return The builder
     */
    public ResponseSpecBuilder expectHeaders(Map<String, Object> expectedHeaders) {
        spec.headers(expectedHeaders);
        return this;
    }

    /**
     * Expect that a response header matches the supplied header name and hamcrest matcher.
     *
     * @param headerName           The name of the expected header
     * @param expectedValueMatcher The Hamcrest matcher that must conform to the value
     * @return The builder
     */
    public ResponseSpecBuilder expectHeader(String headerName, Matcher<String> expectedValueMatcher) {
        spec.header(headerName, expectedValueMatcher);
        return this;
    }

    /**
     * Expect that a response header matches the supplied name and value.
     *
     * @param headerName    The name of the expected header
     * @param expectedValue The value of the expected header
     * @return The builder
     */
    public ResponseSpecBuilder expectHeader(String headerName, String expectedValue) {
        spec.header(headerName, expectedValue);
        return this;
    }

    /**
     * Expect that response cookies matches those specified in a Map.
     * <p>
     * E.g. expect that the response of the GET request to "/something" contains cookies <tt>cookieName1=cookieValue1</tt>
     * and <tt>cookieName2=cookieValue2</tt>:
     * <pre>
     * Map expectedCookies = new HashMap();
     * expectedCookies.put("cookieName1", "cookieValue1"));
     * expectedCookies.put("cookieName2", "cookieValue2");
     * </pre>
     * <p>
     * You can also use Hamcrest matchers:
     * <pre>
     * Map expectedCookies = new HashMap();
     * expectedCookies.put("cookieName1", containsString("Value1"));
     * expectedCookies.put("cookieName2", "cookieValue2");
     * </pre>
     * </p>
     *
     * @param expectedCookies A Map of expected response cookies
     * @return The builder
     */
    public ResponseSpecBuilder expectCookies(Map<String, Object> expectedCookies) {
        spec.cookies(expectedCookies);
        return this;
    }

    /**
     * Expect that a response cookie matches the supplied cookie name and hamcrest matcher.
     * <p>
     * E.g. <tt>cookieName1=cookieValue1</tt>
     * </p>
     *
     * @param cookieName           The name of the expected cookie
     * @param expectedValueMatcher The Hamcrest matcher that must conform to the value
     * @return The builder
     */
    public ResponseSpecBuilder expectCookie(String cookieName, Matcher<String> expectedValueMatcher) {
        spec.cookie(cookieName, expectedValueMatcher);
        return this;
    }

    /**
     * Expect that a response cookie matches the supplied name and value.
     *
     * @param cookieName    The name of the expected cookie
     * @param expectedValue The value of the expected cookie
     * @return The builder
     */
    public ResponseSpecBuilder expectCookie(String cookieName, String expectedValue) {
        spec.cookie(cookieName, expectedValue);
        return this;
    }

    /**
     * Expect that a cookie exist in the response, regardless of value (it may have no value at all).
     *
     * @param cookieName the cookie to validate that it exists
     * @return the response specification
     */
    public ResponseSpecBuilder expectCookie(String cookieName) {
        spec.cookie(cookieName);
        return this;
    }

    /**
     * Set the root path of the response body so that you don't need to write the entire path for each expectation.
     * E.g. instead of writing:
     * <p/>
     * <pre>
     * expect().
     *          body("x.y.firstName", is(..)).
     *          body("x.y.lastName", is(..)).
     *          body("x.y.age", is(..)).
     *          body("x.y.gender", is(..)).
     * when().
     *          get(..);
     * </pre>
     * <p/>
     * you can use a root path and do:
     * <pre>
     * expect().
     *          rootPath("x.y").
     *          body("firstName", is(..)).
     *          body("lastName", is(..)).
     *          body("age", is(..)).
     *          body("gender", is(..)).
     * when().
     *          get(..);
     * </pre>
     *
     * @param rootPath The root path to use.
     */
    public ResponseSpecBuilder rootPath(String rootPath) {
        spec.rootPath(rootPath);
        return this;
    }

    /**
     * Set the root path of the response body so that you don't need to write the entire path for each expectation. The same as {@link #rootPath(String)}
     * but also provides a way to defined arguments.
     *
     * @param rootPath  The root path to use.
     * @param arguments The arguments.
     * @see ResponseSpecification#rootPath(String, java.util.List)
     */
    public ResponseSpecBuilder rootPath(String rootPath, List<Argument> arguments) {
        spec.rootPath(rootPath, arguments);
        return this;
    }

    /**
     * Append the given path to the root path of the response body so that you don't need to write the entire path for each expectation.
     * E.g. instead of writing:
     * <p/>
     * <pre>
     * expect().
     *          root("x.y").
     *          body("age", is(..)).
     *          body("gender", is(..)).
     *          body("name.firstName", is(..)).
     *          body("name.lastName", is(..)).
     * when().
     *          get(..);
     * </pre>
     * <p/>
     * you can use a append root and do:
     * <pre>
     * expect().
     *          root("x.y").
     *          body("age", is(..)).
     *          body("gender", is(..)).
     *          appendRoot("name").
     *          body("firstName", is(..)).
     *          body("lastName", is(..)).
     * when().
     *          get(..);
     * </pre>
     *
     * @param pathToAppend The root path to append.
     */
    public ResponseSpecBuilder appendRootPath(String pathToAppend) {
        spec.appendRoot(pathToAppend);
        return this;
    }

    /**
     * Append the given path to the root path with arguments supplied of the response body so that you don't need to write the entire path for each expectation.
     * This is mainly useful when you have parts of the path defined in variables.
     * E.g. instead of writing:
     * <p/>
     * <pre>
     * String namePath = "name";
     * expect().
     *          root("x.y").
     *          body("age", is(..)).
     *          body("gender", is(..)).
     *          body(namePath + "first", is(..)).
     *          body(namePath + "last", is(..)).
     * when().
     *          get(..);
     * </pre>
     * <p/>
     * you can use a append root and do:
     * <pre>
     * String namePath = "name";
     * expect().
     *          root("x.y").
     *          body("age", is(..)).
     *          body("gender", is(..)).
     *          appendRoot("%s", withArgs(namePath)).
     *          body("first", is(..)).
     *          body("last", is(..)).
     * when().
     *          get(..);
     * </pre>
     *
     * @param pathToAppend The root path to use. The path and arguments follows the standard <a href="http://download.oracle.com/javase/1,5.0/docs/api/java/util/Formatter.html#syntax">formatting syntax</a> of Java.
     */
    public ResponseSpecBuilder appendRootPath(String pathToAppend, List<Argument> arguments) {
        spec.appendRoot(pathToAppend, arguments);
        return this;
    }

    /**
     * Reset the root path of the response body so that you don't need to write the entire path for each expectation.
     * For example:
     * <p/>
     * <pre>
     * expect().
     *          root("x.y").
     *          body("firstName", is(..)).
     *          body("lastName", is(..)).
     *          noRoot()
     *          body("z.something1", is(..)).
     *          body("w.something2", is(..)).
     * when().
     *          get(..);
     * </pre>
     * <p/>
     * This is the same as calling <code>rootPath("")</code> but more expressive.
     *
     * @see #rootPath(String)
     */
    public ResponseSpecBuilder noRootPath() {
        spec.noRootPath();
        return this;
    }

    /**
     * Detach the given path from the root path.
     * E.g. instead of writing:
     * <p/>
     * <pre>
     * when().
     *          get(..);
     * then().
     *          root("x.y").
     *          body("age", is(..)).
     *          body("gender", is(..)).
     *          root("x").
     *          body("firstName", is(..)).
     *          body("lastName", is(..)).
     * </pre>
     * <p/>
     * you can use a append root and do:
     * <pre>
     * when().
     *          get(..);
     * then().
     *          root("x.y").
     *          body("age", is(..)).
     *          body("gender", is(..)).
     *          detachRoot("y").
     *          body("firstName", is(..)).
     *          body("lastName", is(..)).
     * </pre>
     *
     * @param pathToDetach The root path to detach.
     */
    public ResponseSpecBuilder detachRootPath(String pathToDetach) {
        spec.detachRoot(pathToDetach);
        return this;
    }

    /**
     * Set the response content type to be <code>contentType</code>.
     * <p>Note that this will affect the way the response is decoded.
     * E,g. if you can't use JSON/XML matching (see e.g. {@link #expectBody(String, org.hamcrest.Matcher)}) if you specify a
     * content-type of "text/plain". If you don't specify the response content type REST Assured will automatically try to
     * figure out which content type to use.</p>
     *
     * @param contentType The content type of the response.
     * @return The builder
     */
    public ResponseSpecBuilder expectContentType(ContentType contentType) {
        spec.contentType(contentType);
        return this;
    }

    /**
     * Set the response content type to be <code>contentType</code>.
     * <p>Note that this will affect the way the response is decoded.
     * E,g. if you can't use JSON/XML matching (see e.g. {@link #expectBody(String, org.hamcrest.Matcher)}) if you specify a
     * content-type of "text/plain". If you don't specify the response content type REST Assured will automatically try to
     * figure out which content type to use.</p>
     *
     * @param contentType The content type of the response.
     * @return The builder
     */
    public ResponseSpecBuilder expectContentType(String contentType) {
        spec.contentType(contentType);
        return this;
    }

    /**
     * Expect that the response content conforms to one or more Hamcrest matchers.
     *
     * @param matcher The hamcrest matcher that must response content must match.
     * @return The builder
     */
    public ResponseSpecBuilder expectBody(Matcher<?> matcher) {
        spec.body(matcher);
        return this;
    }

    /**
     * Expect that the JSON or XML response content conforms to one or more Hamcrest matchers.<br>
     * <h3>JSON example</h3>
     * <p/>
     * Assume that a GET request to "/lotto" returns a JSON response containing:
     * <pre>
     * { "lotto":{
     *   "lottoId":5,
     *   "winning-numbers":[2,45,34,23,7,5,3],
     *   "winners":[{
     *     "winnerId":23,
     *     "numbers":[2,45,34,23,3,5]
     *   },{
     *     "winnerId":54,
     *     "numbers":[52,3,12,11,18,22]
     *   }]
     *  }}
     * </pre>
     * <p/>
     * You can verify that the lottoId is equal to 5 like this:
     * <pre>
     * ResponseSpecBuilder builder = new ResponseSpecBuilder();
     * builder.expectBody("lotto.lottoId", equalTo(5));
     * </pre>
     *
     * @param matcher The hamcrest matcher that the response content must match.
     * @return The builder
     */
    public ResponseSpecBuilder expectBody(String path, Matcher<?> matcher) {
        spec.body(path, matcher);
        return this;
    }

    /**
     * Same as {@link #expectBody(String, org.hamcrest.Matcher)} expect that you can pass arguments to the path. This
     * is useful in situations where you have e.g. pre-defined variables that constitutes the path:
     * <pre>
     * String someSubPath = "else";
     * int index = 1;
     * expect().body("something.%s[%d]", withArgs(someSubPath, index), equalTo("some value")). ..
     * </pre>
     * <p/>
     * or if you have complex root paths and don't wish to duplicate the path for small variations:
     * <pre>
     * expect().
     *          root("filters.filterConfig[%d].filterConfigGroups.find { it.name == 'Gold' }.includes").
     *          body(withArgs(0), hasItem("first")).
     *          body(withArgs(1), hasItem("second")).
     *          ..
     * </pre>
     * <p/>
     * The path and arguments follows the standard <a href="http://download.oracle.com/javase/1,5.0/docs/api/java/util/Formatter.html#syntax">formatting syntax</a> of Java.
     * <p>
     * Note that <code>withArgs</code> can be statically imported from the <code>com.jayway.restassured.RestAssured</code> class.
     * </p>
     *
     * @param path    The body path
     * @param matcher The hamcrest matcher that must response body must match.
     * @return the response specification
     * @see #expectBody(String, org.hamcrest.Matcher)
     */
    public ResponseSpecBuilder expectBody(String path, List<Argument> arguments, Matcher<?> matcher) {
        spec.body(path, arguments, matcher);
        return this;
    }


    /**
     * Merge this builder with settings from another specification. Note that the supplied specification
     * can overwrite data in the current specification. The following settings are overwritten:
     * <ul>
     * <li>Content type</li>
     * <li>Root path</
     * <li>Status code</li>
     * <li>Status line</li>
     * </ul>
     * The following settings are merged:
     * <ul>
     * <li>Response body expectations</li>
     * <li>Cookies</li>
     * <li>Headers</li>
     * </ul>
     *
     * @param specification The specification the add.
     * @return The builder
     */
    public ResponseSpecBuilder addResponseSpecification(ResponseSpecification specification) {
        if (!(specification instanceof ResponseSpecificationImpl)) {
            throw new IllegalArgumentException("specification must be of type " + ResponseSpecificationImpl.class.getClass() + ".");
        }

        ResponseSpecificationImpl rs = (ResponseSpecificationImpl) specification;
        SpecificationMerger.merge((ResponseSpecificationImpl) spec, rs);
        return this;
    }

    /**
     * Register a content-type to be parsed using a predefined parser. E.g. let's say you want parse
     * content-type <tt>application/vnd.uoml+xml</tt> with the XML parser to be able to verify the response using the XML dot notations:
     * <pre>
     * expect().body("document.child", equalsTo("something"))..
     * </pre>
     * Since <tt>application/vnd.uoml+xml</tt> is not registered to be processed by the XML parser by default you need to explicitly
     * tell REST Assured to use this parser before making the request:
     * <pre>
     * expect().parser("application/vnd.uoml+xml", Parser.XML").when(). ..;
     * </pre>
     * <p/>
     * You can also specify by default by using:
     * <pre>
     * RestAssured.registerParser("application/vnd.uoml+xml, Parser.XML");
     * </pre>
     *
     * @param contentType The content-type to register
     * @param parser      The parser to use when verifying the response.
     * @return The builder
     */
    public ResponseSpecBuilder registerParser(String contentType, Parser parser) {
        spec.parser(contentType, parser);
        return this;
    }

    /**
     * Register a default predefined parser that will be used if no other parser (registered or pre-defined) matches the response
     * content-type. E.g. let's say that for some reason no content-type is defined in the response but the content is nevertheless
     * JSON encoded. To be able to expect the content in REST Assured you need to set the default parser:
     * <pre>
     * expect().defaultParser(Parser.JSON).when(). ..;
     * </pre>
     * <p/>
     * You can also specify it for every response by using:
     * <pre>
     * RestAssured.defaultParser(Parser.JSON);
     * </pre>
     *
     * @param parser The parser to use when verifying the response if no other parser is found for the response content-type.
     */
    public ResponseSpecBuilder setDefaultParser(Parser parser) {
        spec.defaultParser(parser);
        return this;
    }

    /**
     * Build the response specification.
     *
     * @return The assembled response specification
     */
    public ResponseSpecification build() {
        return spec;
    }

    private ResponseParserRegistrar getResponseParserRegistrar() {
        ResponseParserRegistrar rpr;
        Field registrarField = null;
        try {
            registrarField = RestAssured.class.getDeclaredField("RESPONSE_PARSER_REGISTRAR");
            try {
                registrarField.setAccessible(true);
                rpr = (ResponseParserRegistrar) registrarField.get(RestAssured.class);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } finally {
            registrarField.setAccessible(false);
        }
        return new ResponseParserRegistrar(rpr);
    }

    private RestAssuredConfig restAssuredConfig() {
        return RestAssured.config == null ? new RestAssuredConfig() : RestAssured.config;
    }
}