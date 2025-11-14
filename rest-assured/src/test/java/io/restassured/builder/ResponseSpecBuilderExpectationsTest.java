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

package io.restassured.builder;

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.specification.ResponseSpecification;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static io.restassured.RestAssured.withArgs;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests assertions for ResponseSpec. For each method (type of assertion), there is following test data provided:
 * 1) assertion description
 * 2) response spec builder with tested assertion
 * 3) response mock that should pass assertion
 * 4) response mock that should fail assertion
 */
public class ResponseSpecBuilderExpectationsTest {

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(
                        "Content matcher",
                        when(responseMock().asString()).thenReturn("goodBody").getMock(),
                        when(responseMock().asString()).thenReturn("badBody").getMock(),
                        new ResponseSpecBuilder().expectBody(startsWith("good"))
                ),
                Arguments.of(
                        "Content matcher with path",
                        responseMockInJson("{\"name\": \"goodValue\"}"),
                        responseMockInJson("{\"name\": \"badValue\"}"),
                        new ResponseSpecBuilder().expectBody("name", startsWith("good"))
                ),
                Arguments.of(
                        "Content matcher with parametrized path",
                        responseMockInJson("{\"name\": [\"value1\", \"value2\"]}"),
                        responseMockInJson("{\"name\": [\"value3\", \"value4\"]}"),
                        new ResponseSpecBuilder().expectBody("name[%d]", withArgs(1), endsWith("2"))
                ),
                Arguments.of(
                        "Status code matcher",
                        when(responseMock().getStatusCode()).thenReturn(567).getMock(),
                        when(responseMock().getStatusCode()).thenReturn(765).getMock(),
                        new ResponseSpecBuilder().expectStatusCode(lessThan(600))
                ),
                Arguments.of(
                        "Status code value",
                        when(responseMock().getStatusCode()).thenReturn(567).getMock(),
                        when(responseMock().getStatusCode()).thenReturn(765).getMock(),
                        new ResponseSpecBuilder().expectStatusCode(567)
                ),
                Arguments.of(
                        "Status line matcher",
                        when(responseMock().getStatusLine()).thenReturn("HTTP/5.6 567 GOOD").getMock(),
                        when(responseMock().getStatusLine()).thenReturn("FTP/4.3 765 BAD").getMock(),
                        new ResponseSpecBuilder().expectStatusLine(containsString("GOOD"))
                ),
                Arguments.of(
                        "Status line value",
                        when(responseMock().getStatusLine()).thenReturn("HTTP/5.6 567 GOOD").getMock(),
                        when(responseMock().getStatusLine()).thenReturn("FTP/4.3 765 BAD").getMock(),
                        new ResponseSpecBuilder().expectStatusLine("HTTP/5.6 567 GOOD")
                ),
                Arguments.of(
                        "Headers map",
                        when(responseMock().getHeaders()).thenReturn(new Headers(new Header("header1", "header1Value"), new Header("header2", "header2Value"))).getMock(),
                        when(responseMock().getHeaders()).thenReturn(new Headers(new Header("header3", "header3Value"))).getMock(),
                        new ResponseSpecBuilder().expectHeaders(Map.of("header1", "header1Value"))
                ),
                Arguments.of(
                        "Header matcher",
                        when(responseMock().getHeaders()).thenReturn(new Headers(new Header("header1", "goodHeaderValue"))).getMock(),
                        when(responseMock().getHeaders()).thenReturn(new Headers(new Header("header1", "badHeaderValue"))).getMock(),
                        new ResponseSpecBuilder().expectHeader("header1", equalTo("goodHeaderValue"))
                ),
                Arguments.of(
                        "Header value",
                        when(responseMock().getHeaders()).thenReturn(new Headers(new Header("header1", "goodHeaderValue"))).getMock(),
                        when(responseMock().getHeaders()).thenReturn(new Headers(new Header("header1", "badHeaderValue"))).getMock(),
                        new ResponseSpecBuilder().expectHeader("header1", "goodHeaderValue")
                ),
                Arguments.of(
                        "Cookies map",
                        when(responseMock().getHeaders()).thenReturn(new Headers(new Header("Set-Cookie", "cookie1=cookie1Val"))).getMock(),
                        when(responseMock().getHeaders()).thenReturn(new Headers(new Header("Set-Cookie", "cookie1=cookie1BadVal"))).getMock(),
                        new ResponseSpecBuilder().expectCookies(Map.of("cookie1", "cookie1Val"))
                ),
                Arguments.of(
                        "Cookie matcher",
                        when(responseMock().getHeaders()).thenReturn(new Headers(new Header("Set-Cookie", "cookie1=cookie1GoodVal"))).getMock(),
                        when(responseMock().getHeaders()).thenReturn(new Headers(new Header("Set-Cookie", "cookie1=cookie1BadVal"))).getMock(),
                        new ResponseSpecBuilder().expectCookie("cookie1", containsString("GoodVal"))
                ),
                Arguments.of(
                        "Cookie value",
                        when(responseMock().getHeaders()).thenReturn(new Headers(new Header("Set-Cookie", "cookie1=cookie1Val"))).getMock(),
                        when(responseMock().getHeaders()).thenReturn(new Headers(new Header("Set-Cookie", "cookie1=cookie1BadVal"))).getMock(),
                        new ResponseSpecBuilder().expectCookie("cookie1", "cookie1Val")
                ),
                Arguments.of(
                        "Cookie presence",
                        when(responseMock().getHeaders()).thenReturn(new Headers(new Header("Set-Cookie", "cookie1=cookie1Val"))).getMock(),
                        when(responseMock().getHeaders()).thenReturn(new Headers(new Header("Set-Cookie", "cookie2=cookie2Val"))).getMock(),
                        new ResponseSpecBuilder().expectCookie("cookie1")
                ),
                Arguments.of(
                        "Response time matcher",
                        when(responseMock().getTimeIn(TimeUnit.MILLISECONDS)).thenReturn(4000L).getMock(),
                        when(responseMock().getTimeIn(TimeUnit.MILLISECONDS)).thenReturn(8000L).getMock(),
                        new ResponseSpecBuilder().expectResponseTime(lessThan(5000L))
                ),
                Arguments.of(
                        "Response time matcher with time unit",
                        when(responseMock().getTimeIn(TimeUnit.NANOSECONDS)).thenReturn(4000L).getMock(),
                        when(responseMock().getTimeIn(TimeUnit.NANOSECONDS)).thenReturn(8000L).getMock(),
                        new ResponseSpecBuilder().expectResponseTime(lessThan(5000L), TimeUnit.NANOSECONDS)
                ),
                Arguments.of(
                        "Content type object",
                        when(responseMock().getContentType()).thenReturn("text/xml").getMock(),
                        when(responseMock().getContentType()).thenReturn("application/json").getMock(),
                        new ResponseSpecBuilder().expectContentType(ContentType.XML)
                ),
                Arguments.of(
                        "Content type string",
                        when(responseMock().getContentType()).thenReturn("text/xml").getMock(),
                        when(responseMock().getContentType()).thenReturn("application/json").getMock(),
                        new ResponseSpecBuilder().expectContentType("text/xml")
                ),
                Arguments.of(
                        "Body matcher",
                        when(responseMock().asString()).thenReturn("goodBody").getMock(),
                        when(responseMock().asString()).thenReturn("badBody").getMock(),
                        new ResponseSpecBuilder().expectBody(containsString("good"))
                ),
                Arguments.of(
                        "Body matcher with path",
                        responseMockInJson("{\"name\": \"goodValue\"}"),
                        responseMockInJson("{\"name\": \"badValue\"}"),
                        new ResponseSpecBuilder().expectBody("name", startsWith("good"))
                ),
                Arguments.of(
                        "Body matcher with parametrized path",
                        responseMockInJson("{\"name\": [\"value1\", \"value2\"]}"),
                        responseMockInJson("{\"name\": [\"value3\", \"value4\"]}"),
                        new ResponseSpecBuilder().expectBody("name[%d]", withArgs(1), endsWith("2"))
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    static void validResponseShouldMatch(String description, Response matchingResponse, Response unmatchedResponse, ResponseSpecBuilder builder) {
        ResponseSpecification responseSpecification = builder.build();
        responseSpecification.validate(matchingResponse);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    static void invalidResponseShouldNotMatch(String description, Response matchingResponse, Response unmatchedResponse, ResponseSpecBuilder builder) {
        ResponseSpecification responseSpecification = builder.build();
        assertThatThrownBy(() -> responseSpecification.validate(unmatchedResponse))
                .isInstanceOf(AssertionError.class);
    }

    private static Response responseMock() {
        return mock(Response.class);
    }

    private static Response responseMockInJson(String body) {
        Response response = mock(Response.class);
        when(response.getContentType()).thenReturn("application/json");
        when(response.contentType()).thenReturn("application/json");
        when(response.asString()).thenReturn(body);
        when(response.asInputStream()).thenReturn(IOUtils.toInputStream(body));
        return response;
    }

}
