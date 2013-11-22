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

package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Headers;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class HeaderITest extends WithJetty {

    @Test
    public void requestSpecificationAllowsSpecifyingHeader() throws Exception {
        given().header("MyHeader", "Something").and().expect().body(containsString("MyHeader")).when().get("/header");
    }

    @Test
    public void allowsParsingMultiValueHeaders() throws Exception {
        final List<String> headers = given().header("MyHeader", "Something", "Something else").when().get("/multiValueHeader").headers().getValues("MultiHeader");

        assertThat(headers, hasItems("Value 1", "Value 2"));
    }

    @Test
    public void requestSpecificationAllowsSpecifyingMultiValueHeaders() throws Exception {
        final List<String> myHeaderValues = given().header("MyHeader", "Something", "Something else").when().get("/multiHeaderReflect").headers().getValues("MyHeader");

        assertThat(myHeaderValues.size(), is(2));
        assertThat(myHeaderValues, hasItems("Something", "Something else"));
    }

    @Test
    public void responseSpecificationAllowsParsingMultiValueHeadersWithValuesIncludingEqualCharacter() throws Exception {
        final List<String> myHeaderValues = given().header("MyHeader", "Some=thing", "Something=else=").when().get("/multiHeaderReflect").headers().getValues("MyHeader");

        assertThat(myHeaderValues.size(), is(2));
        assertThat(myHeaderValues, hasItems("Some=thing", "Something=else="));
    }

    @Test
    public void requestSpecificationAllowsSpecifyingHeadersObject() throws Exception {
        final Header header1 = new Header("MyHeader", "Something");
        final Header header2 = new Header("MyHeader", "Something else");
        final Headers headers = new Headers(header1, header2);

        final List<String> myHeaderValues = given().headers(headers).when().get("/multiHeaderReflect").headers().getValues("MyHeader");

        assertThat(myHeaderValues.size(), is(2));
        assertThat(myHeaderValues, hasItems("Something", "Something else"));
    }

    @Test
    public void requestSpecificationAllowsSpecifyingHeaders() throws Exception {
        given().headers("MyHeader", "Something").and().expect().body(containsString("MyHeader")).when().get("/header");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingMultipleHeaders() throws Exception {
        given().header("MyHeader", "Something").and().headers("MyHeader2", "Something else", "MyHeader3", "H").and().
                expect().body(containsString("MyHeader"), containsString("MyHeader2"), containsString("MyHeader3")).when()
                .get("/header");
    }

    @Test
    public void supportsHeaderStringMatching() throws Exception {
        expect().response().header("Content-Type", "application/json; charset=UTF-8").when().get("/lotto");
    }

    @Test
    public void multipleHeaderStatementsAreConcatenated() throws Exception {
        expect().response().header("Content-Type", "application/json; charset=UTF-8").and().header("Content-Length", "160").when().get("/lotto");
    }

    @Test
    public void multipleHeadersShortVersionUsingPlainStrings() throws Exception {
        expect().response().headers("Content-Type", "application/json; charset=UTF-8", "Content-Length", "160").when().get("/lotto");
    }

    @Test
    public void multipleHeadersShortVersionUsingHamcrestMatching() throws Exception {
        expect().response().headers("Content-Type", containsString("application/json"), "Content-Length", equalTo("160")).when().get("/lotto");
    }

    @Test
    public void multipleHeadersShortVersionUsingMixOfHamcrestMatchingAndStringMatching() throws Exception {
        expect().response().headers("Content-Type", containsString("application/json"), "Content-Length", "160").when().get("/lotto");
    }

    @Test
    public void multipleHeadersUsingMap() throws Exception {
        Map expectedHeaders = new HashMap();
        expectedHeaders.put("Content-Type", "application/json; charset=UTF-8");
        expectedHeaders.put("Content-Length", "160");

        expect().response().headers(expectedHeaders).when().get("/lotto");
    }

    @Test
    public void multipleHeadersUsingMapWithHamcrestMatcher() throws Exception {
        Map expectedHeaders = new HashMap();
        expectedHeaders.put("Content-Type", containsString("application/json; charset=UTF-8"));
        expectedHeaders.put("Content-Length", equalTo("160"));

        expect().response().headers(expectedHeaders).when().get("/lotto");
    }

    @Test
    public void multipleHeadersUsingMapWithMixOfStringAndHamcrestMatcher() throws Exception {
        Map expectedHeaders = new HashMap();
        expectedHeaders.put("Content-Type", containsString("application/json; charset=UTF-8"));
        expectedHeaders.put("Content-Length", "160");

        expect().response().headers(expectedHeaders).when().get("/lotto");
    }

    @Test
    public void whenExpectedHeaderDoesntMatchAnAssertionThenAssertionErrorIsThrown() throws Exception {
        exception.expect(AssertionError.class);
        exception.expectMessage(containsString("Expected header \"Content-Length\" was not \"161\", was \"160\". Headers are:"));

        expect().response().header("Content-Length", "161").when().get("/lotto");
    }

    @Test
    public void whenExpectedHeaderIsNotFoundThenAnAssertionErrorIsThrown() throws Exception {
        exception.expect(AssertionError.class);
        exception.expectMessage(equalTo("1 expectation failed.\n" +
                "Expected header \"Not-Defined\" was not \"160\", was \"null\". Headers are:\n" +
                "Content-Type=application/json; charset=UTF-8\n" +
                "Content-Length=160\n" +
                "Server=Jetty(6.1.14)\n"));

        expect().response().header("Not-Defined", "160").when().get("/lotto");
    }

}
