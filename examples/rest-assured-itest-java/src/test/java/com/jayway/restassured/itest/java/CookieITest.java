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
import com.jayway.restassured.response.Cookie;
import com.jayway.restassured.response.Cookies;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class CookieITest extends WithJetty {

    @Test
    public void cookiesReturnsAMapWhereTheFirstValueOfAMultiValueCookieIsUsed() throws Exception {
        final Map<String,String> cookies = get("/multiCookie").cookies();

        assertThat(cookies, hasEntry("cookie1", "cookieValue1"));
    }

    @Test
    public void detailedCookiesAllowsToGetMultiValues() throws Exception {
        final Cookies cookies = get("/multiCookie").detailedCookies();

        assertThat(cookies.getValues("cookie1"), hasItems("cookieValue1", "cookieValue2"));
    }

    @Test
    public void whenUsingTheDslAndExpectingAMultiValueCookieThenTheFirstValueIsUsed() throws Exception {
        expect().cookie("cookie1", equalTo("cookieValue1")).when().get("/multiCookie");
    }

    @Test
    public void supportsCookieStringMatchingUsingTheDsl() throws Exception {
        expect().cookie("key1", "value1").when().get("/setCookies");
    }

    @Test
    public void canSpecifyMultiValueCookiesUsingByPassingInSeveralValuesToTheCookieMethod() throws Exception {
        final List<String> cookieValues = given().cookie("key1", "value1", "value2").when().post("/reflect").detailedCookies().getValues("key1");

        assertThat(cookieValues, hasItems("value1", "value2"));
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

    @Test
    public void requestSpecificationAllowsSpecifyingCookieWithNoValue() throws Exception {
        given().cookie("some_cookie").expect().body(equalTo("some_cookie")).when().get("/cookie_with_no_value");
    }

    @Test
    public void responseSpecificationAllowsParsingCookieWithNoValue() throws Exception {
        expect().cookie("PLAY_FLASH").when().get("/response_cookie_with_no_value");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingCookies() throws Exception {
        given().cookies("username", "John", "token", "1234").then().expect().body(equalTo("username, token")).when().get("/cookie");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingCookieUsingMap() throws Exception {
        Map<String, String> cookies = new HashMap<String, String>();
        cookies.put("username", "John");
        cookies.put("token", "1234");
        given().cookies(cookies).then().expect().body(equalTo("username, token")).when().get("/cookie");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingMultipleCookies() throws Exception {
        Map<String, String> cookies = new HashMap<String, String>();
        cookies.put("username", "John");
        cookies.put("token", "1234");
        given().cookies(cookies).and().cookies("key1", "value1").then().expect().body(equalTo("username, token, key1")).when().get("/cookie");
    }

    @Test
    public void canGetCookieDetails() throws Exception {
        final List<Cookie> cookies = get("/multiCookie").detailedCookies().getList("cookie1");

        assertThat(cookies.size(), is(2));

        final Cookie firstCookie = cookies.get(0);
        assertThat(firstCookie.getValue(), equalTo("cookieValue1"));
        assertThat(firstCookie.getDomain(), equalTo("localhost"));

        final Cookie secondCookie = cookies.get(1);
        assertThat(secondCookie.getValue(), equalTo("cookieValue2"));
        assertThat(secondCookie.getDomain(), equalTo("localhost"));
        assertThat(secondCookie.getPath(), equalTo("/"));
        assertThat(secondCookie.getMaxAge(), is(1234567));
        assertThat(secondCookie.isSecured(), is(true));
        assertThat(secondCookie.getVersion(), is(1));
    }
}
