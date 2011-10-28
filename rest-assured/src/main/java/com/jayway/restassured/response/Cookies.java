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

package com.jayway.restassured.response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.jayway.restassured.assertion.AssertParameter.notNull;

/**
 * Represents the a number of response cookies
 */
public class Cookies implements Iterable<Cookie> {

    private final List<Cookie> cookies;

    public Cookies(List<Cookie> cookies) {
        notNull(cookies, "Cookies");
        this.cookies = cookies;
    }

    /**
     * @return The size of the cookies
     */
    public int size() {
        return cookies.size();
    }

    /**
     * @return <code>true</code> if one or more cookies are defined, <code>false</code> otherwise.
     */
    public boolean exist() {
        return !cookies.isEmpty();
    }

    /**
     *  Get a single cookie with the supplied name. If there are several cookies match the <code>cookieName</code> then
     *  the first one is returned.
     *
     * @param cookieName The name of the cookie to find
     * @return The found cookie or <code>null</code> if no cookie was found.
     */
    public Cookie get(String cookieName) {
        notNull(cookieName, "Cookie name");
        for (Cookie cookie : cookies) {
            if(cookie.getName().equals(cookieName)) {
                return cookie;
            }
        }
        return null;
    }

    /**
     *  Get all cookies with the supplied name. If there's only one cookie matching the <code>cookieName</code> then
     *  a list with only that cookie is returned.
     *
     * @param cookieName The name of the cookie to find
     * @return The found cookies or empty list if no cookie was found.
     */
    public List<Cookie> multiGet(String cookieName) {
        notNull(cookieName, "Cookie name");
        final List<Cookie> cookieList = new ArrayList<Cookie>();
        for (Cookie cookie : cookies) {
            if(cookie.getName().equals(cookieName)) {
                cookieList.add(cookie);
            }
        }
        return Collections.unmodifiableList(cookieList);
    }

    public Iterator<Cookie> iterator() {
        return cookies.iterator();
    }
}
