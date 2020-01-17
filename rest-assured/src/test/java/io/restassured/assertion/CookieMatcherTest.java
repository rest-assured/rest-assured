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

package io.restassured.assertion;

import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Sergey Podgurskiy
 */
public class CookieMatcherTest {
    @Test
    public void testSetVersion() throws ParseException {
        String[] cookies = new String[]{
                "DEVICE_ID=123; Domain=.test.com; Expires=Thu, 12-Oct-2023 09:34:31 GMT; Path=/; Secure; HttpOnly; SameSite=Lax",
                "SPRING_SECURITY_REMEMBER_ME_COOKIE=12345;Version=0;Domain=.test.com;Path=/;Max-Age=1209600",
                "COOKIE_WITH_ZERO_MAX_AGE=1234;Version=0;Domain=.test.com;Path=/;Max-Age=0",
                "COOKIE_WITH_NEGATIVE_MAX_AGE=123456;Version=0;Domain=.test.com;Path=/;Max-Age=-1"};

        Cookies result = CookieMatcher.getCookies(cookies);
        assertEquals(4, result.size());

        Cookie sprintCookie = result.get("SPRING_SECURITY_REMEMBER_ME_COOKIE");
        assertEquals(0, sprintCookie.getVersion());
        assertEquals("12345", sprintCookie.getValue());
        assertEquals(".test.com", sprintCookie.getDomain());
        assertEquals("/", sprintCookie.getPath());
        assertEquals(1209600, sprintCookie.getMaxAge());
        assertEquals(false, sprintCookie.isSecured());
        assertEquals(false, sprintCookie.isHttpOnly());

        Cookie cookieWithZeroMaxAge = result.get("COOKIE_WITH_ZERO_MAX_AGE");
        assertEquals(0, cookieWithZeroMaxAge.getVersion());
        assertEquals("1234", cookieWithZeroMaxAge.getValue());
        assertEquals(".test.com", cookieWithZeroMaxAge.getDomain());
        assertEquals("/", cookieWithZeroMaxAge.getPath());
        assertEquals(0, cookieWithZeroMaxAge.getMaxAge());
        assertEquals(false, cookieWithZeroMaxAge.isSecured());
        assertEquals(false, cookieWithZeroMaxAge.isHttpOnly());

        Cookie cookieWithNegativeMaxAge = result.get("COOKIE_WITH_NEGATIVE_MAX_AGE");
        assertEquals(0, cookieWithNegativeMaxAge.getVersion());
        assertEquals("123456", cookieWithNegativeMaxAge.getValue());
        assertEquals(".test.com", cookieWithNegativeMaxAge.getDomain());
        assertEquals("/", cookieWithNegativeMaxAge.getPath());
        assertEquals(-1, cookieWithNegativeMaxAge.getMaxAge());
        assertEquals(false, cookieWithNegativeMaxAge.isSecured());
        assertEquals(false, cookieWithNegativeMaxAge.isHttpOnly());

        Cookie deviceCookie = result.get("DEVICE_ID");
        assertEquals(-1, deviceCookie.getVersion());
        assertEquals("123", deviceCookie.getValue());
        assertEquals(".test.com", deviceCookie.getDomain());
        assertEquals("/", deviceCookie.getPath());
        assertEquals(new SimpleDateFormat("EEE, d-MMM-yyyy HH:mm:ss Z", Locale.ENGLISH).parse("Thu, 12-Oct-2023 09:34:31 GMT"), deviceCookie.getExpiryDate());
        assertEquals(true, deviceCookie.isSecured());
        assertEquals(true, deviceCookie.isHttpOnly());
        assertEquals("Lax", deviceCookie.getSameSite());

    }

    @Test public void
    deals_with_empty_cookie_values() {
        // Given
        String[] cookiesAsString = new String[]{
                "un=bob; domain=bob.com; path=/", "", "_session_id=asdfwerwersdfwere; domain=bob.com; path=/; HttpOnly"};

        // When
        Cookies cookies = CookieMatcher.getCookies(cookiesAsString);

        // Then
        assertThat(cookies.size(), is(3));
        assertThat(cookies, Matchers.<Cookie>hasItem(nullValue()));
    }
}
