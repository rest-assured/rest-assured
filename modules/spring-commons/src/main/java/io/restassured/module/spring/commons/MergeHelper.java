/*
 * Copyright 2026 the original author or authors.
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

import io.restassured.config.HeaderConfig;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.http.Headers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Merging a specification doesn't add a header or cookie that is already there with the same value, or a result handler
 * or post processor that is already there, again, as in REST Assured. So merging a specification made by
 * <code>given()</code>, which contains the static ones already, doesn't add them twice.
 */
public class MergeHelper {

    private MergeHelper() {
    }

    /**
     * @return The headers of <code>with</code> that aren't in <code>headers</code> with the same value. A header that
     * the header config overwrites is kept, so that it replaces the current one.
     */
    public static Headers headersNotIn(Headers headers, Headers with, HeaderConfig headerConfig) {
        return new Headers(notIn(headers.asList(), with.asList(),
                (header, other) -> !headerConfig.shouldOverwriteHeaderWithName(other.getName()) && header.equals(other)));
    }

    /**
     * @return The cookies of <code>with</code> that aren't in <code>cookies</code> with the same value
     */
    public static Cookies cookiesNotIn(Cookies cookies, Cookies with) {
        return new Cookies(notIn(cookies.asList(), with.asList(), Cookie::equals));
    }

    /**
     * @return The items of <code>with</code> that aren't in <code>items</code>, compared by identity
     */
    public static <T> List<T> sameNotIn(List<T> items, List<T> with) {
        return notIn(items, with, (item, other) -> item == other);
    }

    private static <T> List<T> notIn(List<T> items, List<T> with, BiPredicate<T, T> same) {
        List<T> notIn = new ArrayList<>();
        for (T other : with) {
            if (items.stream().noneMatch(item -> same.test(item, other))) {
                notIn.add(other);
            }
        }
        return notIn;
    }
}
