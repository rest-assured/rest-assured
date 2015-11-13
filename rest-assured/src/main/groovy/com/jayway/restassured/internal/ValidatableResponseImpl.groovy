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

import com.jayway.restassured.config.RestAssuredConfig
import com.jayway.restassured.internal.log.LogRepository
import com.jayway.restassured.response.ExtractableResponse
import com.jayway.restassured.response.Response
import com.jayway.restassured.response.ValidatableResponse
import org.hamcrest.Matcher

import java.util.concurrent.TimeUnit

class ValidatableResponseImpl extends ValidatableResponseOptionsImpl<ValidatableResponse, Response> implements ValidatableResponse {

  ValidatableResponseImpl(String contentType, ResponseParserRegistrar rpr, RestAssuredConfig config, Response response,
                          ExtractableResponse<Response> extractableResponse, LogRepository logRepository) {
    super(rpr, config, response, extractableResponse, logRepository)
  }

  Response originalResponse() {
    response
  }

  ValidatableResponse responseTime(Matcher<Long> matcher) {
    responseTime(matcher, TimeUnit.MILLISECONDS)
  }

  ValidatableResponse responseTime(Matcher<Long> matcher, TimeUnit timeUnit) {
    responseSpec.responseTime(matcher, timeUnit)
    this
  }
}
