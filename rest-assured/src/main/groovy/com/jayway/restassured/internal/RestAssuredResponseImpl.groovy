/*
 * Copyright 2013 the original author or authors.
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
package com.jayway.restassured.internal

import com.jayway.restassured.response.Response
import com.jayway.restassured.response.ValidatableResponse

import java.util.concurrent.TimeUnit

class RestAssuredResponseImpl extends RestAssuredResponseOptionsImpl<Response> implements Response {

  public void parseResponse(httpResponse, content, hasBodyAssertions, ResponseParserRegistrar responseParserRegistrar) {
    groovyResponse.parseResponse(httpResponse, content, hasBodyAssertions, responseParserRegistrar);
  }

  ValidatableResponse then() {
    return new ValidatableResponseImpl(contentType, rpr, config, this, this, logRepository);
  }

  long time() {
    return groovyResponse.responseTime()
  }

  long timeIn(TimeUnit timeUnit) {
    return groovyResponse.responseTimeIn(timeUnit)
  }

  long getTime() {
    return groovyResponse.responseTime()
  }

  long getTimeIn(TimeUnit timeUnit) {
    return groovyResponse.responseTimeIn(timeUnit)
  }
}