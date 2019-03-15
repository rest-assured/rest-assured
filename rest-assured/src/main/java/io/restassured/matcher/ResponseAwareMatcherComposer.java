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

package io.restassured.matcher;

import io.restassured.internal.common.assertion.AssertParameter;
import io.restassured.response.ResponseBody;
import io.restassured.response.ResponseOptions;
import org.hamcrest.Matcher;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;

/**
 * Allows you to compose {@link ResponseAwareMatcher}'s with other {@link ResponseAwareMatcher}s or Hamcrest {@link Matcher}s.
 *
 * @since 2.5.0
 */
public class ResponseAwareMatcherComposer {

    // and

    /**
     * Compose this {@link ResponseAwareMatcher} with another {@link ResponseAwareMatcher}.
     *
     * @param matcher1 The first matcher to compose
     * @param matcher2 The second matcher to compose
     * @return A and-composed {@link ResponseAwareMatcher}.
     */
    @SuppressWarnings("unchecked")
    public static <T extends ResponseBody<T> & ResponseOptions<T>> ResponseAwareMatcher<T> and(
            final ResponseAwareMatcher<T> matcher1, final ResponseAwareMatcher<T> matcher2) {
        return and(matcher1, matcher2, new ResponseAwareMatcher[0]);
    }

    /**
     * Compose this {@link ResponseAwareMatcher} with another {@link ResponseAwareMatcher}.
     *
     * @param matcher1           The first matcher to compose
     * @param matcher2           The second matcher to compose
     * @param additionalMatchers The additional matchers to compose
     * @return A and-composed {@link ResponseAwareMatcher}.
     */
    @SuppressWarnings("unchecked")
    public static <T extends ResponseBody<T> & ResponseOptions<T>> ResponseAwareMatcher<T> and(
            final ResponseAwareMatcher<T> matcher1, final ResponseAwareMatcher<T> matcher2,
            final ResponseAwareMatcher<T>... additionalMatchers) {
        return response -> {
            Matcher<?>[] matchers = toHamcrestMatchers(response, matcher1, matcher2, additionalMatchers);
            return allOf((Matcher<? super Object>[]) matchers);
        };
    }

    /**
     * Compose this {@link ResponseAwareMatcher} with another {@link ResponseAwareMatcher}.
     *
     * @param matcher1           The first matcher to compose
     * @param matcher2           The second matcher to compose
     * @param additionalMatchers The additional matchers to compose
     * @return A and-composed {@link ResponseAwareMatcher}.
     * // TODO Add @SafeVarargs when migrating to Java 7+
     */
    public static <T extends ResponseBody<T> & ResponseOptions<T>> ResponseAwareMatcher<T> and(
            final ResponseAwareMatcher<T> matcher1, final Matcher matcher2,
            final ResponseAwareMatcher<T>... additionalMatchers) {
        return and(matcher1, response -> matcher2, additionalMatchers);
    }

    /**
     * Compose this {@link ResponseAwareMatcher} with another {@link ResponseAwareMatcher}.
     *
     * @param matcher1 The first matcher to compose
     * @param matcher2 The second matcher to compose
     * @return A and-composed {@link ResponseAwareMatcher}.
     */
    @SuppressWarnings("unchecked")
    public static <T extends ResponseBody<T> & ResponseOptions<T>> ResponseAwareMatcher<T> and(
            final ResponseAwareMatcher<T> matcher1, final Matcher matcher2) {
        return and(matcher1, matcher2, new ResponseAwareMatcher[0]);
    }

    /**
     * Compose this {@link ResponseAwareMatcher} with another {@link ResponseAwareMatcher}.
     *
     * @param matcher1           The first matcher to compose
     * @param matcher2           The second matcher to compose
     * @param additionalMatchers The additional matchers to compose
     * @return A and-composed {@link ResponseAwareMatcher}.
     * // TODO Add @SafeVarargs when migrating to Java 7+
     */
    public static <T extends ResponseBody<T> & ResponseOptions<T>> ResponseAwareMatcher<T> and(
            final Matcher matcher1, final ResponseAwareMatcher<T> matcher2,
            final ResponseAwareMatcher<T>... additionalMatchers) {
        return and(matcher2, matcher1, additionalMatchers);
    }

    // or

    /**
     * Compose this {@link ResponseAwareMatcher} with another {@link ResponseAwareMatcher}.
     *
     * @param matcher1 The first matcher to compose
     * @param matcher2 The second matcher to compose
     * @return A or-composed {@link ResponseAwareMatcher}.
     */
    @SuppressWarnings("unchecked")
    public static <T extends ResponseBody<T> & ResponseOptions<T>> ResponseAwareMatcher<T> or(
            final ResponseAwareMatcher<T> matcher1, final ResponseAwareMatcher<T> matcher2) {
        return or(matcher1, matcher2, new ResponseAwareMatcher[0]);
    }

    /**
     * Compose this {@link ResponseAwareMatcher} with another {@link ResponseAwareMatcher}.
     *
     * @param matcher1           The first matcher to compose
     * @param matcher2           The second matcher to compose
     * @param additionalMatchers The additional matchers to compose
     * @return A or-composed {@link ResponseAwareMatcher}.
     */
    @SuppressWarnings("unchecked")
    public static <T extends ResponseBody<T> & ResponseOptions<T>> ResponseAwareMatcher<T> or(
            final ResponseAwareMatcher<T> matcher1, final ResponseAwareMatcher<T> matcher2,
            final ResponseAwareMatcher<T>... additionalMatchers) {
        return response -> {
            Matcher<?>[] matchers = toHamcrestMatchers(response, matcher1, matcher2, additionalMatchers);
            return anyOf((Matcher<? super Object>[]) matchers);
        };
    }

    /**
     * Compose this {@link ResponseAwareMatcher} with another {@link ResponseAwareMatcher}.
     *
     * @param matcher1           The first matcher to compose
     * @param matcher2           The second matcher to compose
     * @param additionalMatchers The additional matchers to compose
     * @return A or-composed {@link ResponseAwareMatcher}.
     * // TODO Add @SafeVarargs when migrating to Java 7+
     */
    public static <T extends ResponseBody<T> & ResponseOptions<T>> ResponseAwareMatcher<T> or(
            final ResponseAwareMatcher<T> matcher1, final Matcher matcher2,
            final ResponseAwareMatcher<T>... additionalMatchers) {
        return or(matcher1, response -> matcher2, additionalMatchers);
    }

    /**
     * Compose this {@link ResponseAwareMatcher} with another {@link ResponseAwareMatcher}.
     *
     * @param matcher1 The first matcher to compose
     * @param matcher2 The second matcher to compose
     * @return A and-composed {@link ResponseAwareMatcher}.
     */
    @SuppressWarnings("unchecked")
    public static <T extends ResponseBody<T> & ResponseOptions<T>> ResponseAwareMatcher<T> or(
            final ResponseAwareMatcher<T> matcher1, final Matcher matcher2) {
        return or(matcher1, matcher2, new ResponseAwareMatcher[0]);
    }

    /**
     * Compose this {@link ResponseAwareMatcher} with another {@link ResponseAwareMatcher}.
     *
     * @param matcher1           The first matcher to compose
     * @param matcher2           The second matcher to compose
     * @param additionalMatchers The additional matchers to compose
     * @return A or-composed {@link ResponseAwareMatcher}.
     * // TODO Add @SafeVarargs when migrating to Java 7+
     */
    public static <T extends ResponseBody<T> & ResponseOptions<T>> ResponseAwareMatcher<T> or(
            final Matcher matcher1, final ResponseAwareMatcher<T> matcher2,
            final ResponseAwareMatcher<T>... additionalMatchers) {
        return or(matcher2, matcher1, additionalMatchers);
    }

    private static <T extends ResponseBody<T> & ResponseOptions<T>> Matcher<?>[] toHamcrestMatchers(T response,
                                                                                                    ResponseAwareMatcher<T> matcher1,
                                                                                                    ResponseAwareMatcher<T> matcher2,
                                                                                                    ResponseAwareMatcher<T>[] additionalMatchers) throws Exception {
        AssertParameter.notNull(matcher1, ResponseAwareMatcher.class);
        AssertParameter.notNull(matcher2, ResponseAwareMatcher.class);

        List<Matcher<?>> hamcrestMatchers = new ArrayList<Matcher<?>>();
        hamcrestMatchers.add(matcher1.matcher(response));
        hamcrestMatchers.add(matcher2.matcher(response));

        if (additionalMatchers != null) {
            for (ResponseAwareMatcher<T> additionalMatcher : additionalMatchers) {
                hamcrestMatchers.add(additionalMatcher.matcher(response));
            }
        }

        return hamcrestMatchers.toArray(new Matcher[hamcrestMatchers.size()]);
    }
}
