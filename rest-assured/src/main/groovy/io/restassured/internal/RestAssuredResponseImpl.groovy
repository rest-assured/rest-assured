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
package io.restassured.internal

import io.restassured.response.Response
import io.restassured.response.ValidatableResponse

class RestAssuredResponseImpl extends RestAssuredResponseOptionsImpl<Response> implements Response {

  void parseResponse(httpResponse, content, hasBodyAssertions, ResponseParserRegistrar responseParserRegistrar) {
    groovyResponse.parseResponse(httpResponse, content, hasBodyAssertions, responseParserRegistrar);
  }

  // Unfortunately this is needed to make compilation from Maven happy
  @Override
  Response prettyPeek() {
    super.prettyPeek()
  }

  // Unfortunately this is needed to make compilation from Maven happy
  @Override
  Response peek() {
    super.peek()
  }

  @Override
  Response thenReturn() {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.thenReturn()
  }

  @Override
  Response andReturn() {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.andReturn()
  }

  ValidatableResponse then() {
    return new ValidatableResponseImpl(contentType, rpr, config, this, this, logRepository);
  }
}