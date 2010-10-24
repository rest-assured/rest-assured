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
    get ("/hello").andAssertThat("hello", equalTo("Hello Scalatra"))
  }

  @Test
  public void ognlJSONAndHamcrestMatcher() throws Exception {
    get("/lotto").andAssertThat("lotto.lottoId", equalTo(5));
  }

  @Test
  public void ognlAndPlainGroovy() throws Exception {
    get ("/lotto").then {response, json -> assertEquals 5, json.lotto.lottoId }
  }

  @Test
  public void ognlReturningArrayAndPlainGroovy() throws Exception {
    get ("/lotto").then {response, json -> assertEquals([23, 54], json.lotto.winners.winnerId)}
  }
  
  @Test
  public void getWithQueryParameters() throws Exception {
    get ("/parameterHello").with().parameters([firstName: "John", lastName: "Doe"]).and().then { response, json -> assertEquals "John Doe", json.name }
  }
}
