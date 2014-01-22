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

package com.jayway.restassured.internal;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.filter.log.LogDetail;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.internal.print.ResponsePrinter;
import com.jayway.restassured.internal.util.SafeExceptionRethrower;
import com.jayway.restassured.matcher.ResponseAwareMatcher;
import com.jayway.restassured.parsing.Parser;
import com.jayway.restassured.response.*;
import com.jayway.restassured.specification.Argument;
import com.jayway.restassured.specification.ResponseSpecification;
import org.hamcrest.Matcher;

import java.util.List;
import java.util.Map;

import static com.jayway.restassured.internal.assertion.AssertParameter.notNull;

public abstract class ValidatableResponseOptionsImpl<T extends ValidatableResponseOptions<T, R>, R extends ResponseBody<R> & ResponseOptions<R>> implements ValidatableResponseLogSpec<T, R> {

    private final ResponseSpecificationImpl responseSpec;
    private final ExtractableResponse<R> extractableResponse;
    protected final Response response;
    private final RestAssuredConfig config;

    public ValidatableResponseOptionsImpl(String contentType, ResponseParserRegistrar rpr, RestAssuredConfig config, Response response, ExtractableResponse<R> extractableResponse) {
        this.config = config;
        this.response = response;
        responseSpec = new ResponseSpecificationImpl(RestAssured.rootPath, contentType, RestAssured.responseSpecification, rpr, config, response);
        this.extractableResponse = extractableResponse;
    }

    public T content(List<Argument> arguments, ResponseAwareMatcher<R> responseAwareMatcher) {
        return content(arguments, getMatcherFromResponseAwareMatcher(responseAwareMatcher));
    }

    public T body(List<Argument> arguments, ResponseAwareMatcher<R> responseAwareMatcher) {
        return body(arguments, getMatcherFromResponseAwareMatcher(responseAwareMatcher));
    }

    public T body(String key, List<Argument> arguments, ResponseAwareMatcher<R> responseAwareMatcher) {
        return body(key, arguments, getMatcherFromResponseAwareMatcher(responseAwareMatcher));
    }

    public T body(String key, ResponseAwareMatcher<R> responseAwareMatcher) {
        notNull(responseAwareMatcher, ResponseAwareMatcher.class);
        return body(key, getMatcherFromResponseAwareMatcher(responseAwareMatcher));
    }

    public T content(String path, List<Argument> arguments, ResponseAwareMatcher<R> responseAwareMatcher) {
        notNull(responseAwareMatcher, ResponseAwareMatcher.class);
        return content(path, arguments, getMatcherFromResponseAwareMatcher(responseAwareMatcher));
    }

    public T content(String path, ResponseAwareMatcher<R> responseAwareMatcher) {
        notNull(responseAwareMatcher, ResponseAwareMatcher.class);
        return content(path, getMatcherFromResponseAwareMatcher(responseAwareMatcher));
    }

    public T content(Matcher<?> matcher, Matcher<?>... additionalMatchers) {
        responseSpec.content(matcher, additionalMatchers);
        return (T) this;
    }

    public T content(List<Argument> arguments, Matcher matcher, Object... additionalKeyMatcherPairs) {
        responseSpec.content(arguments, matcher, additionalKeyMatcherPairs);
        return (T) this;
    }

    public T content(String key, Matcher<?> matcher, Object... additionalKeyMatcherPairs) {
        responseSpec.content(key, matcher, additionalKeyMatcherPairs);
        return (T) this;
    }

    public T body(String path, List<Argument> arguments, Matcher matcher, Object... additionalKeyMatcherPairs) {
        responseSpec.body(path, arguments, matcher, additionalKeyMatcherPairs);
        return (T) this;
    }

    public T body(List<Argument> arguments, Matcher matcher, Object... additionalKeyMatcherPairs) {
        responseSpec.body(arguments, matcher, additionalKeyMatcherPairs);
        return (T) this;
    }

    public T statusCode(Matcher<? super Integer> expectedStatusCode) {
        responseSpec.statusCode(expectedStatusCode);
        return (T) this;
    }

    public T statusCode(int expectedStatusCode) {
        responseSpec.statusCode(expectedStatusCode);
        return (T) this;
    }

    public T statusLine(Matcher<? super String> expectedStatusLine) {
        responseSpec.statusLine(expectedStatusLine);
        return (T) this;
    }

    public T statusLine(String expectedStatusLine) {
        responseSpec.statusLine(expectedStatusLine);
        return (T) this;
    }

    public T headers(Map<String, ?> expectedHeaders) {
        responseSpec.headers(expectedHeaders);
        return (T) this;
    }

    public T headers(String firstExpectedHeaderName, Object firstExpectedHeaderValue, Object... expectedHeaders) {
        responseSpec.headers(firstExpectedHeaderName, firstExpectedHeaderValue, expectedHeaders);
        return (T) this;
    }

    public T header(String headerName, Matcher<?> expectedValueMatcher) {
        responseSpec.header(headerName, expectedValueMatcher);
        return (T) this;
    }


    public T header(String headerName, String expectedValue) {
        responseSpec.header(headerName, expectedValue);
        return (T) this;
    }


    public T cookies(Map<String, ?> expectedCookies) {
        responseSpec.cookies(expectedCookies);
        return (T) this;
    }


    public T cookie(String cookieName) {
        responseSpec.cookie(cookieName);
        return (T) this;
    }


    public T cookies(String firstExpectedCookieName, Object firstExpectedCookieValue, Object... expectedCookieNameValuePairs) {
        responseSpec.cookies(firstExpectedCookieName, firstExpectedCookieValue, expectedCookieNameValuePairs);
        return (T) this;
    }


    public T cookie(String cookieName, Matcher<?> expectedValueMatcher) {
        responseSpec.cookie(cookieName, expectedValueMatcher);
        return (T) this;
    }


    public T cookie(String cookieName, Object expectedValue) {
        responseSpec.cookie(cookieName, expectedValue);
        return (T) this;
    }


    public T rootPath(String rootPath) {
        responseSpec.rootPath(rootPath);
        return (T) this;
    }


    public T rootPath(String rootPath, List<Argument> arguments) {
        responseSpec.rootPath(rootPath, arguments);
        return (T) this;
    }


    public T root(String rootPath, List<Argument> arguments) {
        responseSpec.root(rootPath, arguments);
        return (T) this;
    }


    public T root(String rootPath) {
        responseSpec.root(rootPath);
        return (T) this;
    }


    public T noRoot() {
        responseSpec.noRoot();
        return (T) this;
    }


    public T noRootPath() {
        responseSpec.noRootPath();
        return (T) this;
    }


    public T appendRoot(String pathToAppend) {
        responseSpec.appendRoot(pathToAppend);
        return (T) this;
    }


    public T appendRoot(String pathToAppend, List<Argument> arguments) {
        responseSpec.appendRoot(pathToAppend, arguments);
        return (T) this;
    }


    public T contentType(ContentType contentType) {
        responseSpec.contentType(contentType);
        return (T) this;
    }


    public T contentType(String contentType) {
        responseSpec.contentType(contentType);
        return (T) this;
    }


    public T contentType(Matcher<? super String> contentType) {
        responseSpec.contentType(contentType);
        return (T) this;
    }


    public T body(Matcher<?> matcher, Matcher<?>... additionalMatchers) {
        responseSpec.body(matcher, additionalMatchers);
        return (T) this;
    }


    public T body(String path, Matcher<?> matcher, Object... additionalKeyMatcherPairs) {
        responseSpec.body(path, matcher, additionalKeyMatcherPairs);
        return (T) this;
    }


    public T content(String path, List<Argument> arguments, Matcher matcher, Object... additionalKeyMatcherPairs) {
        responseSpec.content(path, arguments, matcher, additionalKeyMatcherPairs);
        return (T) this;
    }


    public T and() {
        return (T) this;
    }

    public T using() {
        return (T) this;
    }

    public T assertThat() {
        return (T) this;
    }

    public T spec(ResponseSpecification responseSpecification) {
        return specification(responseSpecification);
    }

    public T specification(ResponseSpecification responseSpecification) {
        notNull(responseSpecification, ResponseSpecification.class);
        // We parse the response as a string here because we need to enforce it otherwise specs won't work
        response.asString();
        responseSpecification.validate(response);
        return (T) this;
    }

    public T parser(String contentType, Parser parser) {
        responseSpec.parser(contentType, parser);
        return (T) this;
    }


    public T defaultParser(Parser parser) {
        responseSpec.defaultParser(parser);
        return (T) this;
    }

    public ExtractableResponse<R> extract() {
        return extractableResponse;
    }

    public ValidatableResponseLogSpec log() {
        return this;
    }

    public T status() {
        return logResponse(LogDetail.STATUS);
    }

    public T ifError() {
        if (response.statusCode() >= 400) {
            return logResponse(LogDetail.ALL);
        }
        return (T) this;
    }

    public T ifStatusCodeIsEqualTo(int statusCode) {
        if (response.statusCode() == statusCode) {
            return logResponse(LogDetail.ALL);
        }
        return (T) this;
    }

    public T ifStatusCodeMatches(Matcher<Integer> matcher) {
        notNull(matcher, "Matcher");
        if (matcher.matches(response.statusCode())) {
            return logResponse(LogDetail.ALL);
        }
        return (T) this;
    }

    public T body() {
        return logResponse(LogDetail.BODY);
    }


    public T body(boolean shouldPrettyPrint) {
        return logResponse(LogDetail.BODY, shouldPrettyPrint);
    }


    public T all() {
        return logResponse(LogDetail.ALL);
    }


    public T all(boolean shouldPrettyPrint) {
        return logResponse(LogDetail.ALL, shouldPrettyPrint);
    }


    public T everything() {
        return all();
    }


    public T everything(boolean shouldPrettyPrint) {
        return all(shouldPrettyPrint);
    }


    public T headers() {
        return logResponse(LogDetail.HEADERS);
    }


    public T cookies() {
        return logResponse(LogDetail.COOKIES);
    }


    private T logResponse(LogDetail logDetail) {
        return logResponse(logDetail, config.getLogConfig().isPrettyPrintingEnabled());
    }

    private T logResponse(LogDetail logDetail, boolean shouldPrettyPrint) {
        ResponsePrinter.print(response, response, config.getLogConfig().defaultStream(), logDetail, shouldPrettyPrint);
        return (T) this;
    }

    public abstract R originalResponse();

    private Matcher<?> getMatcherFromResponseAwareMatcher(ResponseAwareMatcher<R> responseAwareMatcher) {
        notNull(responseAwareMatcher, ResponseAwareMatcher.class);
        try {
            return responseAwareMatcher.matcher(originalResponse());
        } catch (Exception e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }

}
