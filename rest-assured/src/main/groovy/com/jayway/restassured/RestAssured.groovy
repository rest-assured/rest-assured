package com.jayway.restassured

import com.jayway.restassured.internal.RequestSpecificationImpl
import com.jayway.restassured.internal.ResponseSpecificationImpl
import com.jayway.restassured.specification.RequestSpecification
import com.jayway.restassured.specification.ResponseSpecification
import com.jayway.restassured.specification.TestSpecification

/**
 * REST Assured is a Java DSL for simplifying testing of REST based services. It supports making POST, GET, PUT, DELETE and HEAD
 * requests and verify the response. Usage examples:
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
 * expect().body(hasXPath("/greeting/firstName", containsString("Jo"))).given().parameters("firstName", "John", "lastName", "Doe").when().get("/greetXML");
 * </pre>
 * or
 * <pre>
 * expect().body(hasXPath("/greeting/firstName[text()='John']")).with().parameters("firstName", "John", "lastName", "Doe").post("/greetXML");
 * </pre>
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
 * Fully body/content matching:
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
 * You can also change the default base URI and port for all subsequent requests:
 * <pre>
 * RestAssured.baseURI = "http://myhost.org";
 * RestAssured.port = 80;
 * </pre>
 * This means that a request like e.g. <code>get("/hello")</code> goes to: <tt>http://myhost.org:8080/hello</tt><br>
 * You can reset to the standard baseURI (localhost) and standard port (8080) using:
 * <pre>
 * RestAssured.reset();
 * </pre>
 * </li>
 * </ol>
 */
class RestAssured {

  public static final String defaultURI = "http://localhost"
  public static final int defaultPort = 8080

  public static String baseURI = defaultURI
  public static int port = defaultPort;

  def static ResponseSpecification expect() {
    createTestSpecification().responseSpecification
  }

  private static TestSpecification createTestSpecification() {
    return new TestSpecification(new RequestSpecificationImpl(baseUri: baseURI, path: "", port: port), new ResponseSpecificationImpl())
  }

  def static RequestSpecification with() {
    return given()
  }

  def static RequestSpecification given() {
    return createTestSpecification().requestSpecification
  }


  def static TestSpecification given(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
    return new TestSpecification(requestSpecification, responseSpecification);
  }

  def static void reset() {
    baseURI = defaultURI
    port = defaultPort
  }
}