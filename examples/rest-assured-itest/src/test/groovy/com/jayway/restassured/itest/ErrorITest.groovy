package com.jayway.restassured.itest

import com.jayway.restassured.itest.support.WithJetty
import org.junit.Test
import static com.jayway.restassured.RestAssured.get
import static groovy.util.GroovyTestCase.assertEquals
import static org.hamcrest.Matchers.equalTo

class ErrorITest extends WithJetty  {

  @Test
  public void error404() throws Exception {
    get ("/illegal").then {response -> assertEquals 404, response.statusLine.statusCode }
  }
}
