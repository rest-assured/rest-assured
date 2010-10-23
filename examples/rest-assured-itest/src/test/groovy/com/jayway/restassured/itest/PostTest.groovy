package com.jayway.restassured.itest

import org.junit.Test
import static com.jayway.restassured.RestAssured.post
import static groovy.util.GroovyTestCase.assertEquals
import com.jayway.restassured.itest.support.WithJetty

class PostTest extends WithJetty {
  @Test
  public void test() throws Exception {
    post ("/hello").then {response, json -> assertEquals "Hello Scalatra", json.hello }
  }
}
