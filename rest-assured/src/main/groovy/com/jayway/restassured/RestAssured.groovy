package com.jayway.restassured

import groovyx.net.http.Method
import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.GET
import com.jayway.restassured.assertion.Assertion
import com.jayway.restassured.assertion.JSONAssertion

class RestAssured {

  public static String baseURI = "http://localhost";

  public static int port = 8080;

  def static Map<String, Object> map(Object...parameters) {
    if(parameters == null || parameters.length < 2) {
      throw new IllegalArgumentException("You must supply at least one key and one value.");
    } else if(parameters.length % 2 != 0) {
      throw new IllegalArgumentException("You must supply the same number of keys as values.")
    }

    Map<String, Object> map = new HashMap<String,Object>();
    for (int i = 0; i < parameters.length; i+=2) {
      map.put(parameters[i], parameters[i+1]);
    }
    return map;
  }

  def static RequestBuilder with() {
      return new RequestBuilder(baseUri: RestAssured.baseURI, path: "", port: port, method: GET)    
  }

  def static RequestBuilder given() {
      return new RequestBuilder(baseUri: RestAssured.baseURI, path: "", port: port, method: GET)
  }

  def static RequestBuilder post() {
    return new RequestBuilder(baseUri: RestAssured.baseURI, path: "", port: port, method: POST)
  }

  def static RequestBuilder post(String path) {
    return new RequestBuilder(baseUri: RestAssured.baseURI, path: path, port: port, method: POST)
  }

  def static RequestBuilder get() {
    return new RequestBuilder(baseUri: RestAssured.baseURI, path: "", port: port, method: GET)
  }

  def static RequestBuilder get(String path) {
    return new RequestBuilder(baseUri: RestAssured.baseURI, path: path, port: port, method: GET)
  }

  private static def test(path,method, successHandler) {
    test(path, method, null, successHandler)
  }

  private static def test(path,method, query, successHandler) {
    performTest(path, method, query, successHandler, { resp ->
      throw new RuntimeException("Unexpected error: ${resp.statusLine.statusCode} : ${resp.statusLine.reasonPhrase}")
    });
  }

  private static def testFailure(path, method, failureHandler) {
    testFailure(path, method, null, failureHandler);
  }

  private static def testFailure(path, method, query, failureHandler) {
    performTest(path, method, query,  { response, json ->   fail String.format("Test didn't fail. Got response %s and JSON %s.", response, json) }, failureHandler)
  }

  
}