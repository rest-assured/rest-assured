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

import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.internal.log.LogConfigRequestLoggingFilter;
import io.restassured.internal.log.LogRepository;
import io.restassured.module.mockmvc.specification.MockMvcRequestLogSpecification;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Sets a {@link LogConfigRequestLoggingFilter}, so what isn't given here is taken from the log config of the request when
 * it's logged
 */
public class MockMvcRequestLogSpecificationImpl implements MockMvcRequestLogSpecification {

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
        return logWith(LogDetail.BODY);
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
        return logWith(LogDetail.ALL);
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
        requestSpecification.setRequestLoggingFilter(ifValidationFailsFilter(logDetail, null, requestSpecification.getLogRepository()));
        return requestSpecification;
    }

    public MockMvcRequestSpecification ifValidationFails(LogDetail logDetail, boolean shouldPrettyPrint) {
        requestSpecification.setRequestLoggingFilter(ifValidationFailsFilter(logDetail, shouldPrettyPrint, requestSpecification.getLogRepository()));
        return requestSpecification;
    }

    /**
     * @param shouldPrettyPrint <code>null</code> to take it from the log config of the request
     * @return A filter that logs the request to a stream of <code>logRepository</code>, which is printed if validation fails
     */
    static RequestLoggingFilter ifValidationFailsFilter(LogDetail logDetail, Boolean shouldPrettyPrint, LogRepository logRepository) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        logRepository.registerRequestLog(baos);
        return new LogConfigRequestLoggingFilter(logDetail, shouldPrettyPrint, new PrintStream(baos));
    }

    private MockMvcRequestSpecification logWith(LogDetail logDetail) {
        return logWith(logDetail, null);
    }

    private MockMvcRequestSpecification logWith(LogDetail logDetail, Boolean shouldPrettyPrint) {
        requestSpecification.setRequestLoggingFilter(new LogConfigRequestLoggingFilter(logDetail, shouldPrettyPrint, null));
        return requestSpecification;
    }
}
