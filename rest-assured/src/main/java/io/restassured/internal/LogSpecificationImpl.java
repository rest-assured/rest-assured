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


package io.restassured.internal;

import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.RequestSpecification;

import java.io.PrintStream;

/**
 * Base class for log specifications
 */
public class LogSpecificationImpl {

    public PrintStream getPrintStream(RequestSpecification requestSpecification) {
        LogConfig logConfig = getLogConfig(requestSpecification);
        PrintStream stream = logConfig == null ? null : logConfig.defaultStream();
        if (stream == null) {
            stream = System.out;
        }
        return stream;
    }

    /**
     * @return {@code false} when the request specification has no log config
     */
    public boolean shouldUrlEncodeRequestUri(RequestSpecification requestSpecification) {
        LogConfig logConfig = getLogConfig(requestSpecification);
        return logConfig != null && logConfig.shouldUrlEncodeRequestUri();
    }

    /**
     * @return {@code true} when the request specification has no log config
     */
    public boolean shouldPrettyPrint(RequestSpecification requestSpecification) {
        LogConfig logConfig = getLogConfig(requestSpecification);
        return logConfig == null || logConfig.isPrettyPrintingEnabled();
    }

    private LogConfig getLogConfig(RequestSpecification requestSpecification) {
        if (requestSpecification == null) {
            throw new IllegalStateException("Cannot configure logging since request specification is not defined. You may be misusing the API.");
        }
        // The config that was set on the request specification, which may be null
        RestAssuredConfig config = ((FilterableRequestSpecification) requestSpecification).getConfig();
        return config == null ? null : config.getLogConfig();
    }
}
