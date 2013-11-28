package com.jayway.restassured.internal

import com.jayway.restassured.RestAssured
import com.jayway.restassured.assertion.BodyMatcherGroup
import com.jayway.restassured.config.RestAssuredConfig
import com.jayway.restassured.http.ContentType
import com.jayway.restassured.parsing.Parser
import com.jayway.restassured.response.ExtractableResponse
import com.jayway.restassured.response.Response
import com.jayway.restassured.response.ValidatableResponse
import com.jayway.restassured.specification.Argument
import com.jayway.restassured.specification.ResponseSpecification
import org.hamcrest.Matcher

class ValidatableResponseImpl implements ValidatableResponse {

    private BodyMatcherGroup bodyMatchers = new BodyMatcherGroup()
    private ResponseSpecificationImpl responseSpec
    private ExtractableResponse extractableResponse

    ValidatableResponseImpl(String contentType, ResponseParserRegistrar rpr, RestAssuredConfig config, Response response, ExtractableResponse extractableResponse) {
        responseSpec = new ResponseSpecificationImpl(RestAssured.rootPath, contentType, RestAssured.responseSpecification, rpr, config, response)
        this.extractableResponse = extractableResponse
        this.bodyMatchers = bodyMatchers
    }

    ValidatableResponse content(Matcher<?> matcher, Matcher<?>... additionalMatchers) {
        responseSpec.content(matcher, additionalMatchers)
        this
    }

    ValidatableResponse content(List<Argument> arguments, Matcher matcher, Object... additionalKeyMatcherPairs) {
        responseSpec.content(arguments, matcher, additionalKeyMatcherPairs)
        this
    }

    ValidatableResponse content(String key, Matcher<?> matcher, Object... additionalKeyMatcherPairs) {
        responseSpec.content(key, matcher, additionalKeyMatcherPairs)
        this
    }

    ValidatableResponse body(String key, List<Argument> arguments, Matcher matcher, Object... additionalKeyMatcherPairs) {
        responseSpec.body(key, arguments, matcher, additionalKeyMatcherPairs)
        this
    }

    ValidatableResponse body(List<Argument> arguments, Matcher matcher, Object... additionalKeyMatcherPairs) {
        responseSpec.body(arguments, matcher, additionalKeyMatcherPairs)
        this
    }

    ValidatableResponse statusCode(Matcher<? super Integer> expectedStatusCode) {
        responseSpec.statusCode(expectedStatusCode)
        this
    }

    ValidatableResponse statusCode(int expectedStatusCode) {
        responseSpec.statusCode(expectedStatusCode)
        this
    }

    ValidatableResponse statusLine(Matcher<? super String> expectedStatusLine) {
        responseSpec.statusLine(expectedStatusLine)
        this
    }

    ValidatableResponse statusLine(String expectedStatusLine) {
        responseSpec.statusLine(expectedStatusLine)
        this
    }

    ValidatableResponse headers(Map<String, ?> expectedHeaders) {
        responseSpec.headers(expectedHeaders)
        this
    }

    ValidatableResponse headers(String firstExpectedHeaderName, Object firstExpectedHeaderValue, Object... expectedHeaders) {
        responseSpec.headers(firstExpectedHeaderName, firstExpectedHeaderValue, expectedHeaders)
        this
    }

    ValidatableResponse header(String headerName, Matcher<?> expectedValueMatcher) {
        responseSpec.header(headerName, expectedValueMatcher)
        this
    }


    ValidatableResponse header(String headerName, String expectedValue) {
        responseSpec.header(headerName, expectedValue)
        this
    }


    ValidatableResponse cookies(Map<String, ?> expectedCookies) {
        responseSpec.cookies(expectedCookies)
        this
    }


    ValidatableResponse cookie(String cookieName) {
        responseSpec.cookie(cookieName)
        this
    }


    ValidatableResponse cookies(String firstExpectedCookieName, Object firstExpectedCookieValue, Object... expectedCookieNameValuePairs) {
        responseSpec.cookies(firstExpectedCookieName, firstExpectedCookieValue, expectedCookieNameValuePairs)
        this
    }


    ValidatableResponse cookie(String cookieName, Matcher<?> expectedValueMatcher) {
        responseSpec.cookie(cookieName, expectedValueMatcher)
        this
    }


    ValidatableResponse cookie(String cookieName, Object expectedValue) {
        responseSpec.cookie(cookieName, expectedValue)
        this
    }



    ValidatableResponse rootPath(String rootPath) {
        responseSpec.rootPath(rootPath)
        this
    }


    ValidatableResponse rootPath(String rootPath, List<Argument> arguments) {
        responseSpec.rootPath(rootPath, arguments)
        this
    }


    ValidatableResponse root(String rootPath, List<Argument> arguments) {
        responseSpec.root(rootPath, arguments)
        this
    }


    ValidatableResponse root(String rootPath) {
        responseSpec.root(rootPath)
        this
    }


    ValidatableResponse noRoot() {
        responseSpec.noRoot()
        this
    }


    ValidatableResponse noRootPath() {
        responseSpec.noRootPath()
        this
    }


    ValidatableResponse appendRoot(String pathToAppend) {
        responseSpec.appendRoot(pathToAppend)
        this
    }


    ValidatableResponse appendRoot(String pathToAppend, List<Argument> arguments) {
        responseSpec.appendRoot(pathToAppend, arguments)
        this
    }


    ValidatableResponse contentType(ContentType contentType) {
        responseSpec.contentType(contentType)
        this
    }


    ValidatableResponse contentType(String contentType) {
        responseSpec.contentType(contentType)
        this
    }


    ValidatableResponse contentType(Matcher<? super String> contentType) {
        responseSpec.contentType(contentType)
        this
    }


    ValidatableResponse body(Matcher<?> matcher, Matcher<?>... additionalMatchers) {
        responseSpec.body(matcher, additionalMatchers)
        this
    }


    ValidatableResponse body(String path, Matcher<?> matcher, Object... additionalKeyMatcherPairs) {
        responseSpec.body(path, matcher, additionalKeyMatcherPairs)
        this
    }


    ValidatableResponse content(String path, List<Argument> arguments, Matcher matcher, Object... additionalKeyMatcherPairs) {
        responseSpec.content(path, arguments, matcher, additionalKeyMatcherPairs)
        this
    }


    ValidatableResponse and() {
        this
    }

    ValidatableResponse using() {
        this
    }

    ValidatableResponse assertThat() {
        this
    }


    ValidatableResponse spec(ResponseSpecification responseSpecificationToMerge) {
        responseSpec.spec(responseSpecificationToMerge)
        this
    }


    ValidatableResponse specification(ResponseSpecification responseSpecificationToMerge) {
        responseSpec.spec(responseSpecificationToMerge)
        this
    }


    ValidatableResponse parser(String contentType, Parser parser) {
        responseSpec.parser(contentType, parser)
        this
    }


    ValidatableResponse defaultParser(Parser parser) {
        responseSpec.defaultParser(parser)
        this
    }

    ExtractableResponse extract() {
        extractableResponse
    }
}
