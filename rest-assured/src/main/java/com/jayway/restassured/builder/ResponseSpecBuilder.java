/*
 * Copyright 2011 the original author or authors.
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

import com.jayway.restassured.internal.ResponseSpecificationImpl;
import com.jayway.restassured.internal.SpecificationMerger;
import com.jayway.restassured.specification.ResponseSpecification;
import groovyx.net.http.ContentType;
import org.hamcrest.Matcher;

import java.util.Map;

import static com.jayway.restassured.RestAssured.rootPath;

/**
 * You can use the builder to construct a response specification. The specification can be used as e.g.
 * <pre>
 * ResponseSpecification responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
 * RequestSpecification requestSpec = new RequestSpecBuilder().addParam("parameter1", "value1").build();
 *
 * given(responseSpec, requestSpec).post("/something");
 * </pre>
 *
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
        spec = new ResponseSpecificationImpl(rootPath);
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
     * <p>
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
     *
     * You can verify that the lottoId is equal to 5 like this:
     * <pre>
     * ResponseSpecBuilder builder = new ResponseSpecBuilder();
     * builder.expectContent("lotto.lottoId", equalTo(5));
     * </pre>
     *
     * @param matcher The hamcrest matcher that the response content must match.
     * @return The builder
     */
    public ResponseSpecBuilder expectContent(String key, Matcher<?> matcher) {
        spec.content(key, matcher);
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
     *
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
     * @param headerName The name of the expected header
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
     * @param headerName The name of the expected header
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
     * @param cookieName The name of the expected cookie
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
     * @param cookieName The name of the expected cookie
     * @param expectedValue The value of the expected cookie
     * @return The builder
     */
    public ResponseSpecBuilder expectCookie(String cookieName, String expectedValue) {
        spec.cookie(cookieName, expectedValue);
        return this;
    }

    /**
     * Set the root path of the response body so that you don't need to write the entire path for each expectation.
     * E.g. instead of writing:
     *
     * <pre>
     * expect().
     *          body("x.y.firstName", is(..)).
     *          body("x.y.lastName", is(..)).
     *          body("x.y.age", is(..)).
     *          body("x.y.gender", is(..)).
     * when().
     *          get(..);
     *</pre>
     *
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
     * <p>
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
     *
     * You can verify that the lottoId is equal to 5 like this:
     * <pre>
     * ResponseSpecBuilder builder = new ResponseSpecBuilder();
     * builder.expectBody("lotto.lottoId", equalTo(5));
     * </pre>
     *
     * @param matcher The hamcrest matcher that the response content must match.
     * @return The builder
     */
    public ResponseSpecBuilder expectBody(String key, Matcher<?> matcher) {
        spec.body(key, matcher);
        return this;
    }

    /**
     * Merge this builder with settings from another specification. Note that the supplied specification
     * can overwrite data in the current specification. The following settings are overwritten:
     * <ul>
     *     <li>Content type</li>
     *     <li>Root path</
     *     <li>Status code</li>
     *     <li>Status line</li>
     * </ul>
     * The following settings are merged:
     * <ul>
     *     <li>Response body expectations</li>
     *     <li>Cookies</li>
     *     <li>Headers</li>
     * </ul>
     * @param specification The specification the add.
     * @return The builder
     */
    public ResponseSpecBuilder addResponseSpecification(ResponseSpecification specification) {
        if(!(specification instanceof ResponseSpecificationImpl)) {
            throw new IllegalArgumentException("specification must be of type "+ResponseSpecificationImpl.class.getClass()+".");
        }

        ResponseSpecificationImpl rs = (ResponseSpecificationImpl) specification;
        SpecificationMerger.merge((ResponseSpecificationImpl) spec, rs);
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
}