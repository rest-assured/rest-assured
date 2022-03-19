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
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.internal.log.LogRepository;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseLogSpecification;
import io.restassured.specification.ResponseSpecification;
import org.hamcrest.Matcher;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

public class ResponseLogSpecificationImpl extends LogSpecificationImpl implements ResponseLogSpecification {
    private ResponseSpecification responseSpecification;
    private LogRepository logRepository;

    @Override
    public ResponseSpecification body() {
        return body(shouldPrettyPrint());
    }

    @Override
    public ResponseSpecification body(boolean shouldPrettyPrint) {
        return logWith(LogDetail.BODY, shouldPrettyPrint);
    }

    @Override
    public ResponseSpecification all() {
        return all(shouldPrettyPrint());
    }

    @Override
    public ResponseSpecification all(boolean shouldPrettyPrint) {
        return logWith(LogDetail.ALL, shouldPrettyPrint);
    }

    @Override
    public ResponseSpecification everything() {
        return all();
    }

    @Override
    public ResponseSpecification everything(boolean shouldPrettyPrint) {
        return all(shouldPrettyPrint);
    }

    @Override
    public ResponseSpecification headers() {
        return logWith(LogDetail.HEADERS);
    }

    @Override
    public ResponseSpecification cookies() {
        return logWith(LogDetail.COOKIES);
    }

    @Override
    public ResponseSpecification ifValidationFails() {
        return ifValidationFails(LogDetail.ALL);
    }

    @Override
    public ResponseSpecification ifValidationFails(LogDetail logDetail) {
        return ifValidationFails(logDetail, shouldPrettyPrint());
    }

    @Override
    public ResponseSpecification ifValidationFails(LogDetail logDetail, boolean shouldPrettyPrint) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        logRepository.registerResponseLog(baos);
        return logWith(logDetail, shouldPrettyPrint, ps);
    }

    @Override
    public ResponseSpecification status() {
        return logWith(LogDetail.STATUS);
    }

    @Override
    public ResponseSpecification ifError() {
        return logWith(new ResponseLoggingFilter(getPrintStream(), greaterThanOrEqualTo(400)));
    }

    @Override
    public ResponseSpecification ifStatusCodeIsEqualTo(int statusCode) {
        return logWith(new ResponseLoggingFilter(getPrintStream(), equalTo(statusCode)));
    }

    @Override
    public ResponseSpecification ifStatusCodeMatches(Matcher<Integer> matcher) {
        return logWith(new ResponseLoggingFilter(getPrintStream(), matcher));
    }

    private ResponseSpecification logWith(LogDetail logDetail) {
        return logWith(new ResponseLoggingFilter(logDetail, getPrintStream()));
    }

    private ResponseSpecification logWith(LogDetail logDetail, boolean prettyPrintingEnabled) {
        return logWith(logDetail, prettyPrintingEnabled, getPrintStream());
    }

    private ResponseSpecification logWith(LogDetail logDetail, boolean prettyPrintingEnabled, PrintStream printStream) {
        return logWith(new ResponseLoggingFilter(logDetail, prettyPrintingEnabled, printStream));
    }

    private ResponseSpecification logWith(ResponseLoggingFilter filter) {
        responseSpecification.request().filter(filter);
        return responseSpecification;
    }

    private PrintStream getPrintStream() {
        RequestSpecification requestSpecification = responseSpecification.request();
        if (requestSpecification == null) {
            throw new IllegalStateException("Cannot configure logging since request specification is not defined. You may be misusing the API.");
        }
        return super.getPrintStream(requestSpecification);
    }

    private boolean shouldPrettyPrint() {
        RequestSpecification responseSpecification = this.responseSpecification.request();
        if (responseSpecification == null) {
            throw new IllegalStateException("Cannot configure logging since response specification is not defined. You may be misusing the API.");
        }
        return super.shouldPrettyPrint(responseSpecification);
    }
}