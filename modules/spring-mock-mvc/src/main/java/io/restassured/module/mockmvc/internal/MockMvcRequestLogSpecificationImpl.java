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
package io.restassured.module.mockmvc.internal;

import io.restassured.authentication.NoAuthScheme;
import io.restassured.config.LogConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.internal.LogSpecificationImpl;
import io.restassured.internal.RequestSpecificationImpl;
import io.restassured.internal.log.LogRepository;
import io.restassured.module.mockmvc.specification.MockMvcRequestLogSpecification;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Set;

public class MockMvcRequestLogSpecificationImpl extends LogSpecificationImpl implements MockMvcRequestLogSpecification {
    private MockMvcRequestSpecificationImpl requestSpecification;

    public MockMvcRequestLogSpecificationImpl(MockMvcRequestSpecificationImpl requestSpecification) {
        this.requestSpecification = requestSpecification;
    }

    public MockMvcRequestSpecification params() {
        return logWith(LogDetail.PARAMS);
    }

    public MockMvcRequestSpecification parameters() {
        return logWith(LogDetail.PARAMS);
    }

    public MockMvcRequestSpecification body() {
        return body(shouldPrettyPrint(toRequestSpecification()));
    }

    public MockMvcRequestSpecification body(boolean shouldPrettyPrint) {
        return logWith(LogDetail.BODY, shouldPrettyPrint);
    }

    public MockMvcRequestSpecification all(boolean shouldPrettyPrint) {
        return logWith(LogDetail.ALL, shouldPrettyPrint);
    }

    public MockMvcRequestSpecification everything(boolean shouldPrettyPrint) {
        return all(shouldPrettyPrint);
    }

    public MockMvcRequestSpecification all() {
        return all(shouldPrettyPrint(toRequestSpecification()));
    }

    public MockMvcRequestSpecification everything() {
        return all();
    }

    public MockMvcRequestSpecification headers() {
        return logWith(LogDetail.HEADERS);
    }

    public MockMvcRequestSpecification cookies() {
        return logWith(LogDetail.COOKIES);
    }

    public MockMvcRequestSpecification ifValidationFails() {
        return ifValidationFails(LogDetail.ALL);
    }

    public MockMvcRequestSpecification ifValidationFails(LogDetail logDetail) {
        return ifValidationFails(logDetail, shouldPrettyPrint(toRequestSpecification()));
    }

    public MockMvcRequestSpecification ifValidationFails(LogDetail logDetail, boolean shouldPrettyPrint) {
        requestSpecification.setRequestLoggingFilter(ifValidationFailsFilter(logDetail, shouldPrettyPrint,
                requestSpecification.getRestAssuredMockMvcConfig().getLogConfig(), requestSpecification.getLogRepository()));
        return requestSpecification;
    }

    /**
     * @return A filter that logs the request to a stream of <code>logRepository</code>, which is printed if validation fails
     */
    static RequestLoggingFilter ifValidationFailsFilter(LogDetail logDetail, boolean shouldPrettyPrint, LogConfig logConfig, LogRepository logRepository) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        logRepository.registerRequestLog(baos);
        return new RequestLoggingFilter(logDetail, shouldPrettyPrint, new PrintStream(baos), logConfig.shouldUrlEncodeRequestUri(), logConfig.blacklistedHeaders());
    }

    private MockMvcRequestSpecification logWith(LogDetail logDetail) {
        RequestSpecificationImpl reqSpec = toRequestSpecification();
        return logWith(logDetail, shouldPrettyPrint(reqSpec));
    }

    private MockMvcRequestSpecification logWith(LogDetail logDetail, boolean prettyPrintingEnabled) {
        return logWith(logDetail, prettyPrintingEnabled, getPrintStream(toRequestSpecification()));
    }

    private MockMvcRequestSpecification logWith(LogDetail logDetail, boolean prettyPrintingEnabled, PrintStream printStream) {
        LogConfig logConfig = requestSpecification.getRestAssuredMockMvcConfig().getLogConfig();
        boolean shouldUrlEncodeRequestUri = logConfig.shouldUrlEncodeRequestUri();
        Set<String> blacklistedHeaders = logConfig.blacklistedHeaders();
        requestSpecification.setRequestLoggingFilter(new RequestLoggingFilter(logDetail, prettyPrintingEnabled, printStream, shouldUrlEncodeRequestUri, blacklistedHeaders));
        return requestSpecification;
    }

    private RequestSpecificationImpl toRequestSpecification() {
        return new RequestSpecificationImpl("", 8080, "", new NoAuthScheme(), Collections.<Filter>emptyList(), null, true, requestSpecification.getRestAssuredConfig(), requestSpecification.getLogRepository(), null, true, true);
    }
}
