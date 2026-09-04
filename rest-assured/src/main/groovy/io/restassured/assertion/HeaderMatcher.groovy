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

package io.restassured.assertion

import io.restassured.internal.util.SafeExceptionRethrower
import io.restassured.matcher.ResponseAwareMatcher
import org.hamcrest.Matcher

class HeaderMatcher {

  def headerName
  def mappingFunction
  Matcher matcher
  ResponseAwareMatcher responseAwareMatcher

  def validateHeader(response) {
    def headers = response.getHeaders()
    def success = true
    def message = ""
    def value = headers.getValue(headerName)
    def effectiveMatcher = matcher
    if (responseAwareMatcher != null) {
      try {
        effectiveMatcher = responseAwareMatcher.matcher(response)
      } catch (Exception e) {
        return SafeExceptionRethrower.safeRethrow(e)
      }
    }
    if (mappingFunction != null) {
      value = mappingFunction.apply(value)
    }
    if (!effectiveMatcher.matches(value)) {
      def headersString = headers.toString()
      success = false
      message = "Expected header \"$headerName\" was not $effectiveMatcher, was \"$value\". Headers are:\n$headersString\n"
    }
    [success: success, errorMessage: message]
  }
}
