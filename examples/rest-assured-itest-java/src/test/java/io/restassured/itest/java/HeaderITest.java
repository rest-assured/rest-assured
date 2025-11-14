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

package io.restassured.itest.java;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.itest.java.support.WithJetty;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HeaderITest extends WithJetty {

    @Test
    public void requestSpecificationAllowsSpecifyingHeader() {
        given().header("MyHeader", "Something").and().expect().body(containsString("MyHeader")).when().get("/header");
    }

    @Test
    public void allowsSupplyingMappingFunction() {
        when().
                get("/hello").
        then().
                header("Content-Length", Integer::parseInt, lessThanOrEqualTo(200));
    }

    @Test
    public void headerExceptionCanFailWhenUsingMappingFunction() {
        assertThatThrownBy(() ->
            when().get("/hello").then().header("Content-Length", Integer::parseInt, greaterThan(200))
        )
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("Expected header \"Content-Length\" was not a value greater than <200>, was \"26\". Headers are:");
    }

    @Test
    public void allowsParsingMultiValueHeaders() {
        final List<String> headers = given().header("MyHeader", "Something", "Something else").when().get("/multiValueHeader").headers().getValues("MultiHeader");

        assertThat(headers, hasItems("Value 1", "Value 2"));
    }

    @Test
    public void orderIsMaintainedForMultiValueHeaders() {
        Headers headers = when().get("/multiValueHeader").headers();

        final List<String> headerListString = headers.getValues("MultiHeader");
        final String firstValue = headers.getValue("MultiHeader");
        final List<Header> headerListHeader = headers.getList("MultiHeader");

        assertThat(headerListString, contains("Value 1", "Value 2"));
        assertThat(headerListHeader, contains(new Header("MultiHeader", "Value 1"), new Header("MultiHeader", "Value 2")));
        assertThat(firstValue, equalTo("Value 2"));
    }

    @Test
    public void requestSpecificationAllowsSpecifyingMultiValueHeaders() {
        final List<String> myHeaderValues = given().header("MyHeader", "Something", "Something else").when().get("/multiHeaderReflect").headers().getValues("MyHeader");

        assertThat(myHeaderValues.size(), is(2));
        assertThat(myHeaderValues, hasItems("Something", "Something else"));
    }

    @Test
    public void responseSpecificationAllowsParsingMultiValueHeadersWithValuesIncludingEqualCharacter() {
        final List<String> myHeaderValues = given().header("MyHeader", "Some=thing", "Something=else=").when().get("/multiHeaderReflect").headers().getValues("MyHeader");

        assertThat(myHeaderValues.size(), is(2));
        assertThat(myHeaderValues, hasItems("Some=thing", "Something=else="));
    }

    @Test
    public void requestSpecificationAllowsSpecifyingHeadersObject() {
        final Header header1 = new Header("MyHeader", "Something");
        final Header header2 = new Header("MyHeader", "Something else");
        final Headers headers = new Headers(header1, header2);

        final List<String> myHeaderValues = given().headers(headers).when().get("/multiHeaderReflect").headers().getValues("MyHeader");

        assertThat(myHeaderValues.size(), is(2));
        assertThat(myHeaderValues, hasItems("Something", "Something else"));
    }

    @Test
    public void requestSpecificationAllowsSpecifyingMultipleHeadersInSequenceWhichGetsTreatedAsMultiHeaders() {
        final List<String> myHeaderValues =
                given().
                        header("MyHeader", "Something").
                        header("MyHeader", "Something else").
                when().
                        get("/multiHeaderReflect").
                then().
                        extract().headers().getValues("MyHeader");

        assertThat(myHeaderValues.size(), is(2));
        assertThat(myHeaderValues, hasItems("Something", "Something else"));
    }

    @Test
    public void requestSpecificationAllowsSpecifyingHeaders() {
        given().headers("MyHeader", "Something").and().expect().body(containsString("MyHeader")).when().get("/header");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingMultipleHeaders() {
        given().header("MyHeader", "Something").and().headers("MyHeader2", "Something else", "MyHeader3", "H").and().
                expect().body(containsString("MyHeader"), containsString("MyHeader2"), containsString("MyHeader3")).when()
                .get("/header");
    }

    @Test
    public void supportsHeaderStringMatching() {
        expect().response().header("Content-Type", "application/json;charset=utf-8").when().get("/lotto");
    }

    @Test
    public void multipleHeaderStatementsAreConcatenated() {
        expect().response().header("Content-Type", "application/json;charset=utf-8").and().header("Content-Length", "160").when().get("/lotto");
    }

    @Test
    public void multipleHeadersShortVersionUsingPlainStrings() {
        expect().response().headers("Content-Type", "application/json;charset=utf-8", "Content-Length", "160").when().get("/lotto");
    }

    @Test
    public void multipleHeadersShortVersionUsingHamcrestMatching() {
        expect().response().headers("Content-Type", containsString("application/json"), "Content-Length", equalTo("160")).when().get("/lotto");
    }

    @Test
    public void multipleHeadersShortVersionUsingMixOfHamcrestMatchingAndStringMatching() {
        expect().response().headers("Content-Type", containsString("application/json"), "Content-Length", "160").when().get("/lotto");
    }

    @Test
    public void multipleHeadersUsingMap() {
        Map<String, Object> expectedHeaders = new HashMap<>();
        expectedHeaders.put("Content-Type", "application/json;charset=utf-8");
        expectedHeaders.put("Content-Length", "160");

        expect().response().headers(expectedHeaders).when().get("/lotto");
    }

    @Test
    public void multipleHeadersUsingMapWithHamcrestMatcher() {
        Map<String, Object> expectedHeaders = new HashMap<>();
        expectedHeaders.put("Content-Type", containsString("application/json;charset=utf-8"));
        expectedHeaders.put("Content-Length", equalTo("160"));

        expect().response().headers(expectedHeaders).when().get("/lotto");
    }

    @Test
    public void multipleHeadersUsingMapWithMixOfStringAndHamcrestMatcher() {
        Map<String, Object> expectedHeaders = new HashMap<>();
        expectedHeaders.put("Content-Type", containsString("application/json;charset=utf-8"));
        expectedHeaders.put("Content-Length", "160");

        expect().response().headers(expectedHeaders).when().get("/lotto");
    }

    @Test
    public void whenExpectedHeaderDoesntMatchAnAssertionThenAssertionErrorIsThrown() {
        assertThatThrownBy(() ->
            expect().response().header("Content-Length", "161").when().get("/lotto")
        )
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("Expected header \"Content-Length\" was not \"161\", was \"160\". Headers are:");
    }

    @Test
    public void whenExpectedHeaderIsNotFoundThenAnAssertionErrorIsThrown() {
        assertThatThrownBy(() ->
                expect().response().header("Not-Defined", "160").when().get("/lotto")
        )
                .isInstanceOf(AssertionError.class)
                .hasMessage("""
                        1 expectation failed.
                        Expected header "Not-Defined" was not "160", was "null". Headers are:
                        Content-Type=application/json;charset=utf-8
                        Content-Length=160
                        Server=Jetty(9.4.34.v20201102)
                        """);
    }

    @Test
    public void whenMultiValueHeadersArePresentedInTheResponseThenTheLastValueHasPrecedence() {
        when().
                get("/multiValueHeader").
        then().
                header("MultiHeader", equalTo("Value 2"));
    }

    @Test
    public void canUseResponseAwareMatchersForHeaderValidation() {
        given().
                redirects().follow(false).
        when().
                post("/redirect").
        then().
                statusCode(301).
                header("Location", response -> endsWith("/redirect/"+response.path("id")));
    }
}
