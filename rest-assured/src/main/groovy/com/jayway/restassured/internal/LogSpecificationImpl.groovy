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

import com.jayway.restassured.config.LogConfig
import com.jayway.restassured.config.RestAssuredConfig
import com.jayway.restassured.specification.RequestSpecification

/**
 * Base class for log specifications
 */
class LogSpecificationImpl {

    def PrintStream getPrintStream(RequestSpecification requestSpecification) {
        def stream = getLogConfig(requestSpecification)?.defaultStream()
        if(stream == null) {
            stream = System.out
        }
        stream
    }

    def boolean shouldPrettyPrint(RequestSpecification requestSpecification) {
        def prettyPrintingEnabled = getLogConfig(requestSpecification)?.isPrettyPrintingEnabled()
        if(prettyPrintingEnabled == null) {
            return true
        }
        prettyPrintingEnabled
    }

    private def LogConfig getLogConfig(RequestSpecification requestSpecification) {
        if(!requestSpecification) {
            throw new IllegalStateException("Cannot configure logging since request specification is not defined. You may be misusing the API.");
        }
        RestAssuredConfig config = requestSpecification.restAssuredConfig
        config?.logConfig
    }
}
