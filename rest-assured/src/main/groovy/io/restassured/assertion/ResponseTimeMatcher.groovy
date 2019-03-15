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

import io.restassured.response.Response
import org.hamcrest.Matcher

import java.util.concurrent.TimeUnit

class ResponseTimeMatcher {

  Matcher<Long> matcher
  TimeUnit timeUnit

  def validate(Response response) {
    def errorMessage = ""
    def success = true

    def time = response.getTimeIn(timeUnit)
    if (time <= -1) {
      errorMessage = "No time was recorded, cannot perform response time validation."
      success = false
    } else if (!matcher.matches(time)) {
      def timeMillis = response.getTime()
      success = false
      errorMessage = "Expected response time was not $matcher ${timeUnit.toString().toLowerCase()}, was $timeMillis milliseconds ($time ${timeUnit.toString().toLowerCase()})."
    }

    return [success: success, errorMessage: errorMessage]
  }
}
