/*
 * Copyright 2011 the original author or authors.
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
import com.jayway.restassured.response.Cookies;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class CookieITest extends WithJetty {

    @Test
    public void test() throws Exception {
        final Map<String,String> cookies = get("/springCookie").cookies();

        System.out.println(cookies);
    }

    @Test
    public void test2() throws Exception {
        final Cookies cookies = get("/springCookie").detailedCookies();

        System.out.println(cookies);
    }

    @Test
    public void test3() throws Exception {
        expect().cookie("cookie1", equalTo("cookieValue1")).when().get("/springCookie");
    }

    public void supportsCookieStringMatching() throws Exception {
        expect().response().cookie("key1", "value1").when().get("/setCookies");
    }

    @Test
    public void multipleCookieStatementsAreConcatenated() throws Exception {
        expect().response().cookie("key1", "value1").and().cookie("key2", "value2").when().get("/setCookies");
    }

    @Test
    public void multipleCookiesShortVersionUsingPlainStrings() throws Exception {
        expect().response().cookies("key1", "value1", "key3", "value3").when().get("/setCookies");
    }

    @Test
    public void multipleCookiesShortVersionUsingHamcrestMatching() throws Exception {
        expect().response().cookies("key2", containsString("2"), "key3", equalTo("value3")).when().get("/setCookies");
    }

    @Test
    public void multipleCookiesShortVersionUsingMixOfHamcrestMatchingAndStringMatching() throws Exception {
        expect().response().cookies("key1", containsString("1"), "key2", "value2").when().get("/setCookies");
    }

    @Test
    public void multipleCookiesUsingMap() throws Exception {
        Map expectedCookies = new HashMap();
        expectedCookies.put("key1", "value1");
        expectedCookies.put("key2", "value2");

        expect().response().cookies(expectedCookies).when().get("/setCookies");
    }

    @Test
    public void multipleCookiesUsingMapWithHamcrestMatcher() throws Exception {
        Map expectedCookies = new HashMap();
        expectedCookies.put("key1", containsString("1"));
        expectedCookies.put("key3", equalTo("value3"));

        expect().response().cookies(expectedCookies).when().get("/setCookies");
    }

    @Test
    public void multipleCookiesUsingMapWithMixOfStringAndHamcrestMatcher() throws Exception {
        Map expectedCookies = new HashMap();
        expectedCookies.put("key1", containsString("1"));
        expectedCookies.put("key2", "value2");

        expect().response().cookies(expectedCookies).when().get("/setCookies");
    }

    @Test
    public void whenExpectedCookieDoesntMatchAnAssertionThenAssertionErrorIsThrown() throws Exception {
        exception.expect(AssertionError.class);
        exception.expectMessage(equalTo("Expected cookie \"key1\" was not \"value2\", was \"value1\"."));

        expect().response().cookie("key1", "value2").when().get("/setCookies");
    }

    @Test
    public void whenExpectedCookieIsNotFoundThenAnAssertionErrorIsThrown() throws Exception {
        exception.expect(AssertionError.class);
        exception.expectMessage(equalTo("Cookie \"Not-Defined\" was not defined in the response. Cookies are: \n" +
                "key1=value1\n" +
                "key2=value2\n" +
                "key3=value3"));

        expect().response().cookie("Not-Defined", "something").when().get("/setCookies");
    }

}
