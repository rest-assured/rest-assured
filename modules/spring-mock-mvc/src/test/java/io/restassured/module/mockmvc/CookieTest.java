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
package io.restassured.module.mockmvc;

import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.module.mockmvc.http.CookieController;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;import io.restassured.response.ResponseOptions;import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.matcher.RestAssuredMatchers.detailedCookie;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;import static org.assertj.core.api.Assertions.assertThat;import static org.hamcrest.Matchers.equalTo;

public class CookieTest {

    @BeforeAll
    public static void configureMockMvcInstance() {
        RestAssuredMockMvc.standaloneSetup(new CookieController());
    }

    @AfterAll
    public static void restRestAssured() {
        RestAssuredMockMvc.reset();
    }

    // Simulate test case from spring cloud contrats
    @Test
    public void validate_shouldReturnACookie() {
        // given:
        MockMvcRequestSpecification request = given()
                .cookie("cookieName1", "foo")
                .cookie("cookieName2", "bar");

        // when:
        ResponseOptions<?> response = given().spec(request)
                .get("/cookie");

        // then:
        assertThat(response.statusCode()).isEqualTo(200);

        // and:
        String responseBody = response.getBody().asString();
        assertThat(responseBody).isEqualTo("{\"cookieValue1\" : \"foo\", \"cookieValue2\" : \"bar\"}");
    }
    
    @Test public void
    can_send_cookie_using_cookie_class() {
        RestAssuredMockMvc.given().
                cookie(new Cookie.Builder("cookieName1", "John Doe").build()).
        when().
                get("/cookie").
        then().
                statusCode(200).
                body("cookieValue1", equalTo("John Doe"));
    }

    @Test public void
    can_send_cookie_using_cookie_name_and_value() {
        RestAssuredMockMvc.given().
                cookie("cookieName1", "John Doe").
        when().
                get("/cookie").
        then().
                statusCode(200).
                body("cookieValue1", equalTo("John Doe"));
    }

    @Test public void
    can_send_multiple_cookies() {
        RestAssuredMockMvc.given().
                cookie("cookieName1", "John Doe").
                cookie("cookieName2", "rest assured").
        when().
                get("/cookie").
        then().
                statusCode(200).
                body("cookieValue1", equalTo("John Doe")).
                body("cookieValue2", equalTo("rest assured"));
    }

    @Test public void
    can_send_cookies_using_map() {
        Map<String, Object> cookies = new HashMap<String, Object>();
        cookies.put("cookieName1", "John Doe");
        cookies.put("cookieName2", "rest assured");

        RestAssuredMockMvc.given().
                cookies(cookies).
        when().
                get("/cookie").
        then().
                statusCode(200).
                body("cookieValue1", equalTo("John Doe")).
                body("cookieValue2", equalTo("rest assured"));
    }

    @Test public void
    can_send_cookies_using_cookies_class() {
        RestAssuredMockMvc.given().
                cookies(new Cookies(new Cookie.Builder("cookieName1", "John Doe").build(), new Cookie.Builder("cookieName2", "rest assured").build())).
        when().
                get("/cookie").
        then().
                statusCode(200).
                body("cookieValue1", equalTo("John Doe")).
                body("cookieValue2", equalTo("rest assured"));
    }

    @Test public void
    can_receive_cookies() {
        RestAssuredMockMvc.given().
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
    
    @Test public void
    can_receive_detailed_cookies() {
        RestAssuredMockMvc.given().
                queryParam("cookieName1", "name").
                queryParam("cookieValue1", "John Doe").
                queryParam("cookieName2", "project").
                queryParam("cookieValue2", "rest assured").
        when().
                get("/setDetailedCookies").
        then().
                statusCode(200).
                cookie("name", detailedCookie().
                    value("John Doe").
                    httpOnly(true).
                    secured(true).
                    sameSite("None").
                    expiryDate(Date.from(ZonedDateTime.of(2023, 1, 1, 12, 30, 0, 0, ZoneId.of("Z")).toInstant()))).
                cookie("project", detailedCookie().
                    value("rest assured").
                    httpOnly(false).
                    secured(false).
                    sameSite("Lax").
                    expiryDate(Date.from(ZonedDateTime.of(2023, 1, 1, 12, 30, 0, 0, ZoneId.of("Z")).toInstant())));
    }
}

// @formatter:on