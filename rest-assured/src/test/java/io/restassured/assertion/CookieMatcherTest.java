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
import io.restassured.internal.assertion.CookieMatcher;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sergey Podgurskiy
 */
public class CookieMatcherTest {
    @Test
    public void testSetVersion() throws ParseException {
        List<String> cookies = Arrays.asList(
                "DEVICE_ID=123; Domain=.test.com; Expires=Thu, 12-Oct-2023 09:34:31 GMT; Path=/; Secure; HttpOnly; SameSite=Lax",
                "SPRING_SECURITY_REMEMBER_ME_COOKIE=12345;Version=0;Domain=.test.com;Path=/;Max-Age=1209600",
                "COOKIE_WITH_ZERO_MAX_AGE=1234;Version=0;Domain=.test.com;Path=/;Max-Age=0",
                "COOKIE_WITH_NEGATIVE_MAX_AGE=123456;Version=0;Domain=.test.com;Path=/;Max-Age=-1");

        Cookies result = CookieMatcher.getCookies(cookies);
        assertThat(result.size()).isEqualTo(4);

        Cookie sprintCookie = result.get("SPRING_SECURITY_REMEMBER_ME_COOKIE");
        assertThat(sprintCookie.getVersion()).isEqualTo(0);
        assertThat(sprintCookie.getValue()).isEqualTo("12345");
        assertThat(sprintCookie.getDomain()).isEqualTo(".test.com");
        assertThat(sprintCookie.getPath()).isEqualTo("/");
        assertThat(sprintCookie.getMaxAge()).isEqualTo(1209600L);
        assertThat(sprintCookie.isSecured()).isFalse();
        assertThat(sprintCookie.isHttpOnly()).isFalse();

        Cookie cookieWithZeroMaxAge = result.get("COOKIE_WITH_ZERO_MAX_AGE");
        assertThat(cookieWithZeroMaxAge.getVersion()).isEqualTo(0L);
        assertThat(cookieWithZeroMaxAge.getValue()).isEqualTo("1234");
        assertThat(cookieWithZeroMaxAge.getDomain()).isEqualTo(".test.com");
        assertThat(cookieWithZeroMaxAge.getPath()).isEqualTo("/");
        assertThat(cookieWithZeroMaxAge.getMaxAge()).isEqualTo(0L);
        assertThat(cookieWithZeroMaxAge.isSecured()).isFalse();
        assertThat(cookieWithZeroMaxAge.isHttpOnly()).isFalse();

        Cookie cookieWithNegativeMaxAge = result.get("COOKIE_WITH_NEGATIVE_MAX_AGE");
        assertThat(cookieWithNegativeMaxAge.getVersion()).isEqualTo(0L);
        assertThat(cookieWithNegativeMaxAge.getValue()).isEqualTo("123456");
        assertThat(cookieWithNegativeMaxAge.getDomain()).isEqualTo(".test.com");
        assertThat(cookieWithNegativeMaxAge.getPath()).isEqualTo("/");
        assertThat(cookieWithNegativeMaxAge.getMaxAge()).isEqualTo(-1L);
        assertThat(cookieWithNegativeMaxAge.isSecured()).isFalse();
        assertThat(cookieWithNegativeMaxAge.isHttpOnly()).isFalse();

        Cookie deviceCookie = result.get("DEVICE_ID");
        assertThat(deviceCookie.getVersion()).isEqualTo(-1);
        assertThat(deviceCookie.getValue()).isEqualTo("123");
        assertThat(deviceCookie.getDomain()).isEqualTo(".test.com");
        assertThat(deviceCookie.getPath()).isEqualTo("/");
        assertThat(deviceCookie.getExpiryDate()).isEqualTo(new SimpleDateFormat("EEE, d-MMM-yyyy HH:mm:ss Z", Locale.ENGLISH).parse("Thu, 12-Oct-2023 09:34:31 GMT"));
        assertThat(deviceCookie.isSecured()).isTrue();
        assertThat(deviceCookie.isHttpOnly()).isTrue();
        assertThat(deviceCookie.getSameSite()).isEqualTo("Lax");
    }

    @Test public void
    deals_with_empty_cookie_values() {
        // Given
        List<String> cookiesAsString = Arrays.asList("un=bob; domain=bob.com; path=/", "", "_session_id=asdfwerwersdfwere; domain=bob.com; path=/; HttpOnly");

        // When
        Cookies cookies = CookieMatcher.getCookies(cookiesAsString);

        // Then
        assertThat(cookies.size()).isEqualTo(3);
        assertThat(cookies).anyMatch(java.util.Objects::isNull);
    }
}
