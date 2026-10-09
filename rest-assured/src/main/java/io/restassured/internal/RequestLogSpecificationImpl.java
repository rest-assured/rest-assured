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

import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.internal.log.LogRepository;
import io.restassured.specification.RequestLogSpecification;
import io.restassured.specification.RequestSpecification;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Set;

public class RequestLogSpecificationImpl extends LogSpecificationImpl implements RequestLogSpecification {
    private RequestSpecification requestSpecification;
    private LogRepository logRepository;
    private Set<String> blacklistedHeaders;

    @Override
    public RequestSpecification params() {
        return logWith(LogDetail.PARAMS);
    }

    @Override
    public RequestSpecification parameters() {
        return logWith(LogDetail.PARAMS);
    }

    @Override
    public RequestSpecification uri() {
        return logWith(LogDetail.URI);
    }

    @Override
    public RequestSpecification method() {
        return logWith(LogDetail.METHOD);
    }

    @Override
    public RequestSpecification body() {
        return body(shouldPrettyPrint(requestSpecification));
    }

    @Override
    public RequestSpecification body(boolean shouldPrettyPrint) {
        return logWith(LogDetail.BODY, shouldPrettyPrint);
    }

    @Override
    public RequestSpecification all(boolean shouldPrettyPrint) {
        return logWith(LogDetail.ALL, shouldPrettyPrint);
    }

    @Override
    public RequestSpecification everything(boolean shouldPrettyPrint) {
        return all(shouldPrettyPrint);
    }

    @Override
    public RequestSpecification all() {
        return all(shouldPrettyPrint(requestSpecification));
    }

    @Override
    public RequestSpecification everything() {
        return all();
    }

    @Override
    public RequestSpecification headers() {
        return logWith(LogDetail.HEADERS);
    }

    @Override
    public RequestSpecification cookies() {
        return logWith(LogDetail.COOKIES);
    }

    @Override
    public RequestSpecification ifValidationFails() {
        return ifValidationFails(LogDetail.ALL);
    }

    @Override
    public RequestSpecification ifValidationFails(LogDetail logDetail) {
        return ifValidationFails(logDetail, shouldPrettyPrint(requestSpecification));
    }

    @Override
    public RequestSpecification ifValidationFails(LogDetail logDetail, boolean shouldPrettyPrint) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        logRepository.registerRequestLog(baos);
        return logWith(logDetail, shouldPrettyPrint, ps);
    }

    public RequestSpecification getRequestSpecification() {
        return requestSpecification;
    }

    public void setRequestSpecification(RequestSpecification requestSpecification) {
        this.requestSpecification = requestSpecification;
    }

    public LogRepository getLogRepository() {
        return logRepository;
    }

    public void setLogRepository(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public Set<String> getBlacklistedHeaders() {
        return blacklistedHeaders;
    }

    public void setBlacklistedHeaders(Set<String> blacklistedHeaders) {
        this.blacklistedHeaders = blacklistedHeaders;
    }

    private RequestSpecification logWith(LogDetail logDetail) {
        return logWith(logDetail, shouldPrettyPrint(requestSpecification));
    }

    private RequestSpecification logWith(LogDetail logDetail, boolean prettyPrintingEnabled) {
        return logWith(logDetail, prettyPrintingEnabled, getPrintStream(requestSpecification));
    }

    private RequestSpecification logWith(LogDetail logDetail, boolean prettyPrintingEnabled, PrintStream printStream) {
        requestSpecification.filter(new RequestLoggingFilter(logDetail, prettyPrintingEnabled, printStream, shouldUrlEncodeRequestUri(requestSpecification), blacklistedHeaders));
        return requestSpecification;
    }
}
