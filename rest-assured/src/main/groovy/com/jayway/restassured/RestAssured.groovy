package com.jayway.restassured

import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.GET

class RestAssured {

  public static String baseURI = "http://localhost";

  public static int port = 8080;

  def static RequestBuilder expect() {
      return new RequestBuilder(baseUri: RestAssured.baseURI, path: "", port: port, method: GET)    
  }

  def static RequestBuilder with() {
      return given()
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
}