/*
 * Copyright 2015 the original author or authors.
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

/*
 * Copyright 2015 the original author or authors.
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
package com.jayway.restassured.matcher;

import com.jayway.restassured.internal.assertion.AssertParameter;
import com.jayway.restassured.response.ResponseBody;
import com.jayway.restassured.response.ResponseOptions;
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
     * Compose this {@link com.jayway.restassured.matcher.ResponseAwareMatcher} with another {@link com.jayway.restassured.matcher.ResponseAwareMatcher}.
     *
     * @param matcher1 The first matcher to compose
     * @param matcher2 The second matcher to compose
     * @return A and-composed {@link com.jayway.restassured.matcher.ResponseAwareMatcher}.
     */
    @SuppressWarnings("unchecked")
    public static <T extends ResponseBody<T> & ResponseOptions<T>> ResponseAwareMatcher<T> and(
            final ResponseAwareMatcher<T> matcher1, final ResponseAwareMatcher<T> matcher2) {
        return and(matcher1, matcher2, new ResponseAwareMatcher[0]);
    }

    /**
     * Compose this {@link com.jayway.restassured.matcher.ResponseAwareMatcher} with another {@link com.jayway.restassured.matcher.ResponseAwareMatcher}.
     *
     * @param matcher1           The first matcher to compose
     * @param matcher2           The second matcher to compose
     * @param additionalMatchers The additional matchers to compose
     * @return A and-composed {@link com.jayway.restassured.matcher.ResponseAwareMatcher}.
     */
    @SuppressWarnings("unchecked")
    public static <T extends ResponseBody<T> & ResponseOptions<T>> ResponseAwareMatcher<T> and(
            final ResponseAwareMatcher<T> matcher1, final ResponseAwareMatcher<T> matcher2,
            final ResponseAwareMatcher<T>... additionalMatchers) {
        return new ResponseAwareMatcher<T>() {
            public Matcher<?> matcher(T response) throws Exception {
                Matcher<?>[] matchers = toHamcrestMatchers(response, matcher1, matcher2, additionalMatchers);
                return allOf((Matcher<? super Object>[]) matchers);
            }
        };
    }

    /**
     * Compose this {@link com.jayway.restassured.matcher.ResponseAwareMatcher} with another {@link com.jayway.restassured.matcher.ResponseAwareMatcher}.
     *
     * @param matcher1           The first matcher to compose
     * @param matcher2           The second matcher to compose
     * @param additionalMatchers The additional matchers to compose
     * @return A and-composed {@link com.jayway.restassured.matcher.ResponseAwareMatcher}.
     * // TODO Add @SafeVarargs when migrating to Java 7+
     */
    public static <T extends ResponseBody<T> & ResponseOptions<T>> ResponseAwareMatcher<T> and(
            final ResponseAwareMatcher<T> matcher1, final Matcher matcher2,
            final ResponseAwareMatcher<T>... additionalMatchers) {
        return and(matcher1, new ResponseAwareMatcher<T>() {
            public Matcher<?> matcher(T response) throws Exception {
                return matcher2;
            }
        }, additionalMatchers);
    }

    /**
     * Compose this {@link com.jayway.restassured.matcher.ResponseAwareMatcher} with another {@link com.jayway.restassured.matcher.ResponseAwareMatcher}.
     *
     * @param matcher1 The first matcher to compose
     * @param matcher2 The second matcher to compose
     * @return A and-composed {@link com.jayway.restassured.matcher.ResponseAwareMatcher}.
     */
    @SuppressWarnings("unchecked")
    public static <T extends ResponseBody<T> & ResponseOptions<T>> ResponseAwareMatcher<T> and(
            final ResponseAwareMatcher<T> matcher1, final Matcher matcher2) {
        return and(matcher1, matcher2, new ResponseAwareMatcher[0]);
    }

    // or

    /**
     * Compose this {@link com.jayway.restassured.matcher.ResponseAwareMatcher} with another {@link com.jayway.restassured.matcher.ResponseAwareMatcher}.
     *
     * @param matcher1 The first matcher to compose
     * @param matcher2 The second matcher to compose
     * @return A or-composed {@link com.jayway.restassured.matcher.ResponseAwareMatcher}.
     */
    @SuppressWarnings("unchecked")
    public static <T extends ResponseBody<T> & ResponseOptions<T>> ResponseAwareMatcher<T> or(
            final ResponseAwareMatcher<T> matcher1, final ResponseAwareMatcher<T> matcher2) {
        return or(matcher1, matcher2, new ResponseAwareMatcher[0]);
    }

    /**
     * Compose this {@link com.jayway.restassured.matcher.ResponseAwareMatcher} with another {@link com.jayway.restassured.matcher.ResponseAwareMatcher}.
     *
     * @param matcher1           The first matcher to compose
     * @param matcher2           The second matcher to compose
     * @param additionalMatchers The additional matchers to compose
     * @return A or-composed {@link com.jayway.restassured.matcher.ResponseAwareMatcher}.
     */
    @SuppressWarnings("unchecked")
    public static <T extends ResponseBody<T> & ResponseOptions<T>> ResponseAwareMatcher<T> or(
            final ResponseAwareMatcher<T> matcher1, final ResponseAwareMatcher<T> matcher2,
            final ResponseAwareMatcher<T>... additionalMatchers) {
        return new ResponseAwareMatcher<T>() {
            public Matcher<?> matcher(T response) throws Exception {
                Matcher<?>[] matchers = toHamcrestMatchers(response, matcher1, matcher2, additionalMatchers);
                return anyOf((Matcher<? super Object>[]) matchers);
            }
        };
    }

    /**
     * Compose this {@link com.jayway.restassured.matcher.ResponseAwareMatcher} with another {@link com.jayway.restassured.matcher.ResponseAwareMatcher}.
     *
     * @param matcher1           The first matcher to compose
     * @param matcher2           The second matcher to compose
     * @param additionalMatchers The additional matchers to compose
     * @return A or-composed {@link com.jayway.restassured.matcher.ResponseAwareMatcher}.
     * // TODO Add @SafeVarargs when migrating to Java 7+
     */
    public static <T extends ResponseBody<T> & ResponseOptions<T>> ResponseAwareMatcher<T> or(
            final ResponseAwareMatcher<T> matcher1, final Matcher matcher2,
            final ResponseAwareMatcher<T>... additionalMatchers) {
        return and(matcher1, new ResponseAwareMatcher<T>() {
            public Matcher<?> matcher(T response) throws Exception {
                return matcher2;
            }
        }, additionalMatchers);
    }

    /**
     * Compose this {@link com.jayway.restassured.matcher.ResponseAwareMatcher} with another {@link com.jayway.restassured.matcher.ResponseAwareMatcher}.
     *
     * @param matcher1 The first matcher to compose
     * @param matcher2 The second matcher to compose
     * @return A and-composed {@link com.jayway.restassured.matcher.ResponseAwareMatcher}.
     */
    @SuppressWarnings("unchecked")
    public static <T extends ResponseBody<T> & ResponseOptions<T>> ResponseAwareMatcher<T> or(
            final ResponseAwareMatcher<T> matcher1, final Matcher matcher2) {
        return or(matcher1, matcher2, new ResponseAwareMatcher[0]);
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
