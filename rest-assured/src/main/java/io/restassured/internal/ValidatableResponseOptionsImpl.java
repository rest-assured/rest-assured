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

import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.internal.log.LogRepository;
import io.restassured.internal.print.ResponsePrinter;
import io.restassured.internal.util.SafeExceptionRethrower;
import io.restassured.matcher.DetailedCookieMatcher;
import io.restassured.matcher.ResponseAwareMatcher;
import io.restassured.parsing.Parser;
import io.restassured.response.*;
import io.restassured.specification.Argument;
import io.restassured.specification.ResponseSpecification;
import org.hamcrest.Matcher;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;

@SuppressWarnings("unchecked")
public abstract class ValidatableResponseOptionsImpl<T extends ValidatableResponseOptions<T, R>, R extends ResponseBody<R> & ResponseOptions<R>> implements ValidatableResponseLogSpec<T, R> {

    public final ResponseSpecificationImpl responseSpec;
    private final ExtractableResponse<R> extractableResponse;
    protected final Response response;
    private final RestAssuredConfig config;

    public ValidatableResponseOptionsImpl(ResponseParserRegistrar rpr, RestAssuredConfig config, Response response,
                                          ExtractableResponse<R> extractableResponse, LogRepository logRepository) {
        this.config = config == null ? RestAssuredConfig.config() : config;
        this.response = response;
        responseSpec = new ResponseSpecificationImpl(RestAssured.rootPath, RestAssured.responseSpecification, rpr, this.config, response, logRepository);
        this.extractableResponse = extractableResponse;
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

    public T header(String headerName, ResponseAwareMatcher<R> r) {
        responseSpec.header(headerName, getMatcherFromResponseAwareMatcher(r));
        return (T) this;
    }

    public T header(String headerName, String expectedValue) {
        responseSpec.header(headerName, expectedValue);
        return (T) this;
    }

    public <U> T header(String headerName, Function<String, U> f, Matcher<? super U> matcher) {
        responseSpec.header(headerName, f, matcher);
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

    public T cookie(String cookieName, DetailedCookieMatcher detailedCookieMatcher) {
        responseSpec.cookie(cookieName, detailedCookieMatcher);
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
        responseSpec.rootPath(rootPath);
        return (T) this;
    }


    public T noRoot() {
        responseSpec.noRootPath();
        return (T) this;
    }


    public T noRootPath() {
        responseSpec.noRootPath();
        return (T) this;
    }


    public T appendRootPath(String pathToAppend) {
        responseSpec.appendRootPath(pathToAppend);
        return (T) this;
    }


    public T appendRootPath(String pathToAppend, List<Argument> arguments) {
        responseSpec.appendRootPath(pathToAppend, arguments);
        return (T) this;
    }

    public T detachRootPath(String pathToDetach) {
        responseSpec.detachRootPath(pathToDetach);
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
        responseSpec.body(path, arguments, matcher, additionalKeyMatcherPairs);
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
        // We parse the response as a string here because we need to enforce it otherwise specs won't work
        response.asString();

        // The following is a work-around to enable logging of request and response if validation fails
        if (responseSpecification instanceof ResponseSpecificationImpl) {
            ResponseSpecificationImpl impl = (ResponseSpecificationImpl) responseSpecification;
            LogConfig globalLogConfig = responseSpec.getConfig().getLogConfig();
            impl.setConfig(config);
            if (globalLogConfig.isLoggingOfRequestAndResponseIfValidationFailsEnabled()) {
                impl.setLogRepository(responseSpec.getLogRepository());
            }
            if (impl.getLogDetail() != null) {
                logResponse(impl.getLogDetail(), globalLogConfig.isPrettyPrintingEnabled(),
                        globalLogConfig.defaultStream());
            }

        }


        // Finally validate the response
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

    public T ifValidationFails() {
        return ifValidationFails(LogDetail.ALL);
    }

    public T ifValidationFails(LogDetail logDetail) {
        return ifValidationFails(logDetail, config.getLogConfig().isPrettyPrintingEnabled());
    }

    public T ifValidationFails(LogDetail logDetail, boolean shouldPrettyPrint) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        responseSpec.getLogRepository().registerResponseLog(baos);
        return logResponse(logDetail, shouldPrettyPrint, ps);
    }

    private T logResponse(LogDetail logDetail) {
        return logResponse(logDetail, config.getLogConfig().isPrettyPrintingEnabled());
    }

    private T logResponse(LogDetail logDetail, boolean shouldPrettyPrint) {
        return logResponse(logDetail, shouldPrettyPrint, config.getLogConfig().defaultStream());
    }

    private T logResponse(LogDetail logDetail, boolean shouldPrettyPrint, PrintStream printStream) {
        ResponsePrinter.print(response, response, printStream, logDetail, shouldPrettyPrint);
        return (T) this;
    }

    public T time(Matcher<Long> matcher) {
        return time(matcher, TimeUnit.MILLISECONDS);
    }

    public T time(Matcher<Long> matcher, TimeUnit timeUnit) {
        responseSpec.time(matcher, timeUnit);
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
