package com.jayway.restassured.itest

import com.jayway.restassured.itest.support.WithJetty
import org.junit.Ignore
import org.junit.Test
import static com.jayway.restassured.RestAssured.get
import static groovy.util.GroovyTestCase.assertEquals
import static org.hamcrest.Matchers.equalTo

class GetTest extends WithJetty  {
  @Test
  public void withoutParameters() throws Exception {
    get ("/hello").then {response, json -> assertEquals "Hello Scalatra", json.hello }
  }

  @Test
  public void getWithPathParams() throws Exception {
    get ("/John/Doe"). then { response, json -> assertEquals "John", json.firstName }
  }

  @Test
  public void usingHamcrestFromGroovy() throws Exception {
    get ("/hello").thenAssertThat("hello", equalTo("Hello Scalatra"))
  }


  @Test
  @Ignore
  public void getWithQueryParameters() throws Exception {
    get ("/hello").withParameters([key: "key", value: "value"]). then { response, json -> assertEquals "Hello Scalatra", json.hello }
  }
}
