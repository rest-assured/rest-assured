package com.jayway.restassured.assertion;

import com.jayway.restassured.response.Cookie;
import com.jayway.restassured.response.Cookies;
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
                "DEVICE_ID=123; Domain=.test.com; Expires=Thu, 12-Oct-2023 09:34:31 GMT; Path=/; Secure; HttpOnly;",
                "SPRING_SECURITY_REMEMBER_ME_COOKIE=12345;Version=0;Domain=.test.com;Path=/;Max-Age=1209600"};

        Cookies result = CookieMatcher.getCookies(cookies);
        assertEquals(2, result.size());

        Cookie sprintCookie = result.get("SPRING_SECURITY_REMEMBER_ME_COOKIE");
        assertEquals(0, sprintCookie.getVersion());
        assertEquals("12345", sprintCookie.getValue());
        assertEquals(".test.com", sprintCookie.getDomain());
        assertEquals("/", sprintCookie.getPath());
        assertEquals(1209600, sprintCookie.getMaxAge());
        assertEquals(false, sprintCookie.isSecured());
        assertEquals(false, sprintCookie.isHttpOnly());

        Cookie deviceCookie = result.get("DEVICE_ID");
        assertEquals(-1, deviceCookie.getVersion());
        assertEquals("123", deviceCookie.getValue());
        assertEquals(".test.com", deviceCookie.getDomain());
        assertEquals("/", deviceCookie.getPath());
        assertEquals(new SimpleDateFormat("EEE, d-MMM-yyyy HH:mm:ss Z", Locale.ENGLISH).parse("Thu, 12-Oct-2023 09:34:31 GMT"), deviceCookie.getExpiryDate());
        assertEquals(true, deviceCookie.isSecured());
        assertEquals(true, deviceCookie.isHttpOnly());

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
