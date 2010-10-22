package com.jayway.restassured

import groovyx.net.http.Method
import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.GET

class RestAssured {

  public static String baseURI = "http://localhost";

  public static int port = 8080;


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