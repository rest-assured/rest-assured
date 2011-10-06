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
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.internal.*;
import com.jayway.restassured.internal.filter.FormAuthFilter;
import com.jayway.restassured.parsing.Parser;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.Argument;
import com.jayway.restassured.specification.RequestSender;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;
import groovyx.net.http.ContentType;
import org.apache.commons.lang.Validate;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
 * REST Assured also supports mapping a request body and response body to a Java object using Jackson, Gson or JAXB. Usage example:
 * <pre>
 * Greeting greeting = get("/greeting").as(Greeting.class);
 * </pre>
 * <pre>
 * Greeting greeting = new Greeting();
 * greeting.setFirstName("John");
 * greeting.setLastName("Doe");
 *
 * given().body(greeting).when().post("/greeting");
 * </pre>
 * See the javadoc for the body method for more details.
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
 *  REST Assured providers predefined parsers for e.g. HTML, XML and JSON. But you can parse other kinds of content by registering a predefined parser for unsupported content-types by using:
 * <pre>
 * RestAssured.registerParser(&lt;content-type&gt;, &lt;parser&gt;);
 * </pre>
 * E.g. to register that content-type <code>'application/vnd.uoml+xml'</code> should be parsed using the XML parser do:
 * <pre>
 * RestAssured.registerParser("application/vnd.uoml+xml", Parser.XML);
 * </pre>
 * You can also unregister a parser using:
 * <pre>
 * RestAssured.unregisterParser("application/vnd.uoml+xml");
 * </pre>
 * If can also specify a default parser for all content-types that do not match a pre-defined or registered parser. This is also useful if the response doesn't contain a content-type at all:
 * <pre>
 * RestAssured.defaultParser = Parser.JSON;
 * </pre>
 * </li>
 * <li>If you need to re-use a specification in multiple tests or multiple requests you can use the {@link com.jayway.restassured.builder.ResponseSpecBuilder}
 * and {@link com.jayway.restassured.builder.RequestSpecBuilder} like this:
 * <pre>
 * RequestSpecification requestSpec = new RequestSpecBuilder().addParameter("parameter1", "value1").build();
 * ResponseSpecification responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
 *
 * given().
 *         spec(requestSpec).
 * expect().
 *         spec(responseSpec).
 *         body("x.y.z", equalTo("something")).
 * when().
 *        get("/something");
 * </pre>
 * </li>
 * <li>You can also create filters and add to the request specification. A filter allows you to inspect and alter a request before it's actually committed and also inspect and alter the
 * response before it's returned to the expectations. You can regard it as an "around advice" in AOP terms. Filters can be used to implement custom authentication schemes, session management, logging etc. E.g.
 * <pre>
 * given().filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(302)). ..
 * </pre>
 * will log/print the response body to after each request.
 * </li>
 * <li>
 * You can also change the default base URI, base path, port, authentication scheme, root path and filters for all subsequent requests:
 * <pre>
 * RestAssured.baseURI = "http://myhost.org";
 * RestAssured.port = 80;
 * RestAssured.basePath = "/resource";
 * RestAssured.authentication = basic("username", "password");
 * RestAssured.rootPath = "store.book";
 * </pre>
 * This means that a request like e.g. <code>get("/hello")</code> goes to: <tt>http://myhost.org:8080/resource/hello</tt>
 * which basic authentication credentials "username" and "password". See {@link #rootPath} for more info about setting the root paths, {@link #filters(java.util.List)} for setting
 * default filters and {@link #keystore(String, String)} for setting the default keystore when using SSL.<br>
 * You can reset to the standard baseURI (localhost), basePath (empty), standard port (8080), default authentication scheme (none), default parser (none) and default root path (empty string) using:
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

    private static ResponseParserRegistrar RESPONSE_PARSER_REGISTRAR = new ResponseParserRegistrar();

    public static final String DEFAULT_URI = "http://localhost";
    public static final String DEFAULT_BODY_ROOT_PATH = "";
    public static final int DEFAULT_PORT = 8080;
    public static final String DEFAULT_PATH = "";
    public static final AuthenticationScheme DEFAULT_AUTH = new NoAuthScheme();
    public static final boolean DEFAULT_URL_ENCODING_ENABLED = true;

    /**
     * The base URI that's used by REST assured when making requests if a non-fully qualified URI is used in the request.
     * Default value is {@value #DEFAULT_URI}.
     */
    public static String baseURI = DEFAULT_URI;

    /**
     * The port that's used by REST assured when it's left out of the specified URI when making a request.
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
     * Specifies if Rest Assured should url encode the URL automatically. Usually this is a recommended but in some cases
     * e.g. the query parameters are already be encoded before you provide them to Rest Assured then it's useful to disable
     * URL encoding. For example:
     * <pre>
     * RestAssured.baseURI = "https://jira.atlassian.com";
     * RestAssured.port = 443;
     * RestAssured.urlEncodingEnabled = false; // Because "query" is already url encoded
     * String query = "project%20=%20BAM%20AND%20issuetype%20=%20Bug";
     * String response = get("/rest/api/2.0.alpha1/search?jql={q}",query).andReturn().asString();
     * ...
     * </pre>
     * The <code>query</code> is already url encoded so you need to disable Rest Assureds url encoding to prevent double encoding.
     */
    public static boolean urlEncodingEnabled = DEFAULT_URL_ENCODING_ENABLED;

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
     * Set the default root path of the response body so that you don't need to write the entire path for each expectation.
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
     * you can use a root and do:
     * <pre>
     * RestAssured.rootPath = "x.y";
     * expect().
     *          body("firstName", is(..)).
     *          body("lastName", is(..)).
     *          body("age", is(..)).
     *          body("gender", is(..)).
     * when().
     *          get(..);
     * </pre>
     */
    public static String rootPath = DEFAULT_BODY_ROOT_PATH;

    /**
     * Specify a default request specification that will be sent with each request. E,g.
     * <pre>
     * RestAssured.requestSpecification = new RequestSpecBuilder().addParameter("parameter1", "value1").build();
     * </pre>
     *
     * means that for each request by Rest Assured "parameter1" will be equal to "value1".
     */
    public static RequestSpecification requestSpecification = null;

    /**
     * Specify a default parser. This parser will be used if the response content-type
     * doesn't match any pre-registered or custom registered parsers. Also useful if the response
     * doesn't contain a content-type at all.
     */
    public static Parser defaultParser = null;

    /**
     * Specify a default response specification that will be sent with each request. E,g.
     * <pre>
     * RestAssured.responseSpecification = new ResponseSpecBuilder().expectStatusCode(200).build();
     * </pre>
     *
     * means that for each response Rest Assured will assert that the status code is equal to 200.
     */
    public static ResponseSpecification responseSpecification = null;

    private static Object requestContentType = null;

    private static Object responseContentType = null;

    private static KeystoreSpec keystoreSpec = new NoKeystoreSpecImpl();

    private static List<Filter> filters = new LinkedList<Filter>();

    /**
     * The following documentation is taken from <a href="HTTP Builder">http://groovy.codehaus.org/modules/http-builder/doc/ssl.html</a>:
     * <p>
     *     <h1>SSL Configuration</h1>
     *
     * SSL should, for the most part, "just work." There are a few situations where it is not completely intuitive. You can follow the example below, or see HttpClient's SSLSocketFactory documentation for more information.
     *
     * <h1>SSLPeerUnverifiedException</h1>
     *
     * If you can't connect to an SSL website, it is likely because the certificate chain is not trusted. This is an Apache HttpClient issue, but explained here for convenience. To correct the untrusted certificate, you need to import a certificate into an SSL truststore.
     *
     * First, export a certificate from the website using your browser. For example, if you go to https://dev.java.net in Firefox, you will probably get a warning in your browser. Choose "Add Exception," "Get Certificate," "View," "Details tab." Choose a certificate in the chain and export it as a PEM file. You can view the details of the exported certificate like so:
     * <pre>
     * $ keytool -printcert -file EquifaxSecureGlobaleBusinessCA-1.crt
     * Owner: CN=Equifax Secure Global eBusiness CA-1, O=Equifax Secure Inc., C=US
     * Issuer: CN=Equifax Secure Global eBusiness CA-1, O=Equifax Secure Inc., C=US
     * Serial number: 1
     * Valid from: Mon Jun 21 00:00:00 EDT 1999 until: Sun Jun 21 00:00:00 EDT 2020
     * Certificate fingerprints:
     * MD5:  8F:5D:77:06:27:C4:98:3C:5B:93:78:E7:D7:7D:9B:CC
     * SHA1: 7E:78:4A:10:1C:82:65:CC:2D:E1:F1:6D:47:B4:40:CA:D9:0A:19:45
     * Signature algorithm name: MD5withRSA
     * Version: 3
     * ....
     * </pre>
     * Now, import that into a Java keystore file:
     *<pre>
     * $ keytool -importcert -alias "equifax-ca" -file EquifaxSecureGlobaleBusinessCA-1.crt -keystore truststore.jks -storepass test1234
     * Owner: CN=Equifax Secure Global eBusiness CA-1, O=Equifax Secure Inc., C=US
     * Issuer: CN=Equifax Secure Global eBusiness CA-1, O=Equifax Secure Inc., C=US
     * Serial number: 1
     * Valid from: Mon Jun 21 00:00:00 EDT 1999 until: Sun Jun 21 00:00:00 EDT 2020
     * Certificate fingerprints:
     * MD5:  8F:5D:77:06:27:C4:98:3C:5B:93:78:E7:D7:7D:9B:CC
     * SHA1: 7E:78:4A:10:1C:82:65:CC:2D:E1:F1:6D:47:B4:40:CA:D9:0A:19:45
     * Signature algorithm name: MD5withRSA
     * Version: 3
     * ...
     * Trust this certificate? [no]:  yes
     * Certificate was added to keystore
     * </pre>
     * Now you want to use this truststore in your client:
     * <pre>
     * RestAssured.keystore("/truststore.jks", "test1234");
     * </pre>
     * or
     * <pre>
     * given().keystore("/truststore.jks", "test1234"). ..
     * </pre>
     * </p>
     * @param pathToJks The path to the JKS
     * @param password The store pass
     */
    public static void keystore(String pathToJks, String password) {
        Validate.notEmpty(pathToJks, "Path to java keystore cannot be empty");
        Validate.notEmpty(password, "Password cannot be empty");
        final KeystoreSpecImpl spec = new KeystoreSpecImpl();
        spec.setPath(pathToJks);
        spec.setPassword(password);
        RestAssured.keystoreSpec = spec;
    }

    /**
     * The the default filters to apply to each request.
     */
    public static void filters(List<Filter> filters) {
        RestAssured.filters.addAll(filters);
    }

    /**
     * @return The current default filters
     */
    public static List<Filter> filters() {
        return Collections.unmodifiableList(filters);
    }

    public static Object requestContentType() {
        return requestContentType;
    }

    public static Object responseContentType() {
        return responseContentType;
    }

    public static KeystoreSpec keystore() {
        return keystoreSpec;
    }

    /**
     * Specify the default content type
     *
     * @param contentType The content type
     */
    public static void requestContentType(ContentType contentType) {
        requestContentType = contentType;
    }

    /**
     * Specify the default content type
     *
     * @param contentType The content type
     */
    public static void requestContentType(String contentType) {
        requestContentType = contentType;
    }

    /**
     * Specify the default content type (also sets the accept header).
     *
     * @param contentType The content type
     */
    public static void responseContentType(ContentType contentType) {
        responseContentType = contentType;
    }

    /**
     * Specify the default content type (also sets the accept header).
     *
     * @param contentType The content type
     */
    public static void responseContentType(String contentType) {
        responseContentType = contentType;
    }

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
     * @return A request specification.
     */
    public static RequestSpecification with() {
        return given();
    }

    /**
     * Create a list of arguments that can be used to create parts of the path in a body/content expression.
     * This is useful in situations where you have e.g. pre-defined variables that constitutes the key. For example:
     * <pre>
     * String someSubPath = "else";
     * int index = 1;
     * expect().body("something.%s[%d]", withArgs(someSubPath, index), equalTo("some value")). ..
     * </pre>
     *
     * or if you have complex root paths and don't wish to duplicate the path for small variations:
     * <pre>
     * expect().
     *          root("filters.filterConfig[%d].filterConfigGroups.find { it.name == 'Gold' }.includes").
     *          body("", withArgs(0), hasItem("first")).
     *          body("", withArgs(1), hasItem("second")).
     *          ..
     * </pre>
     *
     * The key and arguments follows the standard <a href="http://download.oracle.com/javase/1,5.0/docs/api/java/util/Formatter.html#syntax">formatting syntax</a> of Java.
     *
     * @return A list of arguments that can be used to build up the
     */
    public static List<Argument> withArguments(Object firstArgument, Object...additionalArguments) {
        Validate.notNull(firstArgument, "You need to supply at least one argument");
        final List<Argument> arguments = new LinkedList<Argument>();
        arguments.add(Argument.arg(firstArgument));
        if(additionalArguments != null && additionalArguments.length > 0) {
            for (Object additionalArgument : additionalArguments) {
                arguments.add(Argument.arg(additionalArgument));
            }
        }
        return Collections.unmodifiableList(arguments);
    }

    /**
     * Slightly shorter version of {@link #withArguments(Object, Object...)}.
     *
     * @return A list of arguments.
     * @see #withArguments(Object, Object...)
     */
    public static List<Argument> withArgs(Object firstArgument, Object...additionalArguments) {
        return withArguments(firstArgument, additionalArguments);
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
     * @return A request specification.
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
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>get("/book/{hotelName}/{roomNumber}", "Hotels R Us", 22);</code>.
     * @return The response of the GET request. The response can only be returned if you don't use any REST Assured response expectations.
     */
    public static Response get(String path, Object...pathParams) {
        return given().get(path, pathParams);
    }

    /**
     * Perform a GET request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path The path to send the request to.
     * @param pathParams The path parameters.
     * @return The response of the GET request. The response can only be returned if you don't use any REST Assured response expectations.
     */
    public static Response get(String path, Map<String, ?> pathParams) {
        return given().get(path, pathParams);
    }

    /**
     * Perform a POST request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path The path to send the request to.
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>post("/book/{hotelName}/{roomNumber}", "Hotels R Us", 22);</code>.
     * @return The response of the request. The response can only be returned if you don't use any REST Assured response expectations.
     */
    public static Response post(String path, Object...pathParams) {
        return given().post(path, pathParams);
    }

    /**
     * Perform a POST request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path The path to send the request to.
     * @param pathParams The path parameters.
     * @return The response of the request. The response can only be returned if you don't use any REST Assured response expectations.
     */
    public static Response post(String path, Map<String, ?> pathParams) {
        return given().post(path, pathParams);
    }

    /**
     * Perform a PUT request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path The path to send the request to.
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>put("/book/{hotelName}/{roomNumber}", "Hotels R Us", 22);</code>.
     * @return The response of the request. The response can only be returned if you don't use any REST Assured response expectations.
     */
    public static Response put(String path, Object...pathParams) {
        return given().put(path, pathParams);
    }

    /**
     * Perform a DELETE request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path The path to send the request to.
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>delete("/book/{hotelName}/{roomNumber}", "Hotels R Us", 22);</code>.
     * @return The response of the request. The response can only be returned if you don't use any REST Assured response expectations.
     */
    public static Response delete(String path, Object...pathParams) {
        return given().delete(path, pathParams);
    }

    /**
     * Perform a DELETE request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path The path to send the request to.
     * @param pathParams The path parameters.
     * @return The response of the request. The response can only be returned if you don't use any REST Assured response expectations.
     */
    public static Response delete(String path, Map<String, ?> pathParams) {
        return given().delete(path, pathParams);
    }

    /**
     * Perform a HEAD request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path The path to send the request to.
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>head("/book/{hotelName}/{roomNumber}", "Hotels R Us", 22);</code>.
     * @return The response of the request. The response can only be returned if you don't use any REST Assured response expectations.
     */
    public static Response head(String path, Object...pathParams) {
        return given().head(path, pathParams);
    }

    /**
     * Perform a HEAD request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path The path to send the request to.
     * @param pathParams The path parameters.
     * @return The response of the request. The response can only be returned if you don't use any REST Assured response expectations.
     */
    public static Response head(String path, Map<String, ?> pathParams) {
        return given().head(path, pathParams);
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
     * Use form authentication. Rest Assured will try to parse the response
     * login page and determine and try find the action, username and password input
     * field automatically.
     * <p>
     * Note that the request will be much faster if you also supply a form auth configuration.
     * </p>
     *
     * @param userName The user name.
     * @param password The password.
     * @see #form(String, String, com.jayway.restassured.authentication.FormAuthConfig)
     * @return The authentication scheme
     */
    public static AuthenticationScheme form(String userName, String password) {
        return form(userName, password, null);
    }

    /**
     * Use form authentication with the supplied configuration.
     *
     * @param userName The user name.
     * @param password The password.
     * @param config The form authentication config
     * @return The authentication scheme
     */
    public static AuthenticationScheme form(String userName, String password, FormAuthConfig config) {
        if(userName == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }
        if(password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        final FormAuthScheme scheme = new FormAuthScheme();
        final FormAuthFilter authFilter = new FormAuthFilter();
        authFilter.setUserName(userName);
        authFilter.setPassword(password);
        authFilter.setConfig(config);
        filters.add(authFilter);
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
     * Register a custom content-type to be parsed using a predefined parser. E.g. let's say you want parse
     * content-type <tt>application/vnd.uoml+xml</tt> with the XML parser to be able to verify the response using the XML dot notations:
     * <pre>
     * expect().body("document.child", equalsTo("something"))..
     * </pre>
     * Since <tt>application/vnd.uoml+xml</tt> is not registered to be processed by the XML parser by default you need to explicitly
     * tell REST Assured to use this parser before making the request:
     * <pre>
     * RestAssured.registerParser("application/vnd.uoml+xml, Parser.XML");
     * </pre>
     *
     * @param contentType The content-type to register
     * @param parser The parser to use when verifying the response.
     */
    public static void registerParser(String contentType, Parser parser) {
        RESPONSE_PARSER_REGISTRAR.registerParser(contentType, parser);
    }

    /**
     * Unregister the parser associated with the provided content-type
     *
     * @param contentType The content-type associated with the parser to unregister.
     */
    public static void unregisterParser(String contentType) {
        RESPONSE_PARSER_REGISTRAR.unregisterParser(contentType);
    }

    /**
     * Resets the {@link #baseURI}, {@link #basePath}, {@link #port}, {@link #authentication} and {@link #rootPath}, {@link #requestContentType(groovyx.net.http.ContentType)},
     * {@link #responseContentType(groovyx.net.http.ContentType)}, {@link #filters(java.util.List)}, {@link #requestSpecification}, {@link #responseSpecification}. {@link #keystore(String, String)}
     * and {@link #urlEncodingEnabled} to their default values of {@value #DEFAULT_URI}, {@value #DEFAULT_PATH}, {@value #DEFAULT_PORT}, <code>no authentication</code>, "", <code>null</code>, <code>null</code>,
     * "empty list", <code>null</code>, <code>null</code>, <code>none</code>, <code>true</code>.
     */
    public static void reset() {
        baseURI = DEFAULT_URI;
        port = DEFAULT_PORT;
        basePath = DEFAULT_PATH;
        authentication = DEFAULT_AUTH;
        rootPath = DEFAULT_BODY_ROOT_PATH;
        filters = new LinkedList<Filter>();
        requestContentType = null;
        responseContentType = null;
        requestSpecification = null;
        responseSpecification = null;
        keystoreSpec = new NoKeystoreSpecImpl();
        urlEncodingEnabled = DEFAULT_URL_ENCODING_ENABLED;
        RESPONSE_PARSER_REGISTRAR = new ResponseParserRegistrar();
        defaultParser = null;
    }

    private static TestSpecificationImpl createTestSpecification() {
        if(defaultParser != null) {
            RESPONSE_PARSER_REGISTRAR.registerDefaultParser(defaultParser);
        }
        return new TestSpecificationImpl(
                new RequestSpecificationImpl(baseURI, port, basePath, authentication, filters, keystoreSpec, requestContentType, requestSpecification, urlEncodingEnabled),
                new ResponseSpecificationImpl(rootPath, responseContentType, responseSpecification, RESPONSE_PARSER_REGISTRAR));
    }
}
