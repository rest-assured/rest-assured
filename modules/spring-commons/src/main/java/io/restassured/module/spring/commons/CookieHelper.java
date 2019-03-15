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
package io.restassured.module.spring.commons;

import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.http.Headers;
import io.restassured.module.spring.commons.config.SpecificationConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CookieHelper {

    private CookieHelper() {
    }

    public static Cookies cookies(Cookies requestCookies, Map<String, ?> cookies, Headers requestHeaders,
                                  SpecificationConfig config) {
        List<Cookie> cookieList = new ArrayList<Cookie>();
        if (requestCookies.exist()) {
            for (Cookie requestCookie : requestCookies) {
                cookieList.add(requestCookie);
            }
        }
        for (Map.Entry<String, ?> stringEntry : cookies.entrySet()) {
            cookieList.add(new Cookie.Builder(stringEntry.getKey(), Serializer.serializeIfNeeded(stringEntry.getValue(),
                    HeaderHelper.getRequestContentType(requestHeaders), config)).build());
        }
        return new Cookies(cookieList);
    }

    public static Cookies cookies(Cookies requestCookies, Cookies cookies) {
        if (cookies.exist()) {
            List<Cookie> cookieList = new ArrayList<Cookie>();
            if (requestCookies.exist()) {
                for (Cookie cookie : requestCookies) {
                    cookieList.add(cookie);
                }
            }
            for (Cookie cookie : cookies) {
                cookieList.add(cookie);
            }
            return new Cookies(cookieList);
        }
        return requestCookies;
    }

    public static Cookies cookie(final String cookieName, final Object cookieValue, Headers requestHeaders,
                                 final SpecificationConfig config, Object... additionalValues) {
        final String contentType = HeaderHelper.getRequestContentType(requestHeaders);
        List<Cookie> cookieList = new ArrayList<Cookie>() {{
            add(new Cookie.Builder(cookieName, Serializer.serializeIfNeeded(cookieValue, contentType, config)).build());
        }};
        if (additionalValues != null) {
            for (Object additionalCookieValue : additionalValues) {
                cookieList.add(new Cookie.Builder(cookieName,
                        Serializer.serializeIfNeeded(additionalCookieValue, contentType, config)).build());
            }
        }
        return new Cookies(cookieList);
    }

    public static Cookies sessionId(Cookies cookies, String sessionIdName, String sessionIdValue) {
        List<Cookie> allOtherCookies = new ArrayList<Cookie>();
        for (Cookie cookie : cookies) {
            if (!cookie.getName().equalsIgnoreCase(sessionIdName)) {
                allOtherCookies.add(cookie);
            }
        }
        allOtherCookies.add(new Cookie.Builder(sessionIdName, sessionIdValue).build());
        return new Cookies(allOtherCookies);
    }
}
