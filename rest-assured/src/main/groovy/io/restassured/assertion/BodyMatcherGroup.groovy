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

import io.restassured.config.RestAssuredConfig
import io.restassured.response.Response

class BodyMatcherGroup {
    private def bodyAssertions = []

    def leftShift(Object bodyMatcher) {
        if (bodyMatcher instanceof BodyMatcherGroup) {
            bodyMatcher.bodyAssertions.each {
                bodyAssertions << it
            }
        } else {
            bodyAssertions << bodyMatcher
        }
    }


    def reset() {
        bodyAssertions.clear()
    }

    def size() {
        bodyAssertions.size()
    }

    def List validate(Response response, contentParser, RestAssuredConfig config) {
        bodyAssertions.collect { it.validate(response, contentParser, config) }
    }

    public boolean containsMatchers() {
        !bodyAssertions.isEmpty()
    }

    def boolean requiresTextParsing() {
        bodyAssertions.any { matcher -> matcher.requiresTextParsing() }
    }

    def boolean requiresPathParsing() {
        bodyAssertions.any { matcher -> matcher.requiresPathParsing() }
    }
}
