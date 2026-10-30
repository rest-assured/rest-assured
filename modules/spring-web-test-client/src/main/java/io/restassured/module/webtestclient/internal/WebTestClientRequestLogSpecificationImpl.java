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

import io.restassured.authentication.NoAuthScheme;
import io.restassured.config.LogConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.internal.LogSpecificationImpl;
import io.restassured.internal.RequestSpecificationImpl;
import io.restassured.internal.log.LogRepository;
import io.restassured.module.webtestclient.specification.WebTestClientRequestLogSpecification;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSpecification;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Set;

public class WebTestClientRequestLogSpecificationImpl extends LogSpecificationImpl implements WebTestClientRequestLogSpecification {

    private final WebTestClientRequestSpecificationImpl requestSpecification;

    public WebTestClientRequestLogSpecificationImpl(WebTestClientRequestSpecificationImpl requestSpecification) {
        this.requestSpecification = requestSpecification;
    }

    @Override
    public WebTestClientRequestSpecification body() {
        return body(shouldPrettyPrint(toRequestSpecification()));
    }

    @Override
    public WebTestClientRequestSpecification body(boolean shouldPrettyPrint) {
        return logWith(LogDetail.BODY, shouldPrettyPrint);
    }

    @Override
    public WebTestClientRequestSpecification all() {
        return all(shouldPrettyPrint(toRequestSpecification()));
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
        return ifValidationFails(logDetail, shouldPrettyPrint(toRequestSpecification()));
    }

    @Override
    public WebTestClientRequestSpecification ifValidationFails(LogDetail logDetail, boolean shouldPrettyPrint) {
        requestSpecification.setRequestLoggingFilter(ifValidationFailsFilter(logDetail, shouldPrettyPrint,
                requestSpecification.getRestAssuredWebTestClientConfig().getLogConfig(), requestSpecification.getLogRepository()));
        return requestSpecification;
    }

    /**
     * @return A filter that logs the request to a stream of <code>logRepository</code>, which is printed if validation fails
     */
    static RequestLoggingFilter ifValidationFailsFilter(LogDetail logDetail, boolean shouldPrettyPrint, LogConfig logConfig, LogRepository logRepository) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        logRepository.registerRequestLog(byteArrayOutputStream);
        return new RequestLoggingFilter(logDetail, shouldPrettyPrint, new PrintStream(byteArrayOutputStream), logConfig.shouldUrlEncodeRequestUri(),
                logConfig.blacklistedHeaders());
    }

    public WebTestClientRequestSpecification params() {
        return logWith(LogDetail.PARAMS);
    }

    private WebTestClientRequestSpecification logWith(LogDetail logDetail, boolean prettyPrintingEnabled, PrintStream printStream) {
        LogConfig logConfig = requestSpecification.getRestAssuredWebTestClientConfig().getLogConfig();
        boolean shouldUrlEncodeRequestUri = logConfig.shouldUrlEncodeRequestUri();
        Set<String> blacklistedHeaders = logConfig.blacklistedHeaders();
        requestSpecification.setRequestLoggingFilter(new RequestLoggingFilter(logDetail, prettyPrintingEnabled, printStream, shouldUrlEncodeRequestUri, blacklistedHeaders));
        return requestSpecification;
    }

    private WebTestClientRequestSpecification logWith(LogDetail logDetail) {
        RequestSpecificationImpl reqSpec = toRequestSpecification();
        return logWith(logDetail, shouldPrettyPrint(reqSpec));
    }

    private RequestSpecificationImpl toRequestSpecification() {
        return new RequestSpecificationImpl("", 8080, "", new NoAuthScheme(),
                Collections.<Filter>emptyList(), null, true,
                requestSpecification.getRestAssuredConfig(), requestSpecification.getLogRepository(), null, true, true
        );
    }

    private WebTestClientRequestSpecification logWith(LogDetail logDetail, boolean prettyPrintingEnabled) {
        return logWith(logDetail, prettyPrintingEnabled, getPrintStream(toRequestSpecification()));
    }
}