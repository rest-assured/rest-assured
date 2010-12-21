package com.jayway.restassured


import com.jayway.restassured.internal.RequestSpecificationImpl
import com.jayway.restassured.specification.RequestSpecification
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import com.jayway.restassured.specification.ResponseSpecification
import com.jayway.restassured.internal.ResponseSpecificationImpl
import com.jayway.restassured.internal.TestSpecification

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

  def static void reset() {
    baseURI = defaultURI
    port = defaultPort
  }
}