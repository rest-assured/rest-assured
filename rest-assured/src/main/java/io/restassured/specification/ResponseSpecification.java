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

package io.restassured.specification;

import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.matcher.DetailedCookieMatcher;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.hamcrest.Matcher;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Allows you to specify how the expected response must look like in order for a test to pass.
 */
public interface ResponseSpecification {

    /**
     * Validates the specified response against this ResponseSpecification
     *
     * @param response The response to validate
     * @return The same response
     */
    Response validate(Response response);

    /**
     * Validate that the response time (in milliseconds) matches the supplied <code>matcher</code>. For example:
     * <p/>
     * <pre>
     * when().
     *        get("/something").
     * then().
     *        time(lessThan(2000L));
     * </pre>
     * <p/>
     * where <code>lessThan</code> is a Hamcrest matcher
     *
     * @return The {@link ValidatableResponse} instance.
     */
    ResponseSpecification time(Matcher<Long> matcher);

    /**
     * Validate that the response time matches the supplied <code>matcher</code> and time unit. For example:
     * <p/>
     * <pre>
     * when().
     *        get("/something").
     * then().
     *        time(lessThan(2L), TimeUnit.SECONDS);
     * </pre>
     * <p/>
     * where <code>lessThan</code> is a Hamcrest matcher
     *
     * @return The {@link ValidatableResponse} instance.
     */
    ResponseSpecification time(Matcher<Long> matcher, TimeUnit timeUnit);

    /**
     * Same as {@link #body(String, org.hamcrest.Matcher, Object...)} expect that you can pass arguments to the key. This
     * is useful in situations where you have e.g. pre-defined variables that constitutes the key:
     * <pre>
     * String someSubPath = "else";
     * int index = 1;
     * when().get().then().body("something.%s[%d]", withArgs(someSubPath, index), equalTo("some value")). ..
     * </pre>
     * <p/>
     * or if you have complex root paths and don't wish to duplicate the path for small variations:
     * <pre>
     * when().
     *          get()
     * then().
     *          rootPath("filters.filterConfig[%d].filterConfigGroups.find { it.name == 'Gold' }.includes").
     *          body(withArgs(0), hasItem("first")).
     *          body(withArgs(1), hasItem("second")).
     *          ..
     * </pre>
     * <p/>
     * The key and arguments follows the standard <a href="https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html#syntax">formatting syntax</a> of Java.
     * <p>
     * Note that <code>withArgs</code> can be statically imported from the <code>io.restassured.RestAssured</code> class.
     * </p>
     *
     * @param key                       The body key
     * @param arguments                 The arguments to apply to the key
     * @param matcher                   The hamcrest matcher that must response body must match.
     * @param additionalKeyMatcherPairs Optionally additional hamcrest matchers that must return <code>true</code>.
     * @return the response specification
     * @see #body(String, org.hamcrest.Matcher, Object...)
     */
    ResponseSpecification body(String key, List<Argument> arguments, Matcher matcher, Object... additionalKeyMatcherPairs);

    /**
     * This as special kind of expectation that is mainly useful when you've specified a root path with an argument placeholder.
     * For example:
     * <pre>
     * when().
     *          get(..).
     * then().
     *          rootPath("x.%s"). // Root path with a placeholder
     *          body(withArgs("firstName"), equalTo(..)).
     *          body(withArgs("lastName"), equalTo(..)).
     * </pre>
     * <p/>
     * Note that this is the same as doing:
     * <pre>
     * when().
     *          get(..);
     * then().
     *          rootPath("x.%s"). // Root path with a placeholder
     *          body(withArgs("firstName"), equalTo(..)).
     *          body(withArgs("lastName"), equalTo(..)).
     * </pre>
     * <p/>
     *
     * @param arguments                 The arguments to apply to the root path.
     * @param matcher                   The hamcrest matcher that must response body must match.
     * @param additionalKeyMatcherPairs Optionally additional hamcrest matchers that must return <code>true</code>.
     * @return the response specification
     * @see #body(String, org.hamcrest.Matcher, Object...)
     */
    ResponseSpecification body(List<Argument> arguments, Matcher matcher, Object... additionalKeyMatcherPairs);

    /**
     * Expect that the response status code matches the given Hamcrest matcher. E.g.
     * <pre>
     * expect().statusCode(equalTo(200)).when().get("/something");
     * </pre>
     *
     * @param expectedStatusCode The expected status code matcher.
     * @return the response specification
     */
    ResponseSpecification statusCode(Matcher<? super Integer> expectedStatusCode);

    /**
     * Expect that the response status code matches an integer. E.g.
     * <pre>
     * expect().statusCode(200).when().get("/something");
     * </pre>
     * <p/>
     * This is the same as:
     * <pre>
     * expect().statusCode(equalTo(200)).when().get("/something");
     * </pre>
     *
     * @param expectedStatusCode The expected status code.
     * @return the response specification
     */
    ResponseSpecification statusCode(int expectedStatusCode);

    /**
     * Expect that the response status line matches the given Hamcrest matcher. E.g.
     * <pre>
     * expect().statusLine(equalTo("No Content")).when().get("/something");
     * </pre>
     *
     * @param expectedStatusLine The expected status line matcher.
     * @return the response specification
     */
    ResponseSpecification statusLine(Matcher<? super String> expectedStatusLine);

    /**
     * Expect that the response status line matches the given String. E.g.
     * <pre>
     * expect().statusLine("No Content").when().get("/something");
     * </pre>
     * <p/>
     * This is the same as:
     * <pre>
     * expect().statusLine(equalTo("No Content")).when().get("/something");
     * </pre>
     *
     * @param expectedStatusLine The expected status line.
     * @return the response specification
     */
    ResponseSpecification statusLine(String expectedStatusLine);

    /**
     * Expect that response headers matches those specified in a Map.
     * <p>
     * E.g. expect that the response of the GET request to "/something" contains header <tt>headerName1=headerValue1</tt>
     * and <tt>headerName2=headerValue2</tt>:
     * <pre>
     * Map expectedHeaders = new HashMap();
     * expectedHeaders.put("headerName1", "headerValue1"));
     * expectedHeaders.put("headerName2", "headerValue2");
     *
     * expect().response().headers(expectedHeaders).when().get("/something");
     * </pre>
     * </p>
     * <p/>
     * <p>
     * You can also use Hamcrest matchers:
     * <pre>
     * Map expectedHeaders = new HashMap();
     * expectedHeaders.put("Content-Type", containsString("charset=UTF-8"));
     * expectedHeaders.put("Content-Length", "160");
     *
     * expect().headers(expectedHeaders).when().get("/something");
     * </pre>
     * </p>
     *
     * @param expectedHeaders The Map of expected response headers
     * @return the response specification
     */
    ResponseSpecification headers(Map<String, ?> expectedHeaders);

    /**
     * Expect that response headers matches the supplied headers and values.
     * <p>
     * E.g. expect that the response of the GET request to "/something" contains header <tt>Pragma=no-cache</tt>
     * and <tt>Content-Encoding=gzip</tt>:
     * <pre>
     * expect().headers("Pragma", "no-cache", "Content-Encoding", "gzip").when().get("/something");
     * </pre>
     * </p>
     * <p/>
     * <p>
     * You can also use Hamcrest matchers:
     * <pre>
     * expect().response().headers("Content-Type", containsString("application/json"), "Pragma", equalsTo("no-cache")).when().get("/something");
     * </pre>
     * <p/>
     * and you can even mix string matching and hamcrest matching:
     * <pre>
     * expect().headers("Content-Type", containsString("application/json"), "Pragma", "no-cache").when().get("/something");
     * </pre>
     * </p>
     *
     * @param firstExpectedHeaderName  The name of the first header
     * @param firstExpectedHeaderValue The value of the first header
     * @param expectedHeaders          A list of expected "header name" - "header value" pairs.
     * @return the response specification
     */
    ResponseSpecification headers(String firstExpectedHeaderName, Object firstExpectedHeaderValue, Object... expectedHeaders);

    /**
     * Expect that a response header matches the supplied header name and hamcrest matcher.
     * <p>
     * E.g. expect that the response of the GET request to "/something" contains header <tt>Pragma=no-cache</tt>:
     * <pre>
     * expect().header("Pragma", containsString("no")).when().get("/something");
     * </pre>
     * </p>
     * <p/>
     * <p>
     * You can also expect several headers:
     * <pre>
     * expect().header("Pragma", equalsTo("no-cache")),and().header("Content-Encoding", containsString("zip")).when().get("/something");
     * </pre>
     * Also take a look at {@link #headers(String, Object, Object...)} )} for a short version of passing multiple headers.
     * </p>
     *
     * @param headerName           The name of the expected header
     * @param expectedValueMatcher The Hamcrest matcher that must conform to the value
     * @return the response specification
     */
    ResponseSpecification header(String headerName, Matcher<?> expectedValueMatcher);

    /**
     * Expect that a response header matches the supplied header name and hamcrest matcher using a mapping function.
     * <p>
     * E.g. expect that the response of the GET request to "/something" contains header <tt>Content-Length: 500</tt> and you want to
     * validate that the length must always be less than 600:
     * <pre>
     * when().
     *        get("/something").
     * then().
     *        header("Content-Length", Integer::parseInt, lessThan(600));
     * </pre>
     * </p>
     *
     * @param headerName           The name of the expected header
     * @param mappingFunction      Map the header to another value type before exposing it to the Hamcrest matcher
     * @param expectedValueMatcher The Hamcrest matcher that must conform to the value
     * @return the response specification
     */
    <T> ResponseSpecification header(String headerName, Function<String, T> mappingFunction, Matcher<? super T> expectedValueMatcher);

    /**
     * Expect that a response header matches the supplied name and value.
     * <p>
     * E.g. expect that the response of the GET request to "/something" contains header <tt>Pragma=no-cache</tt>:
     * <pre>
     * expect().header("Pragma", "no-cache").when().get("/something");
     * </pre>
     * </p>
     * <p/>
     * <p>
     * You can also expect several headers:
     * <pre>
     * expect().header("Pragma", "no-cache"),and().header("Content-Encoding", "gzip").when().get("/something");
     * </pre>
     * Also take a look at {@link #headers(String, Object, Object...)} for a short version of passing multiple headers.
     * </p>
     *
     * @param headerName    The name of the expected header
     * @param expectedValue The value of the expected header
     * @return the response specification
     */
    ResponseSpecification header(String headerName, String expectedValue);

    /**
     * Expect that response cookies matches those specified in a Map.
     * <p>
     * E.g. expect that the response of the GET request to "/something" contains cookies <tt>cookieName1=cookieValue1</tt>
     * and <tt>cookieName2=cookieValue2</tt>:
     * <pre>
     * Map expectedCookies = new HashMap();
     * expectedCookies.put("cookieName1", "cookieValue1"));
     * expectedCookies.put("cookieName2", "cookieValue2");
     *
     * expect().response().cookies(expectedCookies).when().get("/something");
     * </pre>
     * </p>
     * <p/>
     * <p>
     * You can also use Hamcrest matchers:
     * <pre>
     * Map expectedCookies = new HashMap();
     * expectedCookies.put("cookieName1", containsString("Value1"));
     * expectedCookies.put("cookieName2", "cookieValue2");
     *
     * expect().cookies(expectedCookies).when().get("/something");
     * </pre>
     * </p>
     *
     * @param expectedCookies A Map of expected response cookies
     * @return the response specification
     */
    ResponseSpecification cookies(Map<String, ?> expectedCookies);

    /**
     * Expect that a cookie exist in the response, regardless of value (it may have no value at all).
     *
     * @param cookieName the cookie to validate that it exists
     * @return the response specification
     */
    ResponseSpecification cookie(String cookieName);

    /**
     * Expect that response cookies matches the supplied cookie names and values.
     * <p>
     * E.g. expect that the response of the GET request to "/something" contains cookies <tt>cookieName1=cookieValue1</tt>
     * and <tt>cookieName2=cookieValue2</tt>:
     * <pre>
     * expect().cookies("cookieName1", "cookieValue1", "cookieName2", "cookieValue2").when().get("/something");
     * </pre>
     * </p>
     * <p/>
     * <p>
     * You can also use Hamcrest matchers:
     * <pre>
     * expect().response().cookies("cookieName1", containsString("Value1"), "cookieName2", equalsTo("cookieValue2")).when().get("/something");
     * </pre>
     * <p/>
     * and you can even mix string matching and hamcrest matching:
     * <pre>
     * expect().cookies("cookieName1", containsString("Value1"), "cookieName2", "cookieValue2").when().get("/something");
     * </pre>
     * </p>
     *
     * @param firstExpectedCookieName      The name of the first cookie
     * @param firstExpectedCookieValue     The value of the first cookie
     * @param expectedCookieNameValuePairs A list of expected "cookie name" - "cookie value" pairs.
     * @return the response specification
     */
    ResponseSpecification cookies(String firstExpectedCookieName, Object firstExpectedCookieValue, Object... expectedCookieNameValuePairs);

    /**
     * Expect that a response cookie matches the supplied cookie name and hamcrest matcher.
     * <p>
     * E.g. expect that the response of the GET request to "/something" contain cookie <tt>cookieName1=cookieValue1</tt>
     * <pre>
     * expect().cookie("cookieName1", containsString("Value1")).when().get("/something");
     * </pre>
     * </p>
     * <p/>
     * <p>
     * You can also expect several cookies:
     * <pre>
     * expect().cookie("cookieName1", equalsTo("cookieValue1")).and().cookie("cookieName2", containsString("Value2")).when().get("/something");
     * </pre>
     * Also take a look at {@link #cookies(String, Object, Object...)} for a short version of passing multiple cookies.
     * </p>
     *
     * @param cookieName           The name of the expected cookie
     * @param expectedValueMatcher The Hamcrest matcher that must conform to the value
     * @return the response specification
     */
    ResponseSpecification cookie(String cookieName, Matcher<?> expectedValueMatcher);

    /**
     * Validate that a detailed response cookie matches the supplied cookie name and hamcrest matcher (see {@link DetailedCookieMatcher}.
     * <p>
     * E.g. expect that the response of the GET request to "/something" contain cookie <tt>cookieName1=cookieValue1</tt>
     * <pre>
     * expect.cookie("cookieName1", detailedCookie().value("cookieValue1").secured(true));
     * </pre>
     * </p>
     * <p/>
     * <p>
     * You can also expect several cookies:
     * <pre>
     * expect().cookie("cookieName1", detailedCookie().value("cookieValue1").secured(true))
     *      .and().cookie("cookieName2", detailedCookie().value("cookieValue2").secured(false));
     * </pre>
     * </p>
     *
     * @param cookieName            The name of the expected cookie
     * @param detailedCookieMatcher The Hamcrest matcher that must conform to the cookie
     * @return the response specification
     */
    ResponseSpecification cookie(String cookieName, DetailedCookieMatcher detailedCookieMatcher);

    /**
     * Expect that a response cookie matches the supplied name and value.
     * <p>
     * E.g. expect that the response of the GET request to "/something" contain cookie <tt>cookieName1=cookieValue1</tt>:
     * <pre>
     * expect().cookie("cookieName1", "cookieValue1").when().get("/something");
     * </pre>
     * </p>
     * <p/>
     * <p>
     * You can also expect several cookies:
     * <pre>
     * expect().cookie("cookieName1", "cookieValue1"),and().cookie("cookieName2", "cookieValue2").when().get("/something");
     * </pre>
     * Also take a look at {@link #cookies(String, Object, Object...)} for a short version of passing multiple cookies.
     * </p>
     *
     * @param cookieName    The name of the expected cookie
     * @param expectedValue The value of the expected cookie
     * @return the response specification
     */
    ResponseSpecification cookie(String cookieName, Object expectedValue);

    /**
     * Returns the {@link ResponseLogSpecification} that allows you to log different parts of the {@link ResponseSpecification}.
     * This is mainly useful for debug purposes when writing your tests. It's a shortcut for:
     * <pre>
     * given().filter(ResponseLoggingFilter.responseLogger()). ..
     * </pre>
     *
     * @return the response log specification
     */
    ResponseLogSpecification log();

    /**
     * Set the root path with arguments of the response body so that you don't need to write the entire path for each expectation.
     *
     * @param rootPath  The root path to use.
     * @param arguments The list of substitution arguments. The path and arguments follows the standard <a href="https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html#syntax">formatting syntax</a> of Java..
     */
    ResponseSpecification rootPath(String rootPath, List<Argument> arguments);

    /**
     * @param rootPath  The root path to use.
     * @param arguments The list of substitution arguments. The path and arguments follows the standard <a href="https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html#syntax">formatting syntax</a> of Java..
     *
     * @see #rootPath(String, List)
     * @deprecated Use {@link #rootPath(String, List)} (String)} instead
     */
    @Deprecated
    default ResponseSpecification root(String rootPath, List<Argument> arguments) {
        return rootPath(rootPath);
    }

    /**
     * Set the root path of the response body so that you don't need to write the entire path for each expectation.
     * E.g. instead of writing:
     * <p/>
     * <pre>
     * when().
     *         get(..);
     * then().
     *         body("x.y.firstName", is(..)).
     *         body("x.y.lastName", is(..)).
     *         body("x.y.age", is(..)).
     *         body("x.y.gender", is(..)).
     * </pre>
     * <p/>
     * you can use a root and do:
     * <pre>
     * when().
     *         get(..).
     * then().
     *        rootPath("x.y").
     *        body("firstName", is(..)).
     *        body("lastName", is(..)).
     *        body("age", is(..)).
     *        body("gender", is(..)).
     * </pre>
     * <p/>
     *
     * @param rootPath The root path to use.
     */
    ResponseSpecification rootPath(String rootPath);

    /**
     * @param rootPath The root path.
     * @see #rootPath(String)
     * @deprecated Use {@link #rootPath(String)} (String)} instead
     */
    @Deprecated
    default ResponseSpecification root(String rootPath) {
        return rootPath(rootPath);
    }

    /**
     * Reset the root path of the response body so that you don't need to write the entire path for each expectation.
     * For example:
     * <p/>
     * <pre>
     * when().
     *          get(..);
     * then().
     *          rootPath("x.y").
     *          body("firstName", is(..)).
     *          body("lastName", is(..)).
     *          noRootPath()
     *          body("z.something1", is(..)).
     *          body("w.something2", is(..)).
     * </pre>
     * <p/>
     * This is the same as calling <code>rootPath("")</code> but less verbose and the it communicates intent better.
     *
     * @see #rootPath(String)
     */
    ResponseSpecification noRootPath();

    /**
     * @deprecated Use {@link #noRootPath()} instead
     */
    @Deprecated
    default ResponseSpecification noRoot() {
        return noRootPath();
    }

    /**
     * Append the given path to the root path of the response body so that you don't need to write the entire path for each expectation.
     * E.g. instead of writing:
     * <p/>
     * <pre>
     * expect().
     *          rootPath("x.y").
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
     *          rootPath("x.y").
     *          body("age", is(..)).
     *          body("gender", is(..)).
     *          appendRootPath("name").
     *          body("firstName", is(..)).
     *          body("lastName", is(..)).
     * when().
     *          get(..);
     * </pre>
     *
     * @param pathToAppend The root path to append.
     */
    ResponseSpecification appendRootPath(String pathToAppend);

    /**
     * Append the given path to the root path with arguments supplied of the response body so that you don't need to write the entire path for each expectation.
     * This is mainly useful when you have parts of the path defined in variables.
     * E.g. instead of writing:
     * <p/>
     * <pre>
     * String namePath = "name";
     * expect().
     *          rootPath("x.y").
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
     *          rootPath("x.y").
     *          body("age", is(..)).
     *          body("gender", is(..)).
     *          appendRootPath("%s", withArgs(namePath)).
     *          body("first", is(..)).
     *          body("last", is(..)).
     * when().
     *          get(..);
     * </pre>
     *
     * @param pathToAppend The root path to use. The path and arguments follows the standard <a href="https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html#syntax">formatting syntax</a> of Java.
     */
    ResponseSpecification appendRootPath(String pathToAppend, List<Argument> arguments);

    /**
     * @param pathToAppend The root path to append.
     * @see #appendRootPath(String, List)
     * @deprecated Use {@link #appendRootPath(String, List)} instead
     */
    default ResponseSpecification appendRoot(String pathToAppend, List<Argument> arguments) {
        return appendRootPath(pathToAppend, arguments);
    }

    /**
     * @param pathToAppend The root path to append.
     * @see #appendRootPath(String)
     * @deprecated Use {@link #appendRootPath(String)} instead
     */
    @Deprecated
    default ResponseSpecification appendRoot(String pathToAppend) {
        return detachRootPath(pathToAppend);
    }

    /**
     * Detach the given path from the root path.
     * E.g. instead of writing:
     * <p/>
     * <pre>
     * when().
     *          get(..);
     * then().
     *          rootPath("x.y").
     *          body("age", is(..)).
     *          body("gender", is(..)).
     *          rootPath("x").
     *          body("firstName", is(..)).
     *          body("lastName", is(..)).
     * </pre>
     * <p/>
     * you can use a append root and do:
     * <pre>
     * when().
     *          get(..);
     * then().
     *          rootPath("x.y").
     *          body("age", is(..)).
     *          body("gender", is(..)).
     *          detachRootPath("y").
     *          body("firstName", is(..)).
     *          body("lastName", is(..)).
     * </pre>
     *
     * @param pathToDetach The root path to detach.
     */
    ResponseSpecification detachRootPath(String pathToDetach);

    /**
     *
     * @param pathToDetach The root path to detach.
     * @see #detachRootPath(String)
     * @deprecated Use {@link #detachRootPath(String)} instead
     */
    @Deprecated
    default ResponseSpecification detachRoot(String pathToDetach) {
        return detachRootPath(pathToDetach);
    }

    /**
     * Set the response content type to be <code>contentType</code>.
     *
     * @param contentType The content type of the response.
     * @return the response specification
     */
    ResponseSpecification contentType(ContentType contentType);

    /**
     * Set the response content type to be <code>contentType</code>.
     *
     * @param contentType The content type of the response.
     * @return the response specification
     */
    ResponseSpecification contentType(String contentType);

    /**
     * Expect the response content type to be <code>contentType</code>.
     *
     * @param contentType The expected content type of the response.
     * @return the response specification
     */
    ResponseSpecification contentType(Matcher<? super String> contentType);

    /**
     * Expect that the response body conforms to one or more Hamcrest matchers. E.g.
     * <pre>
     * // Expect that the response body (content) contains the string "winning-numbers"
     * when().get("/lotto").then().body(containsString("winning-numbers"));
     *
     * // Expect that the response body (content) contains the string "winning-numbers" and "winners"
     * when().get("/lotto").then().body(containsString("winning-numbers"), containsString("winners"));
     * </pre>
     *
     * @param matcher            The hamcrest matcher that must response body must match.
     * @param additionalMatchers Optionally additional hamcrest matchers that must return <code>true</code>.
     * @return the response specification
     */
    ResponseSpecification body(Matcher<?> matcher, Matcher<?>... additionalMatchers);

    /**
     * Expect that the JSON or XML response body conforms to one or more Hamcrest matchers.<br>
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
     * <p/>
     * You can verify that the lottoId is equal to 5 like this:
     * <pre>
     * when().get("/lotto").then().body("lotto.lottoId", equalTo(5));
     * </pre>
     * <p/>
     * You can also verify that e.g. one of the the winning numbers is 45.
     * <pre>
     * when().get("/lotto").then().body("lotto.winning-numbers", hasItem(45));
     * </pre>
     * <p/>
     * Or both at the same time:
     * <pre>
     * when().get("/lotto").then().body("lotto.lottoId", equalTo(5)).and().body("lotto.winning-numbers", hasItem(45));
     * </pre>
     * <p/>
     * or a slightly short version:
     * <pre>
     * when().get("/lotto").then().body("lotto.lottoId", equalTo(5), "lotto.winning-numbers", hasItem(45));
     * </pre>
     * </p>
     * <h3>XML example</h3>
     * <p>
     * Assume that a GET request to "/xml" returns a XML response containing:
     * <pre>
     * &lt;greeting&gt;
     *    &lt;firstName&gt;John&lt;/firstName&gt;
     *    &lt;lastName&gt;Doe&lt;/lastName&gt;
     * &lt;/greeting&gt;
     * </pre>
     * </p>
     * <p/>
     * You can now verify that the firstName is equal to "John" like this:
     * <pre>
     * when().get("/xml").then().body("greeting.firstName", equalTo("John"));
     * </pre>
     * <p/>
     * To verify both the first name and last name you can do like this:
     * <pre>
     * when().get("/xml").then().body("greeting.firstName", equalTo("John")).and().body("greeting.lastName", equalTo("Doe"));
     * </pre>
     * <p/>
     * Or the slightly shorter version of:
     * <pre>
     * when().get("/xml").then().body("greeting.firstName", equalTo("John"), "greeting.lastName", equalTo("Doe"));
     * </pre>
     * <h3>Notes</h3>
     * <p>
     * Note that if the response body type is not of type <tt>application/xml</tt> or <tt>application/json</tt> you
     * <i>cannot</i> use this verification.
     * </p>
     * <p/>
     * <p>
     * The only difference between the <code>content</code> and <code>body</code> methods are of syntactic nature.
     * </p>
     *
     * @param path                      The body path
     * @param matcher                   The hamcrest matcher that must response body must match.
     * @param additionalKeyMatcherPairs Optionally additional hamcrest matchers that must return <code>true</code>.
     * @return the response specification
     */
    ResponseSpecification body(String path, Matcher<?> matcher, Object... additionalKeyMatcherPairs);

    /**
     * Syntactic sugar, e.g.
     * <pre>
     * expect().body(containsString("OK")).when().get("/something");
     * </pre>
     * <p/>
     * is that same as:
     * <pre>
     * expect().body(containsString("OK")).get("/something");
     * </pre>
     *
     * @return the response specification
     */
    RequestSender when();

    /**
     * Returns the request io.restassured.specification so that you can define the properties of the request.
     * <pre>
     * expect().body(containsString("OK")).given().parameters("param1", "value1").when().get("/something");
     * </pre>
     *
     * @return the request io.restassured.specification
     */
    RequestSpecification given();

    /**
     * Syntactic sugar, e.g.
     * <pre>
     * expect().that().body(containsString("OK")).when().get("/something");
     * </pre>
     * <p/>
     * is that same as:
     * <pre>
     * expect().body(containsString("OK")).get("/something");
     * </pre>
     *
     * @return the response specification
     */
    ResponseSpecification that();

    /**
     * Returns the request io.restassured.specification so that you can define the properties of the request.
     * <pre>
     * expect().body(containsString("OK")).and().request().parameters("param1", "value1").when().get("/something");
     * </pre>
     *
     * @return the request io.restassured.specification
     */
    RequestSpecification request();

    /**
     * Syntactic sugar, e.g.
     * <pre>
     * expect().response().body(containsString("OK")).when().get("/something");
     * </pre>
     * <p/>
     * is that same as:
     * <pre>
     * expect().body(containsString("OK")).get("/something");
     * </pre>
     *
     * @return the response specification
     */
    ResponseSpecification response();

    /**
     * Syntactic sugar, e.g.
     * <pre>
     * expect().body(containsString("OK")).and().body(containsString("something else")).when().get("/something");
     * </pre>
     * <p/>
     * is that same as:
     * <pre>
     * expect().body(containsString("OK")).body(containsString("something else")).when().get("/something");
     * </pre>
     *
     * @return the response specification
     */
    ResponseSpecification and();

    /**
     * Returns the request io.restassured.specification so that you can define the properties of the request.
     * <pre>
     * expect().body(containsString("OK")).and().with().request().parameters("param1", "value1").get("/something");
     * </pre>
     *
     * @return the request io.restassured.specification
     */
    RequestSpecification with();

    /**
     * Syntactic sugar, e.g.
     * <pre>
     * expect().body(containsString("OK")).then().get("/something");
     * </pre>
     * <p/>
     * is that same as:
     * <pre>
     * expect().body(containsString("OK")).get("/something");
     * </pre>
     *
     * @return the response specification
     */
    ResponseSpecification then();

    /**
     * Syntactic sugar, e.g.
     * <pre>
     * expect().body(containsString("OK")).and().expect().body(containsString("something else")).when().get("/something");
     * </pre>
     * <p/>
     * is that same as:
     * <pre>
     * * expect().body(containsString("OK")).and().body(containsString("something else")).when().get("/something");
     * </pre>
     *
     * @return the response specification
     */
    ResponseSpecification expect();

    /**
     * Expect that the response matches an entire specification.
     * <pre>
     * ResponseSpecification responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
     *
     * when().
     *        get("/something").
     * then().
     *         spec(responseSpec).
     *         body("x.y.z", equalTo("something"));
     * </pre>
     * <p/>
     * This is useful when you want to reuse multiple different expectations in several tests.
     * <p/>
     * The specification passed to this method is merged with the current specification. Note that the supplied specification
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
     * <p/>
     *
     * @param responseSpecificationToMerge The specification to merge with.
     * @return the response specification
     */
    ResponseSpecification spec(ResponseSpecification responseSpecificationToMerge);

    /**
     * Register a content-type to be parsed using a predefined parser. E.g. let's say you want parse
     * content-type <tt>application/vnd.uoml+xml</tt> with the XML parser to be able to verify the response using the XML dot notations:
     * <pre>
     * expect().body("document.child", equalsTo("something"))..
     * </pre>
     * Since <tt>application/vnd.uoml+xml</tt> is not registered to be processed by the XML parser by default you need to explicitly
     * tell REST Assured to use this parser before making the request:
     * <pre>
     * expect().parser("application/vnd.uoml+xml", Parser.XML).when(). ..;
     * </pre>
     * <p/>
     * You can also specify by it for every response by using:
     * <pre>
     * RestAssured.registerParser("application/vnd.uoml+xml", Parser.XML);
     * </pre>
     *
     * @param contentType The content-type to register
     * @param parser      The parser to use when verifying the response.
     */
    ResponseSpecification parser(String contentType, Parser parser);

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
    ResponseSpecification defaultParser(Parser parser);

    /**
     * Log different parts of the {@link ResponseSpecification} by using {@link LogDetail}.
     * This is mainly useful for debug purposes when writing your tests. It's a shortcut for:
     * <pre>
     * given().filter(new ResponseLoggingFilter(logDetail))). ..
     * </pre>
     *
     * @param logDetail The log detail
     */
    ResponseSpecification logDetail(LogDetail logDetail);
}