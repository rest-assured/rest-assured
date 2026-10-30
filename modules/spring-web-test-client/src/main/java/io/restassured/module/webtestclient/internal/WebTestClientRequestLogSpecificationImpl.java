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
package io.restassured.module.webtestclient.internal;

import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.internal.log.LogConfigRequestLoggingFilter;
import io.restassured.internal.log.LogRepository;
import io.restassured.module.webtestclient.specification.WebTestClientRequestLogSpecification;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSpecification;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Sets a {@link LogConfigRequestLoggingFilter}, so what isn't given here is taken from the log config of the request when
 * it's logged
 */
public class WebTestClientRequestLogSpecificationImpl implements WebTestClientRequestLogSpecification {

    private final WebTestClientRequestSpecificationImpl requestSpecification;

    public WebTestClientRequestLogSpecificationImpl(WebTestClientRequestSpecificationImpl requestSpecification) {
        this.requestSpecification = requestSpecification;
    }

    @Override
    public WebTestClientRequestSpecification body() {
        return logWith(LogDetail.BODY);
    }

    @Override
    public WebTestClientRequestSpecification body(boolean shouldPrettyPrint) {
        return logWith(LogDetail.BODY, shouldPrettyPrint);
    }

    @Override
    public WebTestClientRequestSpecification all() {
        return logWith(LogDetail.ALL);
    }

    @Override
    public WebTestClientRequestSpecification all(boolean shouldPrettyPrint) {
        return logWith(LogDetail.ALL, shouldPrettyPrint);
    }

    @Override
    public WebTestClientRequestSpecification everything() {
        return all();
    }

    @Override
    public WebTestClientRequestSpecification everything(boolean shouldPrettyPrint) {
        return all(shouldPrettyPrint);
    }

    @Override
    public WebTestClientRequestSpecification headers() {
        return logWith(LogDetail.HEADERS);
    }

    @Override
    public WebTestClientRequestSpecification cookies() {
        return logWith(LogDetail.COOKIES);
    }

    @Override
    public WebTestClientRequestSpecification ifValidationFails() {
        return ifValidationFails(LogDetail.ALL);
    }

    @Override
    public WebTestClientRequestSpecification ifValidationFails(LogDetail logDetail) {
        requestSpecification.setRequestLoggingFilter(ifValidationFailsFilter(logDetail, null, requestSpecification.getLogRepository()));
        return requestSpecification;
    }

    @Override
    public WebTestClientRequestSpecification ifValidationFails(LogDetail logDetail, boolean shouldPrettyPrint) {
        requestSpecification.setRequestLoggingFilter(ifValidationFailsFilter(logDetail, shouldPrettyPrint, requestSpecification.getLogRepository()));
        return requestSpecification;
    }

    /**
     * @param shouldPrettyPrint <code>null</code> to take it from the log config of the request
     * @return A filter that logs the request to a stream of <code>logRepository</code>, which is printed if validation fails
     */
    static RequestLoggingFilter ifValidationFailsFilter(LogDetail logDetail, Boolean shouldPrettyPrint, LogRepository logRepository) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        logRepository.registerRequestLog(byteArrayOutputStream);
        return new LogConfigRequestLoggingFilter(logDetail, shouldPrettyPrint, new PrintStream(byteArrayOutputStream));
    }

    public WebTestClientRequestSpecification params() {
        return logWith(LogDetail.PARAMS);
    }

    private WebTestClientRequestSpecification logWith(LogDetail logDetail) {
        return logWith(logDetail, null);
    }

    private WebTestClientRequestSpecification logWith(LogDetail logDetail, Boolean shouldPrettyPrint) {
        requestSpecification.setRequestLoggingFilter(new LogConfigRequestLoggingFilter(logDetail, shouldPrettyPrint, null));
        return requestSpecification;
    }
}