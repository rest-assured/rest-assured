/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.restassured

import io.restassured.authentication.ExplicitNoAuthScheme
import io.restassured.builder.RequestSpecBuilder
import io.restassured.builder.ResponseSpecBuilder
import io.restassured.config.RestAssuredConfig
import io.restassured.filter.Filter
import io.restassured.filter.FilterContext
import io.restassured.filter.log.LogDetail
import io.restassured.http.ContentType
import io.restassured.internal.SpecificationMerger
import io.restassured.internal.filter.FormAuthFilter
import io.restassured.parsing.Parser
import io.restassured.response.Response
import io.restassured.specification.FilterableRequestSpecification
import io.restassured.specification.FilterableResponseSpecification
import io.restassured.specification.RequestSpecification
import org.junit.jupiter.api.Test

import static io.restassured.config.RedirectConfig.redirectConfig
import static io.restassured.config.RestAssuredConfig.newConfig
import static io.restassured.config.SessionConfig.DEFAULT_SESSION_ID_NAME
import static io.restassured.config.SessionConfig.sessionConfig
import static java.util.Arrays.asList
import static org.assertj.core.api.Assertions.assertThat
import static org.hamcrest.Matchers.equalTo

class SpecificationMergerTest {

  @Test
  void mergesCookies() {
    def merge = new ResponseSpecBuilder().expectCookie("first", "value1").build()
    def with = new ResponseSpecBuilder().expectCookie("second", "value2").build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.cookieAssertions.size()).isEqualTo(2)
  }

  @Test
  void mergesHeaders() {
    def merge = new ResponseSpecBuilder().expectHeader("first", "value1").build()
    def with = new ResponseSpecBuilder().expectHeader("second", "value2").build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.headerAssertions.size()).isEqualTo(2)
  }

  @Test
  void mergesBodyMatchers() {
    def merge = new ResponseSpecBuilder().expectBody("first", equalTo("value1")).build()
    def with = new ResponseSpecBuilder().expectBody("second", equalTo("value2")).build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.bodyMatchers.size()).isEqualTo(2)
  }

  @Test
  void mergesFilters() {
    def merge = new RequestSpecBuilder().addFilter(newFilter()).addFilter(newFilter()).build()
    def with = new RequestSpecBuilder().addFilter(newFilter()).addFilters(asList(newFilter(), newFilter())).build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.filters.size()).isEqualTo(5)
  }

  @Test
  void sameFilterNotAddedTwice() {
    Filter filter = newFilter()
    def merge = new RequestSpecBuilder().addFilter(filter).build()
    def with = new RequestSpecBuilder().addFilter(filter).build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.getDefinedFilters().size()).isEqualTo(1)
  }

  @Test
  void overwritesContentType() {
    def merge = new ResponseSpecBuilder().expectContentType(ContentType.ANY).build()
    def with = new ResponseSpecBuilder().expectContentType(ContentType.BINARY).build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.contentType).isEqualTo(ContentType.BINARY)
  }

  @Test
  void overwritesRootPath() {
    def merge = new ResponseSpecBuilder().rootPath("rootPath").build()
    def with = new ResponseSpecBuilder().rootPath("new.").build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.bodyRootPath).isEqualTo("new.")
  }

  @Test
  void overwritesStatusCode() {
    def merge = new ResponseSpecBuilder().expectStatusCode(200).build()
    def with = new ResponseSpecBuilder().expectStatusCode(400).build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.expectedStatusCode.matches(400)).isTrue()
  }

  @Test
  void overwritesStatusLine() {
    def merge = new ResponseSpecBuilder().expectStatusLine("something").build()
    def with = new ResponseSpecBuilder().expectStatusLine("something else").build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.expectedStatusLine.matches("something else")).isTrue()
  }

  @Test
  void overwritesUrlEncodingStatus() {
    def merge = new RequestSpecBuilder().setUrlEncodingEnabled(true).build()
    def with = new RequestSpecBuilder().setUrlEncodingEnabled(false).build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.urlEncodingEnabled).isFalse()
  }

  @Test
  void mergesMultiPartParams() {
    def merge = new RequestSpecBuilder().addMultiPart("controlName1", "fileName1", new byte[0]).build()
    def with = new RequestSpecBuilder().addMultiPart("controlName2", "fileName2", new byte[0]).build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.multiParts.size()).isEqualTo(2)
  }

  @Test
  void mergesResponseParsers() {
    def merge = new ResponseSpecBuilder().registerParser("some/xml", Parser.XML).build()
    def with = new ResponseSpecBuilder().registerParser("some/json", Parser.JSON).build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.rpr.hasCustomParser("some/xml")).isTrue()
    assertThat(merge.rpr.hasCustomParser("some/json")).isTrue()
  }

  @Test
  void overwritesDefaultParser() {
    def merge = new ResponseSpecBuilder().setDefaultParser(Parser.XML).build()
    def with = new ResponseSpecBuilder().setDefaultParser(Parser.JSON).build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.rpr.defaultParser).isEqualTo(Parser.JSON)
  }

  @Test
  void overwritesLogDetail() {
    def merge = new ResponseSpecBuilder().log(LogDetail.COOKIES).build()
    def with = new ResponseSpecBuilder().log(LogDetail.URI).build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.getLogDetail()).isEqualTo(LogDetail.URI)
  }

  @Test
  void authFiltersAreOverwritten() {
    def merge = new RequestSpecBuilder().addFilter(new FormAuthFilter()).build()
    def with = new RequestSpecBuilder().addFilter(new FormAuthFilter()).build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.filters.size()).isEqualTo(1)
  }

  @Test
  void authFiltersAreRemovedIfMergedSpecContainsExplicitNoAuth() {
    def merge = new RequestSpecBuilder().addFilter(new FormAuthFilter()).build()
    def with = new RequestSpecBuilder().setAuth(new ExplicitNoAuthScheme()).build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.filters.size()).isEqualTo(0)
  }

  @Test
  void restAssuredConfigurationIsOverwritten() {
    def merge = new RequestSpecBuilder().setConfig(new RestAssuredConfig()).build()
    def with = new RequestSpecBuilder().setConfig(newConfig().redirect(redirectConfig().allowCircularRedirects(false))).build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.restAssuredConfig.getRedirectConfig().allowsCircularRedirects()).isFalse()
  }

  @Test
  void mergesRequestCookies() {
    def merge = new RequestSpecBuilder().addCookie("first", "value1").build()
    def with = new RequestSpecBuilder().addCookie("second", "value2").build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.cookies.size()).isEqualTo(2)
  }

  @Test
  void overwritesSessionId() {
    def merge = new RequestSpecBuilder().setSessionId("value1").build()
    def with = new RequestSpecBuilder().setSessionId("value2").build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.cookies.get(DEFAULT_SESSION_ID_NAME).getValue()).isEqualTo("value2")
  }

  @Test
  void overwritesSessionIdWhenMergingMultipleTimes() {
    def merge = new RequestSpecBuilder().setSessionId("value1").build()
    def with1 = new RequestSpecBuilder().setSessionId("value2").build()
    def with2 = new RequestSpecBuilder().setSessionId("value3").build()
    SpecificationMerger.merge(merge, with1)
    assertThat(merge.cookies.get(DEFAULT_SESSION_ID_NAME).getValue()).isEqualTo("value2")
    SpecificationMerger.merge(merge, with2)
    assertThat(merge.cookies.get(DEFAULT_SESSION_ID_NAME).getValue()).isEqualTo("value3")
  }

  @Test
  void overwritesSessionIdWhenDefinedInConfig() {
    def merge = new RequestSpecBuilder().setConfig(newConfig().sessionConfig(sessionConfig().sessionIdName("ikk"))).setSessionId("ikk", "value1").build()
    def with = new RequestSpecBuilder().setConfig(newConfig().sessionConfig(sessionConfig().sessionIdName("ikk2"))).setSessionId("ikk2", "value2").build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.cookies.hasCookieWithName("ikk")).isFalse()
    assertThat(merge.cookies.get("ikk2").getValue()).isEqualTo("value2")
  }

  @Test
  void copiesSessionIdWhenFirstRequestSpecBuilderDoesntHaveASessionIdSpecified() {
    def merge = new RequestSpecBuilder().build()
    def with = new RequestSpecBuilder().setConfig(newConfig().sessionConfig(sessionConfig().sessionIdName("ikk2"))).setSessionId("ikk2", "value2").build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.cookies.get("ikk2").getValue()).isEqualTo("value2")
  }

  @Test
  void doesntOverwriteSessionIdFromMergingSpecWhenItDoesntHaveASessionIdSpecified() {
    def merge = new RequestSpecBuilder().setConfig(newConfig().sessionConfig(sessionConfig().sessionIdName("ikk2"))).setSessionId("ikk2", "value2").build()
    def with = new RequestSpecBuilder().build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.cookies.get("ikk2").getValue()).isEqualTo("value2")
  }

  @Test
  void mergeRequestSpecsOverrideBaseUri() {
    RequestSpecification merge = new RequestSpecBuilder().setBaseUri("http://www.exampleSpec.com").build()
    RequestSpecification with = new RequestSpecBuilder().setBaseUri("http://www.exampleSpec2.com").build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.getProperties().get("baseUri")).isEqualTo("http://www.exampleSpec2.com")
  }

  @Test
  void mergeRequestSpecsOverrideBasePath() {
    RequestSpecification merge = new RequestSpecBuilder().setBasePath("http://www.exampleSpec.com").build()
    RequestSpecification with = new RequestSpecBuilder().setBasePath("http://www.exampleSpec2.com").build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.getProperties().get("basePath")).isEqualTo("http://www.exampleSpec2.com")
  }

  @Test
  void mergeRequestSpecsOverrideProxySpecification() {
    RequestSpecification merge = new RequestSpecBuilder().setProxy("127.0.0.1").build()
    RequestSpecification with = new RequestSpecBuilder().setProxy("localhost").build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.proxySpecification.host).isEqualTo("localhost")
  }

  @Test
  void mergeRequestSpecsOverrideAllowContentType() {
    RequestSpecification merge = new RequestSpecBuilder().setContentType("content-type").build()
    RequestSpecification with = new RequestSpecBuilder().noContentType().build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.allowContentType).isFalse()
  }

  @Test
  void mergeRequestSpecsOverrideAddCsrfFilter() {
    RequestSpecification merge = new RequestSpecBuilder().build()
    RequestSpecification with = new RequestSpecBuilder().disableCsrf().build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.addCsrfFilter).isFalse()
  }

  @Test
  void mergeRequestSpecsOverrideContentTypeWhenDisallowContentTypeOnOriginal() {
    RequestSpecification merge = new RequestSpecBuilder().noContentType().build()
    RequestSpecification with = new RequestSpecBuilder().setContentType("content-type").build()
    SpecificationMerger.merge(merge, with)
    assertThat(merge.allowContentType).isTrue()
  }

  private Filter newFilter() {
    return new Filter() {
      @Override
      Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        return ctx.next(requestSpec, responseSpec)
      }
    }
  }
}
