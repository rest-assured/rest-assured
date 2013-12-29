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

package com.jayway.restassured.response;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.parsing.Parser;
import com.jayway.restassured.specification.Argument;
import com.jayway.restassured.specification.ResponseSpecification;
import org.hamcrest.Matcher;

import java.util.List;
import java.util.Map;

/**
 * A validatable response of a request made by REST Assured.
 * <p>
 * Usage example:
 * <pre>
 * get("/lotto").then().body("lotto.lottoId", is(5));
 * </pre>
 * </p>
 */
public interface ValidatableResponseOptions<T extends ValidatableResponseOptions<T, R>, R extends ResponseOptions<R>> {

    /**
     * Validate that the response content conforms to one or more Hamcrest matchers. E.g.
     * <pre>
     * // Validate that the response content (body) contains the string "winning-numbers"
     * get("/lotto").then().content(containsString("winning-numbers"));
     *
     * // Validate that the response content (body) contains the string "winning-numbers" and "winners"
     * get("/lotto").then().assertThat().content(containsString("winning-numbers"), containsString("winners"));
     * </pre>
     *
     * @param matcher            The hamcrest matcher that must response content must match.
     * @param additionalMatchers Optionally additional hamcrest matchers that must return <code>true</code>.
     * @return the response specification
     */
    T content(Matcher<?> matcher, Matcher<?>... additionalMatchers);

    /**
     * This as special kind of validation that is mainly useful when you've specified a root path with an argument placeholder.
     * For example:
     * <pre>
     * get(..).then().assertThat().
     *          root("x.%s"). // Root path with a placeholder
     *          content(withArgs("firstName"), equalTo(..)).
     *          content(withArgs("lastName"), equalTo(..)).
     * </pre>
     * <p/>
     * Note that this is the same as doing:
     * <pre>
     * get(..).then().assertThat().
     *          root("x.%s"). // Root path with a placeholder
     *          body(withArgs("firstName"), equalTo(..)).
     *          body(withArgs("lastName"), equalTo(..)).
     * </pre>
     * <p/>
     * <p>
     * Note that this method is the same as {@link #body(java.util.List, org.hamcrest.Matcher, Object...)} but with a method name.
     * </p>
     *
     * @param arguments                 The arguments to apply to the root path.
     * @param matcher                   The hamcrest matcher that must response body must match.
     * @param additionalKeyMatcherPairs Optionally additional hamcrest matchers that must return <code>true</code>.
     * @return the response specification
     * @see #body(String, org.hamcrest.Matcher, Object...)
     */
    T content(List<Argument> arguments, Matcher matcher, Object... additionalKeyMatcherPairs);

    /**
     * Validate that the JSON or XML response content conforms to one or more Hamcrest matchers.<br>
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
     * get("/lotto").then().assertThat().content("lotto.lottoId", equalTo(5));
     * </pre>
     * <p/>
     * You can also verify that e.g. one of the the winning numbers is 45.
     * <pre>
     * get("/lotto").then().content("lotto.winning-numbers", hasItem(45));
     * </pre>
     * <p/>
     * Or both at the same time:
     * <pre>
     * get("/lotto").then().content("lotto.lottoId", equalTo(5)).and().content("lotto.winning-numbers", hasItem(45));
     * </pre>
     * <p/>
     * or a slightly short version:
     * <pre>
     * get("/lotto").then().content("lotto.lottoId", equalTo(5), "lotto.winning-numbers", hasItem(45));
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
     * get("/xml").then().content("greeting.firstName", equalTo("John"));
     * </pre>
     * <p/>
     * To verify both the first name and last name you can do like this:
     * <pre>
     * get("/xml").then().content("greeting.firstName", equalTo("John")).and().content("greeting.lastName", equalTo("Doe"));
     * </pre>
     * <p/>
     * Or the slightly shorter version of:
     * <pre>
     * get("/xml").then().content("greeting.firstName", equalTo("John"), "greeting.lastName", equalTo("Doe"));
     * </pre>
     * <h3>Notes</h3>
     * <p>
     * Note that if the response content type is not of type <tt>application/xml</tt> or <tt>application/json</tt> you
     * <i>cannot</i> use this verification.
     * </p>
     * <p/>
     * <p>
     * The only difference between the <code>content</code> and <code>body</code> methods are of syntactic nature.
     * </p>
     *
     * @param matcher                   The hamcrest matcher that must response content must match.
     * @param additionalKeyMatcherPairs Optionally additional hamcrest matchers that must return <code>true</code>.
     * @return the response specification
     */
    T content(String key, Matcher<?> matcher, Object... additionalKeyMatcherPairs);

    /**
     * Same as {@link #body(String, org.hamcrest.Matcher, Object...)} expect that you can pass arguments to the key. This
     * is useful in situations where you have e.g. pre-defined variables that constitutes the key:
     * <pre>
     * String someSubPath = "else";
     * int index = 1;
     * get("/x").then().body("something.%s[%d]", withArgs(someSubPath, index), equalTo("some value")). ..
     * </pre>
     * <p/>
     * or if you have complex root paths and don't wish to duplicate the path for small variations:
     * <pre>
     * get("/x").then().
     *          root("filters.filterConfig[%d].filterConfigGroups.find { it.name == 'Gold' }.includes").
     *          body(withArgs(0), hasItem("first")).
     *          body(withArgs(1), hasItem("second")).
     *          ..
     * </pre>
     * <p/>
     * The key and arguments follows the standard <a href="http://download.oracle.com/javase/1,5.0/docs/api/java/util/Formatter.html#syntax">formatting syntax</a> of Java.
     * <p>
     * Note that <code>withArgs</code> can be statically imported from the <code>com.jayway.restassured.RestAssured</code> class.
     * </p>
     *
     * @param key                       The body key
     * @param arguments                 The arguments to apply to the key
     * @param matcher                   The hamcrest matcher that must response body must match.
     * @param additionalKeyMatcherPairs Optionally additional hamcrest matchers that must return <code>true</code>.
     * @return the response specification
     * @see #body(String, org.hamcrest.Matcher, Object...)
     */
    T body(String key, List<Argument> arguments, Matcher matcher, Object... additionalKeyMatcherPairs);

    /**
     * This as special kind of expectation that is mainly useful when you've specified a root path with an argument placeholder.
     * For example:
     * <pre>
     *  get(..).then().
     *            root("x.%s"). // Root path with a placeholder
     *            body(withArgs("firstName"), equalTo(..)).
     *            body(withArgs("lastName"), equalTo(..)).
     * </pre>
     * <p/>
     * Note that this is the same as doing:
     * <pre>
     *  get(..).then().
     *            root("x.%s"). // Root path with a placeholder
     *            content(withArgs("firstName"), equalTo(..)).
     *            content(withArgs("lastName"), equalTo(..)).
     * </pre>
     * <p/>
     * <p>
     * Note that this method is the same as {@link #content(java.util.List, org.hamcrest.Matcher, Object...)} but with a method name.
     * </p>
     *
     * @param arguments                 The arguments to apply to the root path.
     * @param matcher                   The hamcrest matcher that must response body must match.
     * @param additionalKeyMatcherPairs Optionally additional hamcrest matchers that must return <code>true</code>.
     * @return the response specification
     * @see #body(String, org.hamcrest.Matcher, Object...)
     */
    T body(List<Argument> arguments, Matcher matcher, Object... additionalKeyMatcherPairs);

    /**
     * Validate that the response status code matches the given Hamcrest matcher. E.g.
     * <pre>
     * get("/something").then().assertThat().statusCode(equalTo(200));
     * </pre>
     *
     * @param expectedStatusCode The expected status code matcher.
     * @return the response specification
     */
    T statusCode(Matcher<? super Integer> expectedStatusCode);

    /**
     * Validate that the response status code matches an integer. E.g.
     * <pre>
     * get("/something").then().assertThat().statusCode(200);
     * </pre>
     * <p/>
     * This is the same as:
     * <pre>
     * get("/something").then().assertThat().statusCode(equalTo(200));
     * </pre>
     *
     * @param expectedStatusCode The expected status code.
     * @return the response specification
     */
    T statusCode(int expectedStatusCode);

    /**
     * Validate that the response status line matches the given Hamcrest matcher. E.g.
     * <pre>
     * expect().statusLine(equalTo("No Content")).when().get("/something");
     * </pre>
     *
     * @param expectedStatusLine The expected status line matcher.
     * @return the response specification
     */
    T statusLine(Matcher<? super String> expectedStatusLine);

    /**
     * Validate that the response status line matches the given String. E.g.
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
    T statusLine(String expectedStatusLine);

    /**
     * Validate that response headers matches those specified in a Map.
     * <p>
     * E.g. expect that the response of the GET request to "/something" contains header <tt>headerName1=headerValue1</tt>
     * and <tt>headerName2=headerValue2</tt>:
     * <pre>
     * Map expectedHeaders = new HashMap();
     * expectedHeaders.put("headerName1", "headerValue1"));
     * expectedHeaders.put("headerName2", "headerValue2");
     *
     * get("/something").then().assertThat().headers(expectedHeaders);
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
     * get("/something").then().assertThat().headers(expectedHeaders);
     * </pre>
     * </p>
     *
     * @param expectedHeaders The Map of expected response headers
     * @return the response specification
     */
    T headers(Map<String, ?> expectedHeaders);

    /**
     * Validate that response headers matches the supplied headers and values.
     * <p>
     * E.g. expect that the response of the GET request to "/something" contains header <tt>Pragma=no-cache</tt>
     * and <tt>Content-Encoding=gzip</tt>:
     * <pre>
     * get("/something").then().assertThat().headers("Pragma", "no-cache", "Content-Encoding", "gzip");
     * </pre>
     * </p>
     * <p/>
     * <p>
     * You can also use Hamcrest matchers:
     * <pre>
     * get("/something").then().assertThat().headers("Content-Type", containsString("application/json"), "Pragma", equalsTo("no-cache"));
     * </pre>
     * <p/>
     * and you can even mix string matching and hamcrest matching:
     * <pre>
     * get("/something").then().assertThat().headers("Content-Type", containsString("application/json"), "Pragma", "no-cache");
     * </pre>
     * </p>
     *
     * @param firstExpectedHeaderName  The name of the first header
     * @param firstExpectedHeaderValue The value of the first header
     * @param expectedHeaders          A list of expected "header name" - "header value" pairs.
     * @return the response specification
     */
    T headers(String firstExpectedHeaderName, Object firstExpectedHeaderValue, Object... expectedHeaders);

    /**
     * Validate that a response header matches the supplied header name and hamcrest matcher.
     * <p>
     * E.g. expect that the response of the GET request to "/something" contains header <tt>Pragma=no-cache</tt>:
     * <pre>
     * get("/something").then().assertThat().header("Pragma", containsString("no"));
     * </pre>
     * </p>
     * <p/>
     * <p>
     * You can also expect several headers:
     * <pre>
     * get("/something").then().assertThat().header("Pragma", equalsTo("no-cache")).and().header("Content-Encoding", containsString("zip"));
     * </pre>
     * Also take a look at {@link #headers(String, Object, Object...)} )} for a short version of passing multiple headers.
     * </p>
     *
     * @param headerName           The name of the expected header
     * @param expectedValueMatcher The Hamcrest matcher that must conform to the value
     * @return the response specification
     */
    T header(String headerName, Matcher<?> expectedValueMatcher);

    /**
     * Validate that a response header matches the supplied name and value.
     * <p>
     * E.g. expect that the response of the GET request to "/something" contains header <tt>Pragma=no-cache</tt>:
     * <pre>
     * get("/something").then().assertThat().header("Pragma", "no-cache");
     * </pre>
     * </p>
     * <p/>
     * <p>
     * You can also expect several headers:
     * <pre>
     * get("/something").then().assertThat().header("Pragma", "no-cache").and().header("Content-Encoding", "gzip");
     * </pre>
     * Also take a look at {@link #headers(String, Object, Object...)} for a short version of passing multiple headers.
     * </p>
     *
     * @param headerName    The name of the expected header
     * @param expectedValue The value of the expected header
     * @return the response specification
     */
    T header(String headerName, String expectedValue);

    /**
     * Validate that response cookies matches those specified in a Map.
     * <p>
     * E.g. expect that the response of the GET request to "/something" contains cookies <tt>cookieName1=cookieValue1</tt>
     * and <tt>cookieName2=cookieValue2</tt>:
     * <pre>
     * Map expectedCookies = new HashMap();
     * expectedCookies.put("cookieName1", "cookieValue1"));
     * expectedCookies.put("cookieName2", "cookieValue2");
     *
     * get("/something").then().assertThat().cookies(expectedCookies);
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
     * get("/something").then().assertThat().cookies(expectedCookies);
     * </pre>
     * </p>
     *
     * @param expectedCookies A Map of expected response cookies
     * @return the response specification
     */
    T cookies(Map<String, ?> expectedCookies);

    /**
     * Validate that a cookie exist in the response, regardless of value (it may have no value at all).
     *
     * @param cookieName the cookie to validate that it exists
     * @return the response specification
     */
    T cookie(String cookieName);

    /**
     * Validate that response cookies matches the supplied cookie names and values.
     * <p>
     * E.g. expect that the response of the GET request to "/something" contains cookies <tt>cookieName1=cookieValue1</tt>
     * and <tt>cookieName2=cookieValue2</tt>:
     * <pre>
     * get("/something").then().assertThat().cookies("cookieName1", "cookieValue1", "cookieName2", "cookieValue2");
     * </pre>
     * </p>
     * <p/>
     * <p>
     * You can also use Hamcrest matchers:
     * <pre>
     * get("/something").then().assertThat().cookies("cookieName1", containsString("Value1"), "cookieName2", equalsTo("cookieValue2"));
     * </pre>
     * <p/>
     * and you can even mix string matching and hamcrest matching:
     * <pre>
     * get("/something").then().assertThat().cookies("cookieName1", containsString("Value1"), "cookieName2", "cookieValue2");
     * </pre>
     * </p>
     *
     * @param firstExpectedCookieName      The name of the first cookie
     * @param firstExpectedCookieValue     The value of the first cookie
     * @param expectedCookieNameValuePairs A list of expected "cookie name" - "cookie value" pairs.
     * @return the response specification
     */
    T cookies(String firstExpectedCookieName, Object firstExpectedCookieValue, Object... expectedCookieNameValuePairs);

    /**
     * Validate that a response cookie matches the supplied cookie name and hamcrest matcher.
     * <p>
     * E.g. expect that the response of the GET request to "/something" contain cookie <tt>cookieName1=cookieValue1</tt>
     * <pre>
     * get("/something").then().assertThat().cookie("cookieName1", containsString("Value1"));
     * </pre>
     * </p>
     * <p/>
     * <p>
     * You can also expect several cookies:
     * <pre>
     * get("/something").then().assertThat().cookie("cookieName1", equalsTo("cookieValue1")).and().cookie("cookieName2", containsString("Value2"));
     * </pre>
     * Also take a look at {@link #cookies(String, Object, Object...)} for a short version of passing multiple cookies.
     * </p>
     *
     * @param cookieName           The name of the expected cookie
     * @param expectedValueMatcher The Hamcrest matcher that must conform to the value
     * @return the response specification
     */
    T cookie(String cookieName, Matcher<?> expectedValueMatcher);

    /**
     * Validate that a response cookie matches the supplied name and value.
     * <p>
     * E.g. expect that the response of the GET request to "/something" contain cookie <tt>cookieName1=cookieValue1</tt>:
     * <pre>
     * get("/something").then().assertThat().cookie("cookieName1", "cookieValue1");
     * </pre>
     * </p>
     * <p/>
     * <p>
     * You can also expect several cookies:
     * <pre>
     * get("/something").then().assertThat().cookie("cookieName1", "cookieValue1").and().cookie("cookieName2", "cookieValue2");
     * </pre>
     * Also take a look at {@link #cookies(String, Object, Object...)} for a short version of passing multiple cookies.
     * </p>
     *
     * @param cookieName    The name of the expected cookie
     * @param expectedValue The value of the expected cookie
     * @return the response specification
     */
    T cookie(String cookieName, Object expectedValue);

    /**
     * Set the root path of the response body so that you don't need to write the entire path for each expectation.
     * E.g. instead of writing:
     * <p/>
     * <pre>
     * get(..).then().
     *          body("x.y.firstName", is(..)).
     *          body("x.y.lastName", is(..)).
     *          body("x.y.age", is(..)).
     *          body("x.y.gender", is(..)).
     * </pre>
     * <p/>
     * you can use a root path and do:
     * <pre>
     * get(..).then().
     *            rootPath("x.y").
     *            body("firstName", is(..)).
     *            body("lastName", is(..)).
     *            body("age", is(..)).
     *            body("gender", is(..));
     * </pre>
     * <p/>
     * Note that this method is exactly the same as {@link #root(String)}.
     *
     * @param rootPath The root path to use.
     */
    T rootPath(String rootPath);

    /**
     * Set the root path with arguments of the response body so that you don't need to write the entire path for each expectation.
     * <p/>
     * Note that this method is exactly the same as {@link #root(String, java.util.List)}.
     *
     * @param rootPath  The root path to use.
     * @param arguments A list of arguments. The path and arguments follows the standard <a href="http://download.oracle.com/javase/1,5.0/docs/api/java/util/Formatter.html#syntax">formatting syntax</a> of Java.
     * @see #rootPath(String)
     */
    T rootPath(String rootPath, List<Argument> arguments);

    /**
     * Set the root path with arguments of the response body so that you don't need to write the entire path for each expectation.
     * <p/>
     * Note that this method is exactly the same as {@link #rootPath(String, java.util.List)}.
     *
     * @param rootPath  The root path to use.
     * @param arguments The list of substitution arguments. The path and arguments follows the standard <a href="http://download.oracle.com/javase/1,5.0/docs/api/java/util/Formatter.html#syntax">formatting syntax</a> of Java..
     * @see #rootPath(String)
     */
    T root(String rootPath, List<Argument> arguments);

    /**
     * Set the root path of the response body so that you don't need to write the entire path for each expectation.
     * E.g. instead of writing:
     * <p/>
     * <pre>
     * get(..).then().
     *          body("x.y.firstName", is(..)).
     *          body("x.y.lastName", is(..)).
     *          body("x.y.age", is(..)).
     *          body("x.y.gender", is(..));
     * </pre>
     * <p/>
     * you can use a root and do:
     * <pre>
     * get(..).then().
     *          root("x.y").
     *          body("firstName", is(..)).
     *          body("lastName", is(..)).
     *          body("age", is(..)).
     *          body("gender", is(..)).
     * </pre>
     * <p/>
     * Note that this method is exactly the same as {@link #rootPath(String)} but slightly shorter.
     *
     * @param rootPath The root path to use.
     */
    T root(String rootPath);

    /**
     * Reset the root path of the response body so that you don't need to write the entire path for each expectation.
     * For example:
     * <p/>
     * <pre>
     * get(..).then().
     *          root("x.y").
     *          body("firstName", is(..)).
     *          body("lastName", is(..)).
     *          noRoot()
     *          body("z.something1", is(..)).
     *          body("w.something2", is(..));
     * </pre>
     * <p/>
     * This is the same as calling <code>rootPath("")</code> but more expressive.
     * Note that this method is exactly the same as {@link #noRootPath()} but slightly shorter.
     *
     * @see #root(String)
     */
    T noRoot();

    /**
     * Reset the root path of the response body so that you don't need to write the entire path for each expectation.
     * For example:
     * <p/>
     * <pre>
     * get(..).then().
     *          rootPath("x.y").
     *          body("firstName", is(..)).
     *          body("lastName", is(..)).
     *          noRootPath()
     *          body("z.something1", is(..)).
     *          body("w.something2", is(..)).
     * </pre>
     * <p/>
     * This is the same as calling <code>rootPath("")</code> but more expressive.
     * Note that this method is exactly the same as {@link #noRoot()} but slightly more expressive.
     *
     * @see #root(String)
     */
    T noRootPath();

    /**
     * Append the given path to the root path of the response body so that you don't need to write the entire path for each expectation.
     * E.g. instead of writing:
     * <p/>
     * <pre>
     * get(..).then().
     *          root("x.y").
     *          body("age", is(..)).
     *          body("gender", is(..)).
     *          body("name.firstName", is(..)).
     *          body("name.lastName", is(..));
     * </pre>
     * <p/>
     * you can use a append root and do:
     * <pre>
     * get(..).then().
     *          root("x.y").
     *          body("age", is(..)).
     *          body("gender", is(..)).
     *          appendRoot("name").
     *          body("firstName", is(..)).
     *          body("lastName", is(..));
     * </pre>
     *
     * @param pathToAppend The root path to use.
     */
    T appendRoot(String pathToAppend);

    /**
     * Append the given path to the root path with arguments supplied of the response body so that you don't need to write the entire path for each expectation.
     * This is mainly useful when you have parts of the path defined in variables.
     * E.g. instead of writing:
     * <p/>
     * <pre>
     * String namePath = "name";
     * get(..).then().
     *           root("x.y").
     *           body("age", is(..)).
     *           body("gender", is(..)).
     *           body(namePath + "first", is(..)).
     *           body(namePath + "last", is(..)).
     * </pre>
     * <p/>
     * you can use a append root and do:
     * <pre>
     * String namePath = "name";
     * get(..).then().
     *          root("x.y").
     *          body("age", is(..)).
     *          body("gender", is(..)).
     *          appendRoot("%s", withArgs(namePath)).
     *          body("first", is(..)).
     *          body("last", is(..)).
     * </pre>
     *
     * @param pathToAppend The root path to use. The path and arguments follows the standard <a href="http://download.oracle.com/javase/1,5.0/docs/api/java/util/Formatter.html#syntax">formatting syntax</a> of Java.
     */
    T appendRoot(String pathToAppend, List<Argument> arguments);

    /**
     * Set the response content type to be <code>contentType</code>.
     * <p>Note that this will affect the way the response is decoded.
     * E,g. if you can't use JSON/XML matching (see e.g. {@link #body(String, org.hamcrest.Matcher, Object...)}) if you specify a
     * content-type of "text/plain". If you don't specify the response content type REST Assured will automatically try to
     * figure out which content type to use.</p>
     *
     * @param contentType The content type of the response.
     * @return the response specification
     */
    T contentType(ContentType contentType);

    /**
     * Set the response content type to be <code>contentType</code>.
     * <p>Note that this will affect the way the response is decoded.
     * E,g. if you can't use JSON/XML matching (see e.g. {@link #body(String, org.hamcrest.Matcher, Object...)}) if you specify a
     * content-type of "text/plain". If you don't specify the response content type REST Assured will automatically try to
     * figure out which content type to use.</p>
     *
     * @param contentType The content type of the response.
     * @return the response specification
     */
    T contentType(String contentType);

    /**
     * Validate the response content type to be <code>contentType</code>.
     *
     * @param contentType The expected content type of the response.
     * @return the response specification
     */
    T contentType(Matcher<? super String> contentType);

    /**
     * Validate that the response body conforms to one or more Hamcrest matchers. E.g.
     * <pre>
     * // Validate that the response body (content) contains the string "winning-numbers"
     * get("/lotto").then().assertThat().body(containsString("winning-numbers"));
     *
     * // Validate that the response body (content) contains the string "winning-numbers" and "winners"
     * get("/lotto").then().assertThat().body(containsString("winning-numbers"), containsString("winners"));
     * </pre>
     *
     * @param matcher            The hamcrest matcher that must response body must match.
     * @param additionalMatchers Optionally additional hamcrest matchers that must return <code>true</code>.
     * @return the response specification
     */
    T body(Matcher<?> matcher, Matcher<?>... additionalMatchers);

    /**
     * Validate that the JSON or XML response body conforms to one or more Hamcrest matchers.<br>
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
     * get("/lotto").then().assertThat().body("lotto.lottoId", equalTo(5));
     * </pre>
     * <p/>
     * You can also verify that e.g. one of the the winning numbers is 45.
     * <pre>
     * get("/lotto").then().assertThat().body("lotto.winning-numbers", hasItem(45));
     * </pre>
     * <p/>
     * Or both at the same time:
     * <pre>
     * get("/lotto").then().assertThat().body("lotto.lottoId", equalTo(5)).and().body("lotto.winning-numbers", hasItem(45));
     * </pre>
     * <p/>
     * or a slightly short version:
     * <pre>
     * get("/lotto").then().assertThat().body("lotto.lottoId", equalTo(5), "lotto.winning-numbers", hasItem(45));
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
     * get("/xml").then().assertThat().body("greeting.firstName", equalTo("John"));
     * </pre>
     * <p/>
     * To verify both the first name and last name you can do like this:
     * <pre>
     * get("/xml").then().assertThat().body("greeting.firstName", equalTo("John")).and().body("greeting.lastName", equalTo("Doe"));
     * </pre>
     * <p/>
     * Or the slightly shorter version of:
     * <pre>
     * get("/xml").then().assertThat().body("greeting.firstName", equalTo("John"), "greeting.lastName", equalTo("Doe"));
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
    T body(String path, Matcher<?> matcher, Object... additionalKeyMatcherPairs);

    /**
     * Same as {@link #body(String, java.util.List, org.hamcrest.Matcher, Object...)} expect that you can pass arguments to the path. This
     * is useful in situations where you have e.g. pre-defined variables that constitutes the path:
     * <pre>
     * String someSubPath = "else";
     * int index = 1;
     * get("/x").then().assertThat().body("something.%s[%d]", withArgs(someSubPath, index), equalTo("some value")). ..
     * </pre>
     * <p/>
     * or if you have complex root paths and don't wish to duplicate the path for small variations:
     * <pre>
     * get("/x").then()
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
     * @param path                      The body path
     * @param matcher                   The hamcrest matcher that must response body must match.
     * @param additionalKeyMatcherPairs Optionally additional hamcrest matchers that must return <code>true</code>.
     * @return the response specification
     * @see #content(String, org.hamcrest.Matcher, Object...)
     */
    T content(String path, List<Argument> arguments, Matcher matcher, Object... additionalKeyMatcherPairs);

    /**
     * Syntactic sugar, e.g.
     * <pre>
     * get("/something").then().assertThat().body(containsString("OK")).and().body(containsString("something else"));
     * </pre>
     * <p/>
     * is that same as:
     * <pre>
     * get("/something").then().assertThat().body(containsString("OK")).body(containsString("something else"));
     * </pre>
     *
     * @return the response specification
     */
    T and();

    /**
     * Syntactic sugar, e.g.
     * <pre>
     * get("/something").then().using().defaultParser(JSON).assertThat().body(containsString("OK")).and().body(containsString("something else"));
     * </pre>
     * <p/>
     * is that same as:
     * <pre>
     * get("/something").then().defaultParser(JSON).body(containsString("OK")).body(containsString("something else"));
     * </pre>
     *
     * @return the response specification
     */
    T using();

    /**
     * Syntactic sugar, e.g.
     * <pre>
     * get("/something").then().assertThat().body(containsString("OK")).and().body(containsString("something else"));
     * </pre>
     * <p/>
     * is that same as:
     * <pre>
     * get("/something").then().body(containsString("OK")).body(containsString("something else"));
     * </pre>
     *
     * @return the response specification
     */
    T assertThat();

    /**
     * Validate that the response matches an entire specification.
     * <pre>
     * ResponseSpecification responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
     * get("/something").then()
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
     * This method is the same as {@link #specification(com.jayway.restassured.specification.ResponseSpecification)} but the name is a bit shorter.
     *
     * @param responseSpecificationToMerge The specification to merge with.
     * @return the response specification
     */
    T spec(ResponseSpecification responseSpecificationToMerge);

    /**
     * Validate that the response matches an entire specification.
     * <pre>
     * ResponseSpecification responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
     *
     * get("/something").then().
     *         specification(responseSpec).
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
     * <p/>
     * This method is the same as {@link #spec(com.jayway.restassured.specification.ResponseSpecification)} but the name is a bit longer.
     *
     * @param responseSpecificationToMerge The specification to merge with.
     * @return the response specification
     */
    T specification(ResponseSpecification responseSpecificationToMerge);

    /**
     * Register a content-type to be parsed using a predefined parser. E.g. let's say you want parse
     * content-type <tt>application/custom</tt> with the XML parser to be able to verify the response using the XML dot notations:
     * <pre>
     * get("/x").then().assertThat().body("document.child", equalsTo("something")
     * </pre>
     * Since <tt>application/custom</tt> is not registered to be processed by the XML parser by default you need to explicitly
     * tell REST Assured to use this parser before making the request:
     * <pre>
     * get("/x").then().parser("application/custom", Parser.XML).assertThat(). ..;
     * </pre>
     * <p/>
     * You can also specify by it for every response by using:
     * <pre>
     * RestAssured.registerParser("application/custom", Parser.XML);
     * </pre>
     *
     * @param contentType The content-type to register
     * @param parser      The parser to use when verifying the response.
     */
    T parser(String contentType, Parser parser);

    /**
     * Register a default predefined parser that will be used if no other parser (registered or pre-defined) matches the response
     * content-type. E.g. let's say that for some reason no content-type is defined in the response but the content is nevertheless
     * JSON encoded. To be able to expect the content in REST Assured you need to set the default parser:
     * <pre>
     * get("/x").then().using().defaultParser(Parser.JSON).assertThat(). ..;
     * </pre>
     * <p/>
     * You can also specify it for every response by using:
     * <pre>
     * RestAssured.defaultParser(Parser.JSON);
     * </pre>
     *
     * @param parser The parser to use when verifying the response if no other parser is found for the response content-type.
     */
    T defaultParser(Parser parser);

    /**
     * Extract values from the response or return the response instance itself. This is useful for example if you want to use values from the
     * response in sequent requests. For example given that the resource <code>title</code> returns the following JSON
     * <pre>
     * {
     *     "title" : "My Title",
     *      "_links": {
     *              "self": { "href": "/title" },
     *              "next": { "href": "/title?page=2" }
     *           }
     * }
     * </pre>
     * and you want to validate that content type is equal to <code>JSON</code> and the title is equal to <code>My Title</code>
     * but you also want to extract the link to the "next" title to use that in a subsequent request. This is how:
     * <pre>
     * String nextTitleLink =
     * given().
     *         param("param_name", "param_value").
     * when().
     *         get("/title").
     * then().
     *         contentType(JSON).
     *         body("title", equalTo("My Title")).
     * extract().
     *         path("_links.next.href");
     *
     * get(nextTitleLink). ..
     * </pre>
     *
     * @return An instance of {@link com.jayway.restassured.response.ExtractableResponse}.
     */
    ExtractableResponse<R> extract();

    /**
     * Returns the {@link com.jayway.restassured.response.ValidatableResponseLogSpec} that allows you to log different parts of the {@link com.jayway.restassured.response.Response}.
     * This is mainly useful for debug purposes when writing your tests. I
     *
     * @return the validatable response log specification
     */
    ValidatableResponseLogSpec<T, R> log();
}
