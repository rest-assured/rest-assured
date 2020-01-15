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

// @formatter:off
package com.oneandone.iocunit.restassuredtest;

import static org.hamcrest.Matchers.equalTo;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.oneandone.iocunit.IocUnitRunner;
import com.oneandone.iocunit.analyzer.annotations.SutClasses;
import com.oneandone.iocunit.restassuredtest.http.CookieResource;

import io.restassured.RestAssured;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;

@RunWith(IocUnitRunner.class)
@SutClasses(CookieResource.class)
public class CookieTest {

    @Test
    public void
    can_send_cookie_using_cookie_class() {
        RestAssured.given().
                cookie(new Cookie.Builder("cookieName1", "John Doe").build()).
                when().
                get("/cookie").
                then().
                statusCode(200).
                body("cookieValue1", equalTo("John Doe"));
    }

    @Test
    public void
    can_send_cookie_using_cookie_name_and_value() {
        RestAssured.given().
                cookie("cookieName1", "John Doe").
                when().
                get("/cookie").
                then().
                statusCode(200).
                body("cookieValue1", equalTo("John Doe"));
    }

    @Test
    public void
    can_send_multiple_cookies() {
        RestAssured.given().
                cookie("cookieName1", "John Doe").
                cookie("cookieName2", "rest assured").
                when().
                get("/cookie").
                then().
                statusCode(200).
                body("cookieValue1", equalTo("John Doe")).
                body("cookieValue2", equalTo("rest assured"));
    }

    @Test
    public void
    can_send_cookies_using_map() {
        Map<String, Object> cookies = new HashMap<String, Object>();
        cookies.put("cookieName1", "John Doe");
        cookies.put("cookieName2", "rest assured");

        RestAssured.given().
                cookies(cookies).
                when().
                get("/cookie").
                then().
                statusCode(200).
                body("cookieValue1", equalTo("John Doe")).
                body("cookieValue2", equalTo("rest assured"));
    }

    @Test
    public void
    can_send_cookies_using_cookies_class() {
        RestAssured.given().
                cookies(new Cookies(new Cookie.Builder("cookieName1", "John Doe").build(), new Cookie.Builder("cookieName2", "rest assured").build())).
                when().
                get("/cookie").
                then().
                statusCode(200).
                body("cookieValue1", equalTo("John Doe")).
                body("cookieValue2", equalTo("rest assured"));
    }

    @Test
    public void
    can_receive_cookies() {
        RestAssured.given().
                queryParam("cookieName1", "name").
                queryParam("cookieValue1", "John Doe").
                queryParam("cookieName2", "project").
                queryParam("cookieValue2", "rest assured").
                when().
                get("/setCookies").
                then().
                statusCode(200).
                cookie("name", "John Doe").
                cookie("project", "rest assured");
    }
}

// @formatter:on