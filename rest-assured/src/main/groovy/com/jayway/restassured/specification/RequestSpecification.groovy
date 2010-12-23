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

package com.jayway.restassured.specification

import groovyx.net.http.ContentType

/**
 * Allows you to specify how the request will look like.
 */
public interface RequestSpecification extends RequestSender {

  /**
   * Specify a String request body (such as e.g. JSON or XML) that'll be sent with the request. This works for the
   * POST and PUT methods only. Trying to do this for the other http methods will cause an exception to be thrown.
   * <p>
   * Example of use:
   * <pre>
   * given().body("{ \"message\" : \"hello world\"}").then().expect().body(equalTo("hello world")).when().post("/json");
   * </pre>
   * This will POST a request containing JSON to "/json" and expect that the response body equals to "hello world".
   * </p>
   *
   * <p>
   * Note that {@link #body(String)} and {@link #content(String)} are the same except for the syntactic difference.
   * </p>
   *
   * @param body The body to send.
   * @return The request specification
   */
  RequestSpecification body(String body);

  /**
   * Specify a byte array request body that'll be sent with the request. This only works for the
   * POST http method. Trying to do this for the other http methods will cause an exception to be thrown.
   * <p>
   * Example of use:
   * <pre>
   * byte[] someBytes = ..
   * given().body(someBytes).then().expect().body(equalTo("hello world")).when().post("/json");
   * </pre>
   * This will POST a request containing <code>someBytes</code> to "/json" and expect that the response body equals to "hello world".
   * </p>
   *
   * <p>
   * Note that {@link #body(byte[])} and {@link #content(byte[])} are the same except for the syntactic difference.
   * </p>
   *
   * @param body The body to send.
   * @return The request specification
   */
  RequestSpecification body(byte[] body);

  /**
   * Specify a String request content (such as e.g. JSON or XML) that'll be sent with the request. This works for the
   * POST and PUT methods only. Trying to do this for the other http methods will cause an exception to be thrown.
   * <p>
   * Example of use:
   * <pre>
   * given().content("{ \"message\" : \"hello world\"}").then().expect().content(equalTo("hello world")).when().post("/json");
   * </pre>
   * This will POST a request containing JSON to "/json" and expect that the response content equals to "hello world".
   * </p>
   *
   * <p>
   * Note that {@link #body(String)} and {@link #content(String)} are the same except for the syntactic difference.
   * </p>
   *
   * @param content The content to send.
   * @return The request specification
   */
  RequestSpecification content(String content);

  /**
   * Specify a byte array request content that'll be sent with the request. This only works for the
   * POST http method. Trying to do this for the other http methods will cause an exception to be thrown.
   * <p>
   * Example of use:
   * <pre>
   * byte[] someBytes = ..
   * given().content(someBytes).then().expect().content(equalTo("hello world")).when().post("/json");
   * </pre>
   * This will POST a request containing <code>someBytes</code> to "/json" and expect that the response content equals to "hello world".
   * </p>
   *
   * <p>
   * Note that {@link #body(byte[])} and {@link #content(byte[])} are the same except for the syntactic difference.
   * </p>
   *
   * @param content The content to send.
   * @return The request specification
   */
  RequestSpecification content(byte[] content);

  /**
   * Specify the cookies that'll be sent with the request. This is done by specifying the cookies in name-value pairs, e.g:
   * <pre>
   * given().cookies("username", "John", "token", "1234").then().expect().body(equalTo("username, token")).when().get("/cookie");
   * </pre>
   *
   * This will send a GET request to "/cookie" with two cookies:
   * <ol>
   *   <li>username=John</li>
   *   <li>token=1234</li>
   * </ol>
   * and expect that the response body is equal to "username, token".
   *
   * @param cookieName The name of the first cookie
   * @param cookieNameValuePairs The value of the first cookie followed by additional cookies in name-value pairs.
   * @return The request specification
   */
  RequestSpecification cookies(String cookieName, String...cookieNameValuePairs);

  /**
   * Specify the cookies that'll be sent with the request as Map e.g:
   * <pre>
   * Map&lt;String, String&gt; cookies = new HashMap&lt;String, String&gt;();
   * cookies.put("username", "John");
   * cookies.put("token", "1234");
   * given().cookies(cookies).then().expect().body(equalTo("username, token")).when().get("/cookie");
   * </pre>
   *
   * This will send a GET request to "/cookie" with two cookies:
   * <ol>
   *   <li>username=John</li>
   *   <li>token=1234</li>
   * </ol>
   * and expect that the response body is equal to "username, token".
   *
   * @param cookies The Map containing the cookie names and their values to set in the request.
   * @return The request specification
   */
  RequestSpecification cookies(Map<String, String> cookies);

  /**
   * Specify a cookie that'll be sent with the request e.g:
   * <p>
   * <pre>
   * given().cookie("username", "John").and().expect().body(equalTo("username")).when().get("/cookie");
   * </pre>
   * This will set the cookie <code>username=John</code> in the GET request to "/cookie".
   * </p>
   *
   * <p>
   * You can also specify several cookies like this:
   * <pre>
   * given().cookie("username", "John").and().cookie("password", "1234").and().expect().body(equalTo("username")).when().get("/cookie");
   * </pre>
   * </p>
   *
   * @see {@link #cookies(String, String[])} for an alternative for specifying multiple cookies.
   * @param key The cookie key
   * @param value The cookie value
   * @return The request specification
   */
  RequestSpecification cookie(String key, String value);

  RequestSpecification parameters(String parameter, String...parameters);

  RequestSpecification parameters(Map<String, String> parametersMap);

  RequestSpecification contentType(ContentType contentType);

  AuthenticationSpecification auth();

  AuthenticationSpecification authentication();

  ResponseSpecification response();

  RequestSpecification port(int port);

  RequestSpecification headers(Map<String, String> headers);

  RequestSpecification header(String key, String value);

  RequestSpecification headers(String headerName, String ... headerNameValuePairs);

  RequestSpecification and();

  RequestSpecification with();

  ResponseSpecification then();

  ResponseSpecification expect();

  RequestSpecification when();

  RequestSpecification given();

  RequestSpecification that();

  RequestSpecification request();
}