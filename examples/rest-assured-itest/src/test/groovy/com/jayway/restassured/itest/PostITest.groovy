package com.jayway.restassured.itest

import org.junit.Test
import static com.jayway.restassured.RestAssured.post
import static groovy.util.GroovyTestCase.assertEquals
import com.jayway.restassured.itest.support.WithJetty
import org.junit.BeforeClass

class PostITest extends WithJetty {

  @Test
  public void simplePost() throws Exception {
    post ("/hello").then {response, json -> assertEquals "Hello Scalatra", json.hello }
  }

  @Test
  public void simpleWithFormParameters() throws Exception {
    post ("/greet").with().parameters([firstName: "John", lastName: "Doe"]). then {response, json -> assertEquals "Greetings John Doe", json.greeting }
  }
}
