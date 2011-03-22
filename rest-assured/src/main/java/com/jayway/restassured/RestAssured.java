/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jayway.restassured;

import com.jayway.restassured.authentication.*;
import com.jayway.restassured.internal.RequestSpecificationImpl;
import com.jayway.restassured.internal.ResponseParserRegistrar;
import com.jayway.restassured.internal.ResponseSpecificationImpl;
import com.jayway.restassured.internal.TestSpecificationImpl;
import com.jayway.restassured.parsing.Parser;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSender;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;

/**
 * REST Assured is a Java DSL for simplifying testing of REST based services built on top of
 * <a href="http://groovy.codehaus.org/modules/http-builder/">HTTP Builder</a>.
 * It supports POST, GET, PUT, DELETE and HEAD
 * requests and to verify the response of these requests. Usage examples:
 *<ol>
 * <li>
 * Assume that the GET request (to <tt>http://localhost:8080/lotto</tt>) returns JSON as:
 * <pre>
 * {
 * "lotto":{
 *   "lottoId":5,
 *   "winning-numbers":[2,45,34,23,7,5,3],
 *   "winners":[{
 *     "winnerId":23,
 *     "numbers":[2,45,34,23,3,5]
 *   },{
 *     "winnerId":54,
 *     "numbers":[52,3,12,11,18,22]
 *   }]
 *  }
 * }
 * </pre>
 *
 * REST assured can then help you to easily make the GET request and verify the response. E.g. if you want to verify
 * that <tt>lottoId</tt> is equal to 5 you can do like this:
 *
 * <pre>
 * expect().body("lotto.lottoId", equalTo(5)).when().get("/lotto");
 * </pre>
 *
 * or perhaps you want to check that the winnerId's are 23 and 54:
 * <pre>
 *  expect().body("lotto.winners.winnerId", hasItems(23, 54)).when().get("/lotto");
 * </pre>
 * </li>
 * <li>
 * XML can be verified in a similar way. Image that a POST request to <tt>http://localhost:8080/greetXML<tt>  returns:
 * <pre>
 * &lt;greeting&gt;
 *     &lt;firstName&gt;{params("firstName")}&lt;/firstName&gt;
 *     &lt;lastName&gt;{params("lastName")}&lt;/lastName&gt;
 *   &lt;/greeting&gt;
 * </pre>
 *
 * i.e. it sends back a greeting based on the <tt>firstName</tt> and <tt>lastName</tt> parameter sent in the request.
 * You can easily perform and verify e.g. the <tt>firstName</tt> with REST assured:
 * <pre>
 * with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting.firstName", equalTo("John")).when().post("/greetXML");
 * </pre>
 *
 * If you want to verify both <tt>firstName</tt> and <tt>lastName</tt> you may do like this:
 * <pre>
 * with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting.firstName", equalTo("John")).and().body("greeting.lastName", equalTo("Doe")).when().post("/greetXML");
 * </pre>
 *
 * or a little shorter:
 * <pre>
 * with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting.firstName", equalTo("John"), "greeting.lastName", equalTo("Doe")).when().post("/greetXML");
 * </pre>
 * </li>
 * <li>
 * You can also verify XML responses using x-path. For example:
 * <pre>
 * expect().body(hasXPath("/greeting/firstName", containsString("Jo"))).given().parameters("firstName", "John", "lastName", "Doe").when().post("/greetXML");
 * </pre>
 * or
 * <pre>
 * expect().body(hasXPath("/greeting/firstName[text()='John']")).with().parameters("firstName", "John", "lastName", "Doe").post("/greetXML");
 * </pre>
 * </li>
 * <li>
 * XML response bodies can also be verified against an XML Schema (XSD) or DTD. <br>XSD example:
 * <pre>
 * expect().body(matchesXsd(xsd)).when().get("/carRecords");
 * </pre>
 * DTD example:
 * <pre>
 * expect().body(matchesDtd(dtd)).when().get("/videos");
 * </pre>
 * <code>matchesXsd</code> and <code>matchesDtd</code> are Hamcrest matchers which you can import from {@link com.jayway.restassured.matcher.RestAssuredMatchers}.
 * </li>
 * <li>
 * Besides specifying request parameters you can also specify headers, cookies, body and content type.<br>
 * <ul>
 * <li>
 * Cookie:
 * <pre>
 * given().cookie("username", "John").then().expect().body(equalTo("username")).when().get("/cookie");
 * </pre>
 * </li>
 * <li>
 * Headers:
 * <pre>
 * given().header("MyHeader", "Something").and(). ..
 * given().headers("MyHeader", "Something", "MyOtherHeader", "SomethingElse").and(). ..
 * </pre>
 * </li>
 * <li>
 * Content Type:
 * <pre>
 * given().contentType(ContentType.TEXT). ..
 * </pre>
 * </li>
 * <li>
 * Body:
 * <pre>
 * given().request().body("some body"). .. // Works for POST and PUT requests
 * given().request().body(new byte[]{42}). .. // Works for POST
 * </pre>
 * </li>
 * </ul>
 * </li>
 * <li>
 * You can also verify status code, status line, cookies, headers, content type and body.
 * <ul>
 * <li>
 * Cookie:
 * <pre>
 * expect().cookie("cookieName", "cookieValue"). ..
 * expect().cookies("cookieName1", "cookieValue1", "cookieName2", "cookieValue2"). ..
 * expect().cookies("cookieName1", "cookieValue1", "cookieName2", containsString("Value2")). ..
 * </pre>
 * </li>
 * <li>
 * Status:
 * <pre>
 * expect().statusCode(200). ..
 * expect().statusLine("something"). ..
 * expect().statusLine(containsString("some")). ..
 * </pre>
 * </li>
 * <li>
 * Headers:
 * <pre>
 * expect().header("headerName", "headerValue"). ..
 * expect().headers("headerName1", "headerValue1", "headerName2", "headerValue2"). ..
 * expect().headers("headerName1", "headerValue1", "headerName2", containsString("Value2")). ..
 * </pre>
 * </li>
 * <li>
 * Content-Type:
 * <pre>
 * expect().contentType(ContentType.JSON). ..
 * </pre>
 * </li>
 * <li>
 * Full body/content matching:
 * <pre>
 * expect().body(equalsTo("something")). ..
 * expect().content(equalsTo("something")). .. // Same as above
 * </pre>
 * </li>
 * </ul>
 * </li>
 * <li>
 * REST assured also supports some authentication schemes, for example basic authentication:
 * <pre>
 * given().auth().basic("username", "password").expect().statusCode(200).when().get("/secured/hello");
 * </pre>
 * Other supported schemes are OAuth and certificate authentication.
 * </li>
 * <li>
 * By default REST assured assumes host localhost and port 8080 when doing a request. If you want a different port you can do:
 * <pre>
 * given().port(80). ..
 * </pre>
 * or simply:
 * <pre>
 * .. when().get("http://myhost.org:80/doSomething");
 * </pre>
 * </li>
 * <li>
 * Parameters can also be set directly on the url:
 * <pre>
 * ..when().get("/name?firstName=John&lastName=Doe");
 * </pre>
 * </li>
 * <li>
 * You can use the {@link com.jayway.restassured.path.xml.XmlPath} or {@link com.jayway.restassured.path.json.JsonPath} to
 * easily parse XML or JSON data from a response.
 *    <ol>
 *        <li>XML example:
 *        <pre>
 *            String xml = post("/greetXML?firstName=John&lastName=Doe").andReturn().asString();
 *            // Now use XmlPath to get the first and last name
 *            String firstName = with(xml).get("greeting.firstName");
 *            String lastName = with(xml).get("greeting.firstName");
 *
 *            // or a bit more efficiently:
 *            XmlPath xmlPath = new XmlPath(xml).setRoot("greeting");
 *            String firstName = xmlPath.get("firstName");
 *            String lastName = xmlPath.get("lastName");
 *        </pre>
 *        </li>
 *        <li>JSON example:
 *        <pre>
 *            String json = get("/lotto").asString();
 *            // Now use JsonPath to get data out of the JSON body
 *            int lottoId = with(json).getInt("lotto.lottoId);
 *            List<Integer> winnerIds = with(json).get("lotto.winners.winnerId");
 *
 *            // or a bit more efficiently:
 *            JsonPath jsonPath = new JsonPath(json).setRoot("lotto");
 *            int lottoId = jsonPath.getInt("lottoId");
 *            List<Integer> winnderIds = jsonPath.get("winnders.winnderId");
 *        </pre>
 *        </li>
 *    </ol>
 * </li>
 * <li>
 *  REST Assured providers predefined parsers for e.g. HTML, XML and JSON. But you can parse other kinds of content by registering a predefined parser for unsupported mime-types by using:
 * <pre>
 * RestAssured.registerParser(&lt;mime-type&gt;, &lt;parser&gt;);
 * </pre>
 * E.g. to register that mime-type <code>'application/vnd.uoml+xml'</code> should be parsed using the XML parser do:
 * <pre>
 * RestAssured.registerParser("application/vnd.uoml+xml", Parser.XML);
 * </pre>
 * You can also unregister a parser using:
 * <pre>
 * RestAssured.unregisterParser("application/vnd.uoml+xml");
 * </pre>
 * </li>
 * <li>
 * You can also change the default base URI, base path, port and authentication scheme for all subsequent requests:
 * <pre>
 * RestAssured.baseURI = "http://myhost.org";
 * RestAssured.port = 80;
 * RestAssured.basePath = "/resource";
 * RestAssured.authentication = basic("username", "password");
 * </pre>
 * This means that a request like e.g. <code>get("/hello")</code> goes to: <tt>http://myhost.org:8080/resource/hello</tt>
 * which basic authentication credentials "username" and "password".<br>
 * You can reset to the standard baseURI (localhost), basePath (empty), standard port (8080) and default authentication scheme (none) using:
 * <pre>
 * RestAssured.reset();
 * </pre>
 * </li>
 * </ol>
 * <p>
 * In order to use REST assured effectively it's recommended to statically import
 * methods from the following classes:
 * <ul>
 * <li>com.jayway.restassured.RestAssured.*</li>
 * <li>com.jayway.restassured.matcher.RestAssuredMatchers.*</li>
 * <li>org.hamcrest.Matchers.*</li>
 * </ul>
 * </p>
 */
public class RestAssured {

    public static final String DEFAULT_URI = "http://localhost";
    public static final int DEFAULT_PORT = 8080;
    public static final String DEFAULT_PATH = "";
    public static final AuthenticationScheme DEFAULT_AUTH = new NoAuthScheme();

    /**
     * The base URI that's used by REST assured when making requests if a non-fully qualified URI is used in the request.
     * Default value is {@value #DEFAULT_URI}.
     */
    public static String baseURI = DEFAULT_URI;

    /**
     * The port that's used by REST assured when is left out of the specified URI when making a request.
     * Default value is {@value #DEFAULT_PORT}.
     */
    public static int port = DEFAULT_PORT;

    /**
     * A base path that's added to the {@link #baseURI} by REST assured when making requests. E.g. let's say that
     * the {@link #baseURI} is <code>http://localhost</code> and <code>basePath</code> is <code>/resource</code>
     * then
     *
     * <pre>
     * ..when().get("/something");
     * </pre>
     *
     * will make a request to <code>http://localhost/resource</code>.
     * Default <code>basePath</code> value is empty.
     */
    public static String basePath = DEFAULT_PATH;

    /**
     * Set an authentication scheme that should be used for each request. By default no authentication is used.
     * If you have specified an authentication scheme and wish to override it for a single request then
     * you can do this using:
     *
     * <pre>
     *     given().auth().none()..
     * </pre>
     *
     */
    public static AuthenticationScheme authentication = DEFAULT_AUTH;

    /**
     * Start building the response part of the test com.jayway.restassured.specification. E.g.
     *
     * <pre>
     * expect().body("lotto.lottoId", equalTo(5)).when().get("/lotto");
     * </pre>
     *
     * will expect that the response body for the GET request to "/lotto" should
     * contain JSON or XML which has a lottoId equal to 5.
     *
     * @return A response com.jayway.restassured.specification.
     */
    public static ResponseSpecification expect() {
        return createTestSpecification().getResponseSpecification();
    }

    /**
     * Start building the request part of the test com.jayway.restassured.specification. E.g.
     *
     * <pre>
     * with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting.firstName", equalTo("John")).when().post("/greetXML");
     * </pre>
     *
     * will send a POST request to "/greetXML" with request parameters <tt>firstName=John</tt> and <tt>lastName=Doe</tt> and
     * expect that the response body containing JSON or XML firstName equal to John.
     *
     * The only difference between {@link #with()} and {@link #given()} is syntactical.
     *
     * @return A request com.jayway.restassured.specification.
     */
    public static RequestSpecification with() {
        return given();
    }

    /**
     * Start building the request part of the test com.jayway.restassured.specification. E.g.
     *
     * <pre>
     * given().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting.firstName", equalTo("John")).when().post("/greetXML");
     * </pre>
     *
     * will send a POST request to "/greetXML" with request parameters <tt>firstName=John</tt> and <tt>lastName=Doe</tt> and
     * expect that the response body containing JSON or XML firstName equal to John.
     *
     * The only difference between {@link #with()} and {@link #given()} is syntactical.
     *
     * @return A request com.jayway.restassured.specification.
     */
    public static RequestSpecification given() {
        return createTestSpecification().getRequestSpecification();
    }

    /**
     * When you have long specifications it can be better to split up the definition of response and request specifications in multiple lines.
     * You can then pass the response and request specifications to this method. E.g.
     *
     * <pre>
     * RequestSpecification requestSpecification = with().parameters("firstName", "John", "lastName", "Doe");
     * ResponseSpecification responseSpecification = expect().body("greeting", equalTo("Greetings John Doe"));
     * given(requestSpecification, responseSpecification).get("/greet");
     * </pre>
     *
     * This will perform a GET request to "/greet" and verify it according to the <code>responseSpecification</code>.
     *
     * @return A test com.jayway.restassured.specification.
     */
    public static RequestSender given(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        return new TestSpecificationImpl(requestSpecification, responseSpecification);
    }

    /**
     * Perform a GET request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path The path to send the request to.
     * @return The response of the GET request. The response can only be returned if you don't use any REST Assured response expectations.
     */
    public static Response get(String path) {
        return given().get(path);
    }

    /**
     * Perform a POST request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path The path to send the request to.
     * @return The response of the request. The response can only be returned if you don't use any REST Assured response expectations.
     */
    public static Response post(String path) {
        return given().post(path);
    }

    /**
     * Perform a PUT request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path The path to send the request to.
     * @return The response of the request. The response can only be returned if you don't use any REST Assured response expectations.
     */
    public static Response put(String path) {
        return given().put(path);
    }

    /**
     * Perform a DELETE request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path The path to send the request to.
     * @return The response of the request. The response can only be returned if you don't use any REST Assured response expectations.
     */
    public static Response delete(String path) {
        return given().delete(path);
    }

    /**
     * Perform a HEAD request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path The path to send the request to.
     * @return The response of the request. The response can only be returned if you don't use any REST Assured response expectations.
     */
    public static Response head(String path) {
        return given().head(path);
    }

    /**
     * Create a http basic authentication scheme.
     *
     * @param userName The user name.
     * @param password The password.
     * @return The authentication scheme
     */
    public static AuthenticationScheme basic(String userName, String password) {
        final BasicAuthScheme scheme = new BasicAuthScheme();
        scheme.setUserName(userName);
        scheme.setPassword(password);
        return scheme;
    }


    /**
     * Return the http preemptive authentication specification for setting up preemptive authentication requests.
     * This means that the authentication details are sent in the request header regardless if the server challenged
     * for authentication or not.
     *
     * @return The authentication scheme
     */
    public static PreemptiveAuthProvider preemptive() {
        return new PreemptiveAuthProvider();
    }

    /**
     * Sets a certificate to be used for SSL authentication. See {@link Class#getResource(String)}
     * for how to get a URL from a resource on the classpath.
     *
     * @param certURL URL to a JKS keystore where the certificate is stored.
     * @param password  password to decrypt the keystore
     * @return The authentication scheme
     */
    public static AuthenticationScheme certificate(String certURL, String password) {
        final CertAuthScheme scheme = new CertAuthScheme();
        scheme.setCertURL(certURL);
        scheme.setPassword(password);
        return scheme;
    }

    /**
     * Use http digest authentication.
     *
     * @param userName The user name.
     * @param password The password.
     * @return The authentication scheme
     */
    public static AuthenticationScheme digest(String userName, String password) {
        return basic(userName, password);
    }

    /**
     * Excerpt from the HttpBuilder docs:<br>
     * OAuth sign the request. Note that this currently does not wait for a WWW-Authenticate challenge before sending the the OAuth header.
     * All requests to all domains will be signed for this instance.
     * This assumes you've already generated an accessToken and secretToken for the site you're targeting.
     * For More information on how to achieve this, see the <a href="http://code.google.com/p/oauth-signpost/wiki/GettingStarted#Using_Signpost">Signpost documentation</a>.
     *
     * @param consumerKey
     * @param consumerSecret
     * @param accessToken
     * @param secretToken
     * @return The authentication scheme
     */
    public static AuthenticationScheme oauth(String consumerKey, String consumerSecret, String accessToken, String secretToken) {
        OAuthScheme scheme = new OAuthScheme();
        scheme.setConsumerKey(consumerKey);
        scheme.setConsumerSecret(consumerSecret);
        scheme.setAccessToken(accessToken);
        scheme.setSecretToken(secretToken);
        return scheme;
    }

    /**
     * Register a custom mime-type to be parsed using a predefined parser. E.g. let's say you want parse
     * mime-type <tt>application/vnd.uoml+xml</tt> with the XML parser to be able to verify the response using the XML dot notations:
     * <pre>
     * expect().body("document.child", equalsTo("something"))..
     * </pre>
     * Since <tt>application/vnd.uoml+xml</tt> is not registered to be processed by the XML parser by default you need to explicitly
     * tell REST Assured to use this parser before making the request:
     * <pre>
     * RestAssured.registerParser("application/vnd.uoml+xml, Parser.XML");
     * </pre>
     *
     * @param mimeType The mime-type to register
     * @param parser The parser to use when verifying the response.
     */
    public static void registerParser(String mimeType, Parser parser) {
        ResponseParserRegistrar.registerParser(mimeType, parser);
    }

    /**
     * Unregister the parser associated with the provided mime-type
     *
     * @param mimeType The mime-type associated with the parser to unregister.
     */
    public static void unregisterParser(String mimeType) {
        ResponseParserRegistrar.unregisterParser(mimeType);
    }

    /**
     * Resets the {@link #baseURI}, {@link #basePath}, {@link #port} and {@link #authentication} to their default values of
     * {@value #DEFAULT_URI}, {@value #DEFAULT_PATH}, {@value #DEFAULT_PORT} and <code>no authentication</code>.
     */
    public static void reset() {
        baseURI = DEFAULT_URI;
        port = DEFAULT_PORT;
        basePath = DEFAULT_PATH;
        authentication = DEFAULT_AUTH;
    }

    private static TestSpecificationImpl createTestSpecification() {
        return new TestSpecificationImpl(new RequestSpecificationImpl(baseURI, port, basePath, authentication), new ResponseSpecificationImpl());
    }
}
